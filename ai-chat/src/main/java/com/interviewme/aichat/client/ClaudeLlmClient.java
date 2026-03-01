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

@Component("claude")
@Slf4j
@ConditionalOnProperty(name = "ai.claude.api-key")
public class ClaudeLlmClient implements LlmClient {

    private final AiProperties aiProperties;
    private final RestClient restClient;

    public ClaudeLlmClient(AiProperties aiProperties) {
        this.aiProperties = aiProperties;
        this.restClient = RestClient.builder()
                .baseUrl(aiProperties.getClaude().getBaseUrl())
                .defaultHeader("x-api-key", aiProperties.getClaude().getApiKey())
                .defaultHeader("anthropic-version", "2023-06-01")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public LlmResponse complete(LlmRequest request) {
        String model = request.modelOverride() != null ? request.modelOverride() : getModel();

        List<Map<String, String>> messages = new ArrayList<>();
        for (LlmChatMessage msg : request.messages()) {
            messages.add(Map.of("role", msg.role(), "content", msg.content()));
        }

        Map<String, Object> body = Map.of(
                "model", model,
                "system", request.systemPrompt(),
                "messages", messages,
                "max_tokens", request.maxTokens(),
                "temperature", request.temperature()
        );

        long start = System.currentTimeMillis();

        @SuppressWarnings("unchecked")
        Map<String, Object> response = restClient.post()
                .uri("/messages")
                .body(body)
                .retrieve()
                .body(Map.class);

        long latency = System.currentTimeMillis() - start;

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> contentBlocks = (List<Map<String, Object>>) response.get("content");
        String text = (String) contentBlocks.getFirst().get("text");

        @SuppressWarnings("unchecked")
        Map<String, Object> usage = (Map<String, Object>) response.get("usage");
        int inputTokens = ((Number) usage.get("input_tokens")).intValue();
        int outputTokens = ((Number) usage.get("output_tokens")).intValue();

        return new LlmResponse(text, inputTokens + outputTokens, getProvider(), model, latency);
    }

    @Override
    public String getModel() {
        return aiProperties.getClaude().getChatModel();
    }

    @Override
    public String getProvider() {
        return "claude";
    }
}
