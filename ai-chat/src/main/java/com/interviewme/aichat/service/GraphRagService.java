package com.interviewme.aichat.service;

import com.ladybugdb.Connection;
import com.ladybugdb.FlatTuple;
import com.ladybugdb.QueryResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Graph RAG: enriches retrieval results with graph context from LadybugDB.
 * Traverses skill relationships (SIMILAR_TO, BELONGS_TO) to add related
 * context that pure text retrieval would miss.
 */
@Service
@Slf4j
public class GraphRagService {

    private static final int MAX_GRAPH_HOPS = 2;
    private static final int MAX_RELATED_SKILLS = 10;

    private final Connection graphConnection;

    public GraphRagService(Connection graphConnection) {
        this.graphConnection = graphConnection;
    }

    public synchronized GraphContext enrichWithGraphContext(String query, List<String> retrievedTexts) {
        Set<String> mentionedSkills = findMentionedSkills(query, retrievedTexts);

        if (mentionedSkills.isEmpty()) {
            return GraphContext.EMPTY;
        }

        Map<String, List<RelatedSkill>> relatedBySkill = new LinkedHashMap<>();
        Set<String> domains = new LinkedHashSet<>();

        try {
            for (String skillName : mentionedSkills) {
                List<RelatedSkill> related = findRelatedSkills(skillName);
                if (!related.isEmpty()) {
                    relatedBySkill.put(skillName, related);
                }

                String domain = findDomain(skillName);
                if (domain != null) {
                    domains.add(domain);
                }
            }
        } catch (Exception e) {
            log.warn("Graph context enrichment failed: {}", e.getMessage());
            return GraphContext.EMPTY;
        }

        log.info("Graph RAG: mentionedSkills={} relatedSkills={} domains={}",
            mentionedSkills.size(),
            relatedBySkill.values().stream().mapToInt(List::size).sum(),
            domains.size());

        return new GraphContext(mentionedSkills, relatedBySkill, domains);
    }

    public String buildGraphContextString(GraphContext ctx) {
        if (ctx == null || ctx.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder("--- Graph Context ---\n");

        if (!ctx.domains.isEmpty()) {
            sb.append("Relevant domains: ").append(String.join(", ", ctx.domains)).append("\n");
        }

        for (Map.Entry<String, List<RelatedSkill>> entry : ctx.relatedBySkill.entrySet()) {
            sb.append("Skills related to '").append(entry.getKey()).append("': ");
            sb.append(entry.getValue().stream()
                .map(r -> r.name + " (similarity: " + String.format("%.2f", r.weight) + ")")
                .collect(Collectors.joining(", ")));
            sb.append("\n");
        }

        return sb.toString();
    }

    private Set<String> findMentionedSkills(String query, List<String> texts) {
        Set<String> mentioned = new LinkedHashSet<>();
        String combined = query + " " + String.join(" ", texts);
        String lowerCombined = combined.toLowerCase();

        try {
            QueryResult result = graphConnection.query("MATCH (s:Skill) WHERE s.isActive = true RETURN s.name AS name");
            while (result.hasNext()) {
                FlatTuple row = result.getNext();
                String skillName = row.getValue(0).getValue();
                if (lowerCombined.contains(skillName.toString().toLowerCase())) {
                    mentioned.add(skillName.toString());
                }
            }
        } catch (Exception e) {
            log.warn("Failed to find mentioned skills: {}", e.getMessage());
        }

        return mentioned;
    }

    private List<RelatedSkill> findRelatedSkills(String skillName) {
        List<RelatedSkill> related = new ArrayList<>();
        try {
            String escaped = skillName.replace("'", "\\'");
            QueryResult result = graphConnection.query(
                    ("MATCH (s:Skill {name: '%s'})-[r:SIMILAR_TO*1..%d]-(related:Skill) " +
                     "WHERE related.isActive = true AND related.name <> '%s' " +
                     "RETURN DISTINCT related.name AS name, min(r.weight) AS weight " +
                     "ORDER BY weight DESC LIMIT %d")
                            .formatted(escaped, MAX_GRAPH_HOPS, escaped, MAX_RELATED_SKILLS));

            while (result.hasNext()) {
                FlatTuple row = result.getNext();
                related.add(new RelatedSkill(
                    row.getValue(0).getValue().toString(),
                    ((Number) row.getValue(1).getValue()).doubleValue()
                ));
            }
        } catch (Exception e) {
            log.debug("Failed to find related skills for '{}': {}", skillName, e.getMessage());
        }
        return related;
    }

    private String findDomain(String skillName) {
        try {
            String escaped = skillName.replace("'", "\\'");
            QueryResult result = graphConnection.query(
                    "MATCH (s:Skill {name: '%s'})-[:BELONGS_TO]->(d:Domain) RETURN d.name AS domain"
                            .formatted(escaped));
            if (result.hasNext()) {
                return result.getNext().getValue(0).getValue().toString();
            }
        } catch (Exception e) {
            log.debug("Failed to find domain for '{}': {}", skillName, e.getMessage());
        }
        return null;
    }

    public record RelatedSkill(String name, double weight) {}

    public record GraphContext(
        Set<String> mentionedSkills,
        Map<String, List<RelatedSkill>> relatedBySkill,
        Set<String> domains
    ) {
        static final GraphContext EMPTY = new GraphContext(Set.of(), Map.of(), Set.of());

        boolean isEmpty() {
            return mentionedSkills.isEmpty() && relatedBySkill.isEmpty() && domains.isEmpty();
        }
    }
}
