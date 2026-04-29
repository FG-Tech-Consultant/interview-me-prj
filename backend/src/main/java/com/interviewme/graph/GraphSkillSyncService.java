package com.interviewme.graph;

import com.interviewme.model.Skill;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class GraphSkillSyncService {

    private final Driver neo4jDriver;

    /**
     * Upserts a Skill node in Neo4j from the PostgreSQL entity.
     */
    public void syncSkill(Skill skill) {
        try (Session session = neo4jDriver.session()) {
            session.run("""
                    MERGE (s:Skill {id: $id})
                    SET s.name = $name,
                        s.category = $category,
                        s.description = $description,
                        s.isActive = $isActive,
                        s.updatedAt = $updatedAt
                    MERGE (d:Domain {id: $categoryId})
                    SET d.name = $category
                    MERGE (s)-[:BELONGS_TO]->(d)
                    """,
                    Map.of(
                            "id", skill.getId().toString(),
                            "name", skill.getName(),
                            "category", skill.getCategory(),
                            "categoryId", slugify(skill.getCategory()),
                            "description", skill.getDescription() != null ? skill.getDescription() : "",
                            "isActive", skill.getIsActive() != null && skill.getIsActive(),
                            "updatedAt", skill.getUpdatedAt() != null ? skill.getUpdatedAt().toString() : ""
                    ));
            log.debug("Synced skill to graph: {} (id={})", skill.getName(), skill.getId());
        } catch (Exception e) {
            log.warn("Failed to sync skill {} to Neo4j: {}", skill.getId(), e.getMessage());
        }
    }

    /**
     * Creates a SIMILAR_TO relationship between two skill nodes.
     */
    public void linkSimilarSkills(String skillId1, String skillId2, double weight) {
        try (Session session = neo4jDriver.session()) {
            session.run("""
                    MATCH (a:Skill {id: $id1}), (b:Skill {id: $id2})
                    MERGE (a)-[r:SIMILAR_TO]-(b)
                    SET r.weight = $weight
                    """,
                    Map.of("id1", skillId1, "id2", skillId2, "weight", weight));
        } catch (Exception e) {
            log.warn("Failed to link skills {} and {}: {}", skillId1, skillId2, e.getMessage());
        }
    }

    /**
     * Counts Skill nodes in the graph — used to detect empty graph for seeding.
     */
    public long countSkillNodes() {
        try (Session session = neo4jDriver.session()) {
            var result = session.run("MATCH (s:Skill) RETURN count(s) AS cnt");
            if (result.hasNext()) {
                return result.next().get("cnt").asLong();
            }
        } catch (Exception e) {
            log.warn("Failed to count skill nodes: {}", e.getMessage());
        }
        return 0;
    }

    private String slugify(String input) {
        return input.toLowerCase().replaceAll("[^a-z0-9]+", "-");
    }
}
