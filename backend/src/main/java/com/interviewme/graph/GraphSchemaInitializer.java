package com.interviewme.graph;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class GraphSchemaInitializer {

    private final Driver neo4jDriver;

    @PostConstruct
    public void initSchema() {
        log.info("Initializing Neo4j graph schema...");
        try (Session session = neo4jDriver.session()) {
            // Constraints
            session.run("CREATE CONSTRAINT skill_id_unique IF NOT EXISTS FOR (s:Skill) REQUIRE s.id IS UNIQUE");
            session.run("CREATE CONSTRAINT domain_id_unique IF NOT EXISTS FOR (d:Domain) REQUIRE d.id IS UNIQUE");
            session.run("CREATE CONSTRAINT tag_name_unique IF NOT EXISTS FOR (t:Tag) REQUIRE t.name IS UNIQUE");
            session.run("CREATE CONSTRAINT tenant_id_unique IF NOT EXISTS FOR (tn:Tenant) REQUIRE tn.id IS UNIQUE");

            // Indexes
            session.run("CREATE INDEX skill_name_idx IF NOT EXISTS FOR (s:Skill) ON (s.name)");
            session.run("CREATE INDEX skill_category_idx IF NOT EXISTS FOR (s:Skill) ON (s.category)");

            log.info("Neo4j graph schema initialized successfully.");
        } catch (Exception e) {
            log.warn("Neo4j schema initialization skipped (DB may be unavailable): {}", e.getMessage());
        }
    }
}
