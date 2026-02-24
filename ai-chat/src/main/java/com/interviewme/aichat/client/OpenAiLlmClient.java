package com.interviewme.aichat.client;

import com.interviewme.aichat.config.AiProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component("openai")
@Slf4j
@ConditionalOnProperty(name = "ai.openai.api-key")
public class OpenAiLlmClient implements LlmClient {

    private final AiProperties aiProperties;
    private final RestClient restClient;

    public OpenAiLlmClient(AiProperties aiProperties) {
        this.aiProperties = aiProperties;
        this.restClient = RestClient.builder()
                .baseUrl(aiProperties.getOpenai().getBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + aiProperties.getOpenai().getApiKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public LlmResponse complete(LlmRequest request) {
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", request.systemPrompt()));
        for (LlmChatMessage msg : request.messages()) {
            messages.add(Map.of("role", msg.role(), "content", msg.content()));
        }

        Map<String, Object> body = Map.of(
                "model", getModel(),
                "messages", messages,
                "max_tokens", request.maxTokens(),
                "temperature", request.temperature()
        );

        long start = System.currentTimeMillis();

        @SuppressWarnings("unchecked")
        Map<String, Object> response = restClient.post()
                .uri("/chat/completions")
                .body(body)
                .retrieve()
                .body(Map.class);

        long latency = System.currentTimeMillis() - start;

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
        @SuppressWarnings("unchecked")
        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        String content = (String) message.get("content");

        @SuppressWarnings("unchecked")
        Map<String, Object> usage = (Map<String, Object>) response.get("usage");
        int totalTokens = ((Number) usage.get("total_tokens")).intValue();

        return new LlmResponse(content, totalTokens, getProvider(), getModel(), latency);
    }

    @Override
    public String getModel() {
        return aiProperties.getOpenai().getChatModel();
    }

    @Override
    public String getProvider() {
        return "openai";
    }
}
