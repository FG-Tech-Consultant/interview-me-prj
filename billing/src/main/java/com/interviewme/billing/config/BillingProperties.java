package com.interviewme.billing.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "billing")
@Data
public class BillingProperties {

    private Map<String, Integer> costs = new HashMap<>();
    private FreeTierConfig freeTier = new FreeTierConfig();
    private RetryConfig retry = new RetryConfig();

    @Data
    public static class FreeTierConfig {
        private int chatMessagesPerMonth = 50;
        private int linkedinDraftsPerMonth = 10;
    }

    @Data
    public static class RetryConfig {
        private int maxAttempts = 3;
        private int backoffMs = 100;
    }
}
