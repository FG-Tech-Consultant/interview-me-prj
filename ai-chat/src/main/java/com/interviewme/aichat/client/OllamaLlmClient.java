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

@Component("ollama")
@Slf4j
@ConditionalOnProperty(name = "ai.ollama.base-url")
public class OllamaLlmClient implements LlmClient {

    private final AiProperties aiProperties;
    private final RestClient restClient;

    public OllamaLlmClient(AiProperties aiProperties) {
        this.aiProperties = aiProperties;
        this.restClient = RestClient.builder()
                .baseUrl(aiProperties.getOllama().getBaseUrl())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
        log.info("OllamaLlmClient initialized with base-url={} model={}",
                aiProperties.getOllama().getBaseUrl(), aiProperties.getOllama().getChatModel());
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
                "stream", false,
                "options", Map.of(
                        "num_predict", request.maxTokens(),
                        "temperature", request.temperature()
                )
        );

        long start = System.currentTimeMillis();

        @SuppressWarnings("unchecked")
        Map<String, Object> response = restClient.post()
                .uri("/api/chat")
                .body(body)
                .retrieve()
                .body(Map.class);

        long latency = System.currentTimeMillis() - start;

        @SuppressWarnings("unchecked")
        Map<String, Object> message = (Map<String, Object>) response.get("message");
        String content = (String) message.get("content");

        int totalTokens = 0;
        if (response.containsKey("eval_count")) {
            totalTokens = ((Number) response.get("eval_count")).intValue();
        }
        if (response.containsKey("prompt_eval_count")) {
            totalTokens += ((Number) response.get("prompt_eval_count")).intValue();
        }

        return new LlmResponse(content, totalTokens, getProvider(), getModel(), latency);
    }

    @Override
    public String getModel() {
        return aiProperties.getOllama().getChatModel();
    }

    @Override
    public String getProvider() {
        return "ollama";
    }
}
