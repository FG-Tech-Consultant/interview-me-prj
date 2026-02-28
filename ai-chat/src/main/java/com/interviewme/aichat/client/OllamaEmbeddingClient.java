package com.interviewme.aichat.client;

import com.interviewme.aichat.config.AiProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
@ConditionalOnExpression("'${ai.ollama.base-url:}' != ''")
@ConditionalOnMissingBean(OpenAiEmbeddingClient.class)
public class OllamaEmbeddingClient implements EmbeddingClient {

    private final AiProperties aiProperties;
    private final RestClient restClient;

    public OllamaEmbeddingClient(AiProperties aiProperties) {
        this.aiProperties = aiProperties;
        this.restClient = RestClient.builder()
                .baseUrl(aiProperties.getOllama().getBaseUrl())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
        log.info("OllamaEmbeddingClient initialized with base-url={} model={}",
                aiProperties.getOllama().getBaseUrl(), aiProperties.getOllama().getEmbeddingModel());
    }

    @Override
    public float[] embed(String text) {
        Map<String, Object> body = Map.of(
                "model", aiProperties.getOllama().getEmbeddingModel(),
                "input", text
        );

        @SuppressWarnings("unchecked")
        Map<String, Object> response = restClient.post()
                .uri("/api/embed")
                .body(body)
                .retrieve()
                .body(Map.class);

        @SuppressWarnings("unchecked")
        List<List<Number>> embeddings = (List<List<Number>>) response.get("embeddings");
        List<Number> embedding = embeddings.get(0);

        float[] result = new float[embedding.size()];
        for (int i = 0; i < embedding.size(); i++) {
            result[i] = embedding.get(i).floatValue();
        }
        return result;
    }

    @Override
    public List<float[]> embedBatch(List<String> texts) {
        Map<String, Object> body = Map.of(
                "model", aiProperties.getOllama().getEmbeddingModel(),
                "input", texts
        );

        @SuppressWarnings("unchecked")
        Map<String, Object> response = restClient.post()
                .uri("/api/embed")
                .body(body)
                .retrieve()
                .body(Map.class);

        @SuppressWarnings("unchecked")
        List<List<Number>> embeddings = (List<List<Number>>) response.get("embeddings");

        List<float[]> results = new ArrayList<>();
        for (List<Number> embedding : embeddings) {
            float[] arr = new float[embedding.size()];
            for (int i = 0; i < embedding.size(); i++) {
                arr[i] = embedding.get(i).floatValue();
            }
            results.add(arr);
        }
        return results;
    }
}
