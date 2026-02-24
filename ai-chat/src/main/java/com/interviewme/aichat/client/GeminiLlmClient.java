package com.interviewme.aichat.client;

import com.interviewme.aichat.config.AiProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component("gemini")
@Slf4j
@ConditionalOnProperty(name = "ai.gemini.api-key")
public class GeminiLlmClient implements LlmClient {

    private final AiProperties aiProperties;
    private final RestClient restClient;

    public GeminiLlmClient(AiProperties aiProperties) {
        this.aiProperties = aiProperties;
        this.restClient = RestClient.builder()
                .baseUrl(aiProperties.getGemini().getBaseUrl())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public LlmResponse complete(LlmRequest request) {
        List<Map<String, Object>> contents = new ArrayList<>();

        // Add system instruction as first user message (Gemini style)
        for (LlmChatMessage msg : request.messages()) {
            String role = "user".equals(msg.role()) ? "user" : "model";
            contents.add(Map.of(
                    "role", role,
                    "parts", List.of(Map.of("text", msg.content()))
            ));
        }

        Map<String, Object> body = Map.of(
                "contents", contents,
                "systemInstruction", Map.of(
                        "parts", List.of(Map.of("text", request.systemPrompt()))
                ),
                "generationConfig", Map.of(
                        "maxOutputTokens", request.maxTokens(),
                        "temperature", request.temperature()
                )
        );

        long start = System.currentTimeMillis();

        @SuppressWarnings("unchecked")
        Map<String, Object> response = restClient.post()
                .uri("/models/{model}:generateContent?key={key}",
                        getModel(), aiProperties.getGemini().getApiKey())
                .body(body)
                .retrieve()
                .body(Map.class);

        long latency = System.currentTimeMillis() - start;

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
        @SuppressWarnings("unchecked")
        Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
        String text = (String) parts.get(0).get("text");

        int totalTokens = 0;
        @SuppressWarnings("unchecked")
        Map<String, Object> usageMetadata = (Map<String, Object>) response.get("usageMetadata");
        if (usageMetadata != null) {
            totalTokens = ((Number) usageMetadata.getOrDefault("totalTokenCount", 0)).intValue();
        }

        return new LlmResponse(text, totalTokens, getProvider(), getModel(), latency);
    }

    @Override
    public String getModel() {
        return aiProperties.getGemini().getChatModel();
    }

    @Override
    public String getProvider() {
        return "gemini";
    }
}
