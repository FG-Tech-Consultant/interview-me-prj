package com.interviewme.graph;

import com.ladybugdb.Connection;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class GraphSchemaInitializer {

    private final Connection graphConnection;

    @PostConstruct
    public void initSchema() {
        log.info("Initializing LadybugDB graph schema...");
        try {
            graphConnection.query("""
                    CREATE NODE TABLE IF NOT EXISTS Skill(
                        id STRING PRIMARY KEY,
                        name STRING,
                        category STRING,
                        description STRING,
                        isActive BOOLEAN,
                        updatedAt STRING
                    )
                    """);

            graphConnection.query("""
                    CREATE NODE TABLE IF NOT EXISTS Domain(
                        id STRING PRIMARY KEY,
                        name STRING
                    )
                    """);

            graphConnection.query("""
                    CREATE NODE TABLE IF NOT EXISTS Tag(
                        name STRING PRIMARY KEY
                    )
                    """);

            graphConnection.query("""
                    CREATE NODE TABLE IF NOT EXISTS Tenant(
                        id STRING PRIMARY KEY
                    )
                    """);

            graphConnection.query("""
                    CREATE REL TABLE IF NOT EXISTS BELONGS_TO(
                        FROM Skill TO Domain
                    )
                    """);

            graphConnection.query("""
                    CREATE REL TABLE IF NOT EXISTS SIMILAR_TO(
                        FROM Skill TO Skill,
                        weight DOUBLE
                    )
                    """);

            log.info("LadybugDB graph schema initialized successfully.");
        } catch (Exception e) {
            log.warn("LadybugDB schema initialization failed: {}", e.getMessage());
        }
    }
}
