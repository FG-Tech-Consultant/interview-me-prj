package com.interviewme.config;

import com.interviewme.aichat.client.LlmClient;
import com.interviewme.aichat.config.AiProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class LlmHealthIndicator implements HealthIndicator {

    private final AiProperties aiProperties;
    private final Map<String, LlmClient> clients;

    @Override
    public Health health() {
        String provider = aiProperties.getDefaultProvider();
        LlmClient client = clients.get(provider);

        if (client == null) {
            return Health.down()
                    .withDetail("provider", provider)
                    .withDetail("error", "No client configured for provider")
                    .build();
        }

        String baseUrl = resolveBaseUrl(provider);
        if (baseUrl == null || baseUrl.isBlank()) {
            return Health.down()
                    .withDetail("provider", provider)
                    .withDetail("model", client.getModel())
                    .withDetail("error", "No base URL configured")
                    .build();
        }

        try {
            long start = System.currentTimeMillis();
            boolean reachable = checkConnectivity(baseUrl);
            long latency = System.currentTimeMillis() - start;

            if (reachable) {
                return Health.up()
                        .withDetail("provider", provider)
                        .withDetail("model", client.getModel())
                        .withDetail("baseUrl", baseUrl)
                        .withDetail("responseTimeMs", latency)
                        .build();
            } else {
                return Health.down()
                        .withDetail("provider", provider)
                        .withDetail("model", client.getModel())
                        .withDetail("baseUrl", baseUrl)
                        .withDetail("error", "Provider not reachable")
                        .build();
            }
        } catch (Exception e) {
            log.warn("LLM health check failed for provider={}: {}", provider, e.getMessage());
            return Health.down()
                    .withDetail("provider", provider)
                    .withDetail("model", client.getModel())
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }

    private boolean checkConnectivity(String baseUrl) {
        try (HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build()) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl))
                    .timeout(Duration.ofSeconds(5))
                    .method("HEAD", HttpRequest.BodyPublishers.noBody())
                    .build();
            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            // Any response (even 401/403) means the service is reachable
            return response.statusCode() < 500;
        } catch (Exception e) {
            return false;
        }
    }

    private String resolveBaseUrl(String provider) {
        return switch (provider.toLowerCase()) {
            case "openai" -> aiProperties.getOpenai().getBaseUrl();
            case "gemini" -> aiProperties.getGemini().getBaseUrl();
            case "claude" -> aiProperties.getClaude().getBaseUrl();
            case "ollama" -> aiProperties.getOllama().getBaseUrl();
            default -> null;
        };
    }
}
