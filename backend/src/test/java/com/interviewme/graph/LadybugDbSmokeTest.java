package com.interviewme.graph;

import com.ladybugdb.Connection;
import com.ladybugdb.Database;
import com.ladybugdb.FlatTuple;
import com.ladybugdb.QueryResult;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LadybugDbSmokeTest {

    @TempDir
    static Path tempDir;

    private static Database db;
    private static Connection conn;

    @BeforeAll
    static void setup() {
        db = new Database(tempDir.resolve("test.lbug").toString());
        conn = new Connection(db);

        conn.query("CREATE NODE TABLE Skill(id INT64 PRIMARY KEY, name STRING, category STRING, isActive BOOLEAN)");
        conn.query("CREATE REL TABLE SIMILAR_TO(FROM Skill TO Skill, weight DOUBLE)");
        conn.query("CREATE REL TABLE REQUIRES(FROM Skill TO Skill)");
    }

    @AfterAll
    static void teardown() {
        if (conn != null) conn.close();
        if (db != null) db.close();
    }

    @Test
    void shouldCreateSkillNodesAndSimilarToRelationship() {
        conn.query("CREATE (s1:Skill {id: 1, name: 'Java', category: 'Programming Language', isActive: true})");
        conn.query("CREATE (s2:Skill {id: 2, name: 'Spring Boot', category: 'Framework', isActive: true})");

        conn.query("""
                MATCH (s1:Skill {id: 1}), (s2:Skill {id: 2})
                CREATE (s1)-[:SIMILAR_TO {weight: 0.8}]->(s2)
                """);

        QueryResult result = conn.query("""
                MATCH (s1:Skill {name: 'Java'})-[:SIMILAR_TO]->(s2:Skill)
                RETURN s2.name AS relatedSkill, s2.category AS category
                """);

        assertTrue(result.hasNext(), "Should find related skills");
        FlatTuple row = result.getNext();
        assertEquals("Spring Boot", row.getValue(0).getValue().toString());
        assertEquals("Framework", row.getValue(1).getValue().toString());
        assertFalse(result.hasNext(), "Should have exactly one related skill");
    }

    @Test
    void shouldTraverseMultipleHops() {
        conn.query("""
                CREATE (s1:Skill {id: 10, name: 'Python', category: 'Programming Language', isActive: true})
                CREATE (s2:Skill {id: 11, name: 'Django', category: 'Framework', isActive: true})
                CREATE (s3:Skill {id: 12, name: 'REST APIs', category: 'Concept', isActive: true})
                CREATE (s1)-[:REQUIRES]->(s2)
                CREATE (s2)-[:REQUIRES]->(s3)
                """);

        QueryResult result = conn.query("""
                MATCH (start:Skill {name: 'Python'})-[:REQUIRES*1..2]->(related:Skill)
                RETURN related.name AS skill ORDER BY skill
                """);

        List<String> skills = new ArrayList<>();
        while (result.hasNext()) {
            skills.add(result.getNext().getValue(0).getValue().toString());
        }
        assertEquals(2, skills.size());
        assertTrue(skills.contains("Django"));
        assertTrue(skills.contains("REST APIs"));
    }
}
