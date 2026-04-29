package com.interviewme.graph;

import com.interviewme.model.Skill;
import com.ladybugdb.Connection;
import com.ladybugdb.QueryResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class GraphSkillSyncService {

    private final Connection graphConnection;

    public synchronized void syncSkill(Skill skill) {
        try {
            String id = skill.getId().toString();
            String name = escape(skill.getName());
            String category = escape(skill.getCategory());
            String description = escape(skill.getDescription() != null ? skill.getDescription() : "");
            boolean isActive = skill.getIsActive() != null && skill.getIsActive();
            String updatedAt = skill.getUpdatedAt() != null ? skill.getUpdatedAt().toString() : "";
            String categoryId = slugify(skill.getCategory());

            graphConnection.query(
                    "MERGE (s:Skill {id: '%s'}) SET s.name = '%s', s.category = '%s', s.description = '%s', s.isActive = %s, s.updatedAt = '%s'"
                            .formatted(id, name, category, description, isActive, updatedAt));

            graphConnection.query(
                    "MERGE (d:Domain {id: '%s'}) SET d.name = '%s'"
                            .formatted(categoryId, category));

            graphConnection.query(
                    "MATCH (s:Skill {id: '%s'}), (d:Domain {id: '%s'}) MERGE (s)-[:BELONGS_TO]->(d)"
                            .formatted(id, categoryId));

            log.debug("Synced skill to graph: {} (id={})", skill.getName(), skill.getId());
        } catch (Exception e) {
            log.warn("Failed to sync skill {} to LadybugDB: {}", skill.getId(), e.getMessage());
        }
    }

    public synchronized void linkSimilarSkills(String skillId1, String skillId2, double weight) {
        try {
            graphConnection.query(
                    "MATCH (a:Skill {id: '%s'}), (b:Skill {id: '%s'}) MERGE (a)-[r:SIMILAR_TO]->(b) SET r.weight = %f"
                            .formatted(skillId1, skillId2, weight));
        } catch (Exception e) {
            log.warn("Failed to link skills {} and {}: {}", skillId1, skillId2, e.getMessage());
        }
    }

    public synchronized long countSkillNodes() {
        try {
            QueryResult result = graphConnection.query("MATCH (s:Skill) RETURN count(s) AS cnt");
            if (result.hasNext()) {
                return result.getNext().getValue(0).getValue();
            }
        } catch (Exception e) {
            log.warn("Failed to count skill nodes: {}", e.getMessage());
        }
        return 0;
    }

    private String slugify(String input) {
        return input.toLowerCase().replaceAll("[^a-z0-9]+", "-");
    }

    private String escape(String input) {
        if (input == null) return "";
        return input.replace("'", "\\'").replace("\\", "\\\\");
    }
}
