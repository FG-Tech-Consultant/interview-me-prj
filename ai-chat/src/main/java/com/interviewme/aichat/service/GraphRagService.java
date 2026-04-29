package com.interviewme.aichat.service;

import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Session;
import org.neo4j.driver.Result;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Graph RAG: enriches retrieval results with graph context from Neo4j.
 * Traverses skill relationships (SIMILAR_TO, BELONGS_TO) to add related
 * context that pure text retrieval would miss.
 */
@Service
@Slf4j
public class GraphRagService {

    private static final int MAX_GRAPH_HOPS = 2;
    private static final int MAX_RELATED_SKILLS = 10;

    private final Driver neo4jDriver;

    public GraphRagService(Driver neo4jDriver) {
        this.neo4jDriver = neo4jDriver;
    }

    /**
     * Extracts skill names mentioned in the query context and finds
     * related skills/domains via graph traversal.
     */
    public GraphContext enrichWithGraphContext(String query, List<String> retrievedTexts) {
        // 1. Find skill names from retrieved texts via graph lookup
        Set<String> mentionedSkills = findMentionedSkills(query, retrievedTexts);

        if (mentionedSkills.isEmpty()) {
            return GraphContext.EMPTY;
        }

        // 2. Traverse graph for related skills and domains
        Map<String, List<RelatedSkill>> relatedBySkill = new LinkedHashMap<>();
        Set<String> domains = new LinkedHashSet<>();

        try (Session session = neo4jDriver.session()) {
            for (String skillName : mentionedSkills) {
                List<RelatedSkill> related = findRelatedSkills(session, skillName);
                if (!related.isEmpty()) {
                    relatedBySkill.put(skillName, related);
                }

                // Find domain
                String domain = findDomain(session, skillName);
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

    /**
     * Builds a context string from graph relationships for injection into RAG context.
     */
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

        try (Session session = neo4jDriver.session()) {
            Result result = session.run("MATCH (s:Skill) WHERE s.isActive = true RETURN s.name AS name");
            while (result.hasNext()) {
                String skillName = result.next().get("name").asString();
                if (lowerCombined.contains(skillName.toLowerCase())) {
                    mentioned.add(skillName);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to find mentioned skills: {}", e.getMessage());
        }

        return mentioned;
    }

    private List<RelatedSkill> findRelatedSkills(Session session, String skillName) {
        List<RelatedSkill> related = new ArrayList<>();
        try {
            Result result = session.run("""
                MATCH (s:Skill {name: $name})-[r:SIMILAR_TO*1..%d]-(related:Skill)
                WHERE related.isActive = true AND related.name <> $name
                RETURN DISTINCT related.name AS name,
                       min(r[0].weight) AS weight
                ORDER BY weight DESC
                LIMIT $limit
                """.formatted(MAX_GRAPH_HOPS),
                Map.of("name", skillName, "limit", MAX_RELATED_SKILLS));

            while (result.hasNext()) {
                Record record = result.next();
                related.add(new RelatedSkill(
                    record.get("name").asString(),
                    record.get("weight").asDouble()
                ));
            }
        } catch (Exception e) {
            log.debug("Failed to find related skills for '{}': {}", skillName, e.getMessage());
        }
        return related;
    }

    private String findDomain(Session session, String skillName) {
        try {
            Result result = session.run(
                "MATCH (s:Skill {name: $name})-[:BELONGS_TO]->(d:Domain) RETURN d.name AS domain",
                Map.of("name", skillName));
            if (result.hasNext()) {
                return result.next().get("domain").asString();
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
