package com.interviewme.graph;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.*;
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
class Neo4jSmokeTest {

    @Container
    private static final Neo4jContainer<?> neo4j = new Neo4jContainer<>("neo4j:5-community")
            .withoutAuthentication();

    private static Driver driver;

    @BeforeAll
    static void setup() {
        driver = GraphDatabase.driver(neo4j.getBoltUrl(), AuthTokens.none());
    }

    @AfterAll
    static void teardown() {
        if (driver != null) {
            driver.close();
        }
    }

    @Test
    void shouldCreateSkillNodesAndSimilarToRelationship() {
        try (Session session = driver.session()) {
            // Create 2 skill nodes
            session.run("CREATE (s1:Skill {id: 1, name: 'Java', category: 'Programming Language'})");
            session.run("CREATE (s2:Skill {id: 2, name: 'Spring Boot', category: 'Framework'})");

            // Create SIMILAR_TO relationship with weight
            session.run("""
                MATCH (s1:Skill {id: 1}), (s2:Skill {id: 2})
                CREATE (s1)-[:SIMILAR_TO {weight: 0.8}]->(s2)
                """);

            // Query traversal: find skills similar to Java
            var result = session.run("""
                MATCH (s1:Skill {name: 'Java'})-[:SIMILAR_TO]->(s2:Skill)
                RETURN s2.name AS relatedSkill, s2.category AS category
                """);

            assertTrue(result.hasNext(), "Should find related skills");
            var record = result.next();
            assertEquals("Spring Boot", record.get("relatedSkill").asString());
            assertEquals("Framework", record.get("category").asString());
            assertFalse(result.hasNext(), "Should have exactly one related skill");
        }
    }

    @Test
    void shouldTraverseMultipleHops() {
        try (Session session = driver.session()) {
            // Create a chain: Python -> Django -> REST APIs
            session.run("""
                CREATE (s1:Skill {id: 10, name: 'Python', category: 'Programming Language'})
                CREATE (s2:Skill {id: 11, name: 'Django', category: 'Framework'})
                CREATE (s3:Skill {id: 12, name: 'REST APIs', category: 'Concept'})
                CREATE (s1)-[:REQUIRES]->(s2)
                CREATE (s2)-[:REQUIRES]->(s3)
                """);

            // 2-hop traversal: find skills reachable from Python within 2 hops
            var result = session.run("""
                MATCH (start:Skill {name: 'Python'})-[:REQUIRES*1..2]->(related:Skill)
                RETURN related.name AS skill ORDER BY skill
                """);

            var skills = result.list(r -> r.get("skill").asString());
            assertEquals(2, skills.size());
            assertTrue(skills.contains("Django"));
            assertTrue(skills.contains("REST APIs"));
        }
    }
}
