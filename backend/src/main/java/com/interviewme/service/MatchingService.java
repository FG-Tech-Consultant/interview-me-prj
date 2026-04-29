package com.interviewme.service;

import com.interviewme.aichat.client.EmbeddingClient;
import com.interviewme.aichat.repository.ContentEmbeddingRepository;
import com.interviewme.aichat.service.EmbeddingService;
import com.interviewme.dto.matching.*;
import com.interviewme.model.Profile;
import com.interviewme.model.Skill;
import com.interviewme.model.UserSkill;
import com.interviewme.repository.ProfileRepository;
import com.interviewme.repository.SkillRepository;
import com.interviewme.repository.UserSkillRepository;
import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Session;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class MatchingService {

    private final Driver neo4jDriver;
    private final SkillRepository skillRepository;
    private final UserSkillRepository userSkillRepository;
    private final ProfileRepository profileRepository;
    private final ContentEmbeddingRepository embeddingRepository;
    private final EmbeddingClient embeddingClient;

    private static final double GRAPH_WEIGHT = 0.4;
    private static final double VECTOR_WEIGHT = 0.6;

    public MatchingService(Driver neo4jDriver,
                           SkillRepository skillRepository,
                           UserSkillRepository userSkillRepository,
                           ProfileRepository profileRepository,
                           ContentEmbeddingRepository embeddingRepository,
                           @Nullable EmbeddingClient embeddingClient) {
        this.neo4jDriver = neo4jDriver;
        this.skillRepository = skillRepository;
        this.userSkillRepository = userSkillRepository;
        this.profileRepository = profileRepository;
        this.embeddingRepository = embeddingRepository;
        this.embeddingClient = embeddingClient;
    }

    @Transactional(readOnly = true)
    public MatchResponse matchCandidates(Long tenantId, MatchRequest request) {
        log.info("Matching candidates for tenantId={} skillIds={} topK={}", tenantId, request.skillIds(), request.topK());

        // 1. Resolve requested skills from PG
        List<Skill> requestedSkills = skillRepository.findAllById(request.skillIds());
        if (requestedSkills.isEmpty()) {
            return new MatchResponse(List.of(), 0, List.of());
        }

        // 2. Graph traversal: expand skills via SIMILAR_TO relationships
        Set<String> expandedSkillNames = new LinkedHashSet<>();
        Set<Long> expandedSkillIds = new LinkedHashSet<>();
        Map<Long, String> skillIdToMatchType = new LinkedHashMap<>();

        for (Skill skill : requestedSkills) {
            expandedSkillNames.add(skill.getName());
            expandedSkillIds.add(skill.getId());
            skillIdToMatchType.put(skill.getId(), "DIRECT");
        }

        Set<Long> adjacentIds = findAdjacentSkills(requestedSkills);
        for (Long adjId : adjacentIds) {
            if (!expandedSkillIds.contains(adjId)) {
                expandedSkillIds.add(adjId);
                skillIdToMatchType.put(adjId, "ADJACENT");
            }
        }

        // Load adjacent skill names
        if (!adjacentIds.isEmpty()) {
            skillRepository.findAllById(adjacentIds).forEach(s -> expandedSkillNames.add(s.getName()));
        }

        // 3. Graph score: find candidates that have any of the expanded skills
        Map<Long, List<MatchedSkill>> candidateSkillMap = buildCandidateSkillMap(
                tenantId, expandedSkillIds, skillIdToMatchType);

        // Compute graph score per candidate (% of requested skills matched, weighted)
        int totalRequested = requestedSkills.size();
        Map<Long, Double> graphScores = new LinkedHashMap<>();
        for (var entry : candidateSkillMap.entrySet()) {
            long directMatches = entry.getValue().stream()
                    .filter(ms -> "DIRECT".equals(ms.matchType()))
                    .count();
            long adjacentMatches = entry.getValue().stream()
                    .filter(ms -> "ADJACENT".equals(ms.matchType()))
                    .count();
            double score = (directMatches + adjacentMatches * 0.5) / totalRequested;
            graphScores.put(entry.getKey(), Math.min(score, 1.0));
        }

        // 4. Vector score: cosine similarity on candidate profile embeddings
        Map<Long, Double> vectorScores = new LinkedHashMap<>();
        if (embeddingClient != null) {
            String queryText = buildQueryText(requestedSkills, request.description());
            vectorScores = computeVectorScores(tenantId, queryText, request.topK(), request.threshold());
        }

        // 5. Combine scores: merge all candidate IDs
        Set<Long> allCandidateIds = new LinkedHashSet<>();
        allCandidateIds.addAll(graphScores.keySet());
        allCandidateIds.addAll(vectorScores.keySet());

        List<ScoredCandidate> scored = new ArrayList<>();
        for (Long profileId : allCandidateIds) {
            double gs = graphScores.getOrDefault(profileId, 0.0);
            double vs = vectorScores.getOrDefault(profileId, 0.0);
            double combined = gs * GRAPH_WEIGHT + vs * VECTOR_WEIGHT;
            scored.add(new ScoredCandidate(profileId, combined, gs, vs));
        }

        // Sort by combined score descending
        scored.sort(Comparator.comparingDouble(ScoredCandidate::combined).reversed());

        // Limit to topK
        List<ScoredCandidate> topCandidates = scored.stream()
                .limit(request.topK())
                .toList();

        // 6. Load profile info
        Set<Long> profileIds = topCandidates.stream().map(ScoredCandidate::profileId).collect(Collectors.toSet());
        Map<Long, Profile> profileMap = profileRepository.findAllById(profileIds).stream()
                .collect(Collectors.toMap(Profile::getId, p -> p));

        List<MatchResult> results = topCandidates.stream()
                .map(sc -> {
                    Profile p = profileMap.get(sc.profileId());
                    return new MatchResult(
                            sc.profileId(),
                            p != null ? p.getFullName() : "Unknown",
                            p != null ? p.getHeadline() : null,
                            p != null ? p.getLocation() : null,
                            Math.round(sc.combined() * 1000.0) / 1000.0,
                            Math.round(sc.graphScore() * 1000.0) / 1000.0,
                            Math.round(sc.vectorScore() * 1000.0) / 1000.0,
                            candidateSkillMap.getOrDefault(sc.profileId(), List.of())
                    );
                })
                .toList();

        return new MatchResponse(results, results.size(), new ArrayList<>(expandedSkillNames));
    }

    /**
     * Neo4j traversal: find skills connected via SIMILAR_TO to the requested skills.
     */
    private Set<Long> findAdjacentSkills(List<Skill> requestedSkills) {
        Set<Long> adjacent = new LinkedHashSet<>();
        List<String> skillIds = requestedSkills.stream()
                .map(s -> s.getId().toString())
                .toList();

        try (Session session = neo4jDriver.session()) {
            var result = session.run("""
                    UNWIND $ids AS sid
                    MATCH (s:Skill {id: sid})-[:SIMILAR_TO]-(adj:Skill)
                    WHERE adj.isActive = true
                    RETURN DISTINCT adj.id AS adjId
                    """,
                    Map.of("ids", skillIds));

            while (result.hasNext()) {
                Record record = result.next();
                String adjIdStr = record.get("adjId").asString();
                try {
                    adjacent.add(Long.parseLong(adjIdStr));
                } catch (NumberFormatException e) {
                    log.debug("Non-numeric skill ID in graph: {}", adjIdStr);
                }
            }
        } catch (Exception e) {
            log.warn("Neo4j traversal failed, continuing with direct skills only: {}", e.getMessage());
        }
        return adjacent;
    }

    /**
     * Find candidates (profiles) that have any of the expanded skills via UserSkill table.
     */
    private Map<Long, List<MatchedSkill>> buildCandidateSkillMap(
            Long tenantId, Set<Long> skillIds, Map<Long, String> skillIdToMatchType) {

        Map<Long, List<MatchedSkill>> result = new LinkedHashMap<>();

        List<UserSkill> userSkills = userSkillRepository.findByTenantIdAndSkillIdInAndDeletedAtIsNull(
                tenantId, new ArrayList<>(skillIds));

        for (UserSkill us : userSkills) {
            String matchType = skillIdToMatchType.getOrDefault(us.getSkillId(), "ADJACENT");
            MatchedSkill ms = new MatchedSkill(
                    us.getSkillId(),
                    us.getSkill() != null ? us.getSkill().getName() : "Unknown",
                    matchType,
                    us.getYearsOfExperience() != null ? us.getYearsOfExperience() : 0,
                    us.getProficiencyDepth() != null ? us.getProficiencyDepth() : 0
            );
            result.computeIfAbsent(us.getProfileId(), k -> new ArrayList<>()).add(ms);
        }
        return result;
    }

    /**
     * Compute vector similarity scores for candidate profiles.
     */
    private Map<Long, Double> computeVectorScores(Long tenantId, String queryText, int topK, double threshold) {
        Map<Long, Double> scores = new LinkedHashMap<>();
        try {
            float[] queryEmbedding = embeddingClient.embed("search_query: " + queryText);
            String embeddingStr = EmbeddingService.floatArrayToString(queryEmbedding);

            var similar = embeddingRepository.findTopKBySimilarityAndType(
                    tenantId, embeddingStr, "PROFILE_SUMMARY", topK, threshold);

            for (var ce : similar) {
                // contentId = profileId for PROFILE_SUMMARY type
                scores.put(ce.getContentId(), computeCosineSimilarity(queryEmbedding, ce.getEmbedding()));
            }
        } catch (Exception e) {
            log.warn("Vector similarity search failed: {}", e.getMessage());
        }
        return scores;
    }

    private double computeCosineSimilarity(float[] query, String storedEmbedding) {
        // The DB already orders by cosine distance, so we use the position-based score
        // For now, parse the stored vector and compute actual cosine similarity
        try {
            String cleaned = storedEmbedding.replace("[", "").replace("]", "");
            String[] parts = cleaned.split(",");
            float[] stored = new float[parts.length];
            for (int i = 0; i < parts.length; i++) {
                stored[i] = Float.parseFloat(parts[i].trim());
            }

            double dotProduct = 0, normA = 0, normB = 0;
            for (int i = 0; i < Math.min(query.length, stored.length); i++) {
                dotProduct += query[i] * stored[i];
                normA += query[i] * query[i];
                normB += stored[i] * stored[i];
            }
            if (normA == 0 || normB == 0) return 0;
            return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
        } catch (Exception e) {
            log.debug("Failed to compute cosine similarity: {}", e.getMessage());
            return 0;
        }
    }

    private String buildQueryText(List<Skill> skills, String description) {
        StringBuilder sb = new StringBuilder();
        sb.append("Required skills: ");
        sb.append(skills.stream().map(Skill::getName).collect(Collectors.joining(", ")));
        if (description != null && !description.isBlank()) {
            sb.append(". ").append(description);
        }
        return sb.toString();
    }

    private record ScoredCandidate(Long profileId, double combined, double graphScore, double vectorScore) {}
}
