package com.interviewme.exports.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "exports")
@Data
public class ExportProperties {

    private String storagePath = "./exports";
    private RetryConfig retry = new RetryConfig();

    @Data
    public static class RetryConfig {
        private int maxAttempts = 3;
        private int initialDelayMs = 1000;
        private int multiplier = 2;
    }
}
