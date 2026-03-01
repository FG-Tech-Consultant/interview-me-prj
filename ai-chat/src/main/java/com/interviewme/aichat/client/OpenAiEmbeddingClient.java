package com.interviewme.aichat.client;

import com.interviewme.aichat.config.AiProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
@ConditionalOnExpression("'${ai.embedding.provider:}' != 'ollama' and '${ai.openai.api-key:}' != ''")
public class OpenAiEmbeddingClient implements EmbeddingClient {

    private final AiProperties aiProperties;
    private final RestClient restClient;

    public OpenAiEmbeddingClient(AiProperties aiProperties) {
        this.aiProperties = aiProperties;
        this.restClient = RestClient.builder()
                .baseUrl(aiProperties.getOpenai().getBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + aiProperties.getOpenai().getApiKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
        log.info("OpenAiEmbeddingClient initialized with model={}", aiProperties.getOpenai().getEmbeddingModel());
    }

    @Override
    public float[] embed(String text) {
        Map<String, Object> body = Map.of(
                "model", aiProperties.getOpenai().getEmbeddingModel(),
                "input", text
        );

        @SuppressWarnings("unchecked")
        Map<String, Object> response = restClient.post()
                .uri("/embeddings")
                .body(body)
                .retrieve()
                .body(Map.class);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> data = (List<Map<String, Object>>) response.get("data");
        @SuppressWarnings("unchecked")
        List<Number> embedding = (List<Number>) data.get(0).get("embedding");

        float[] result = new float[embedding.size()];
        for (int i = 0; i < embedding.size(); i++) {
            result[i] = embedding.get(i).floatValue();
        }
        return result;
    }

    @Override
    public List<float[]> embedBatch(List<String> texts) {
        Map<String, Object> body = Map.of(
                "model", aiProperties.getOpenai().getEmbeddingModel(),
                "input", texts
        );

        @SuppressWarnings("unchecked")
        Map<String, Object> response = restClient.post()
                .uri("/embeddings")
                .body(body)
                .retrieve()
                .body(Map.class);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> data = (List<Map<String, Object>>) response.get("data");

        List<float[]> results = new ArrayList<>();
        for (Map<String, Object> item : data) {
            @SuppressWarnings("unchecked")
            List<Number> embedding = (List<Number>) item.get("embedding");
            float[] arr = new float[embedding.size()];
            for (int i = 0; i < embedding.size(); i++) {
                arr[i] = embedding.get(i).floatValue();
            }
            results.add(arr);
        }
        return results;
    }
}
