package com.interviewme.graph;

import com.ladybugdb.Database;
import com.ladybugdb.Connection;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class LadybugDbConfig {

    @Value("${ladybugdb.path:./data/graph.lbug}")
    private String dbPath;

    private Database database;
    private Connection connection;

    @Bean
    public Database ladybugDatabase() {
        log.info("Initializing LadybugDB at: {}", dbPath);
        java.io.File dir = new java.io.File(dbPath).getParentFile();
        if (dir != null && !dir.exists()) {
            dir.mkdirs();
        }
        database = new Database(dbPath);
        return database;
    }

    @Bean
    public Connection ladybugConnection(Database db) {
        connection = new Connection(db);
        return connection;
    }

    @PreDestroy
    public void cleanup() {
        log.info("Shutting down LadybugDB...");
        if (connection != null) {
            try { connection.close(); } catch (Exception e) { /* ignore */ }
        }
        if (database != null) {
            try { database.close(); } catch (Exception e) { /* ignore */ }
        }
    }
}
