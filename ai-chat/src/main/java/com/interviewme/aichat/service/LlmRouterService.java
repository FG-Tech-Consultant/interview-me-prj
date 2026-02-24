package com.interviewme.aichat.service;

import com.interviewme.aichat.client.LlmChatMessage;
import com.interviewme.aichat.client.LlmClient;
import com.interviewme.aichat.client.LlmRequest;
import com.interviewme.aichat.client.LlmResponse;
import com.interviewme.aichat.config.AiProperties;
import com.interviewme.aichat.exception.LlmUnavailableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class LlmRouterService {

    private final Map<String, LlmClient> clients;
    private final AiProperties aiProperties;

    public LlmRouterService(Map<String, LlmClient> clients, AiProperties aiProperties) {
        this.clients = clients;
        this.aiProperties = aiProperties;
        log.info("LlmRouterService initialized with providers: {}", clients.keySet());
    }

    public LlmResponse complete(Long tenantId, String systemPrompt, List<LlmChatMessage> history) {
        String provider = aiProperties.getDefaultProvider();
        LlmClient client = clients.get(provider);

        if (client == null) {
            throw new LlmUnavailableException("No LLM client configured for provider: " + provider);
        }

        LlmRequest request = new LlmRequest(
                systemPrompt,
                history,
                aiProperties.getChat().getMaxTokens(),
                aiProperties.getChat().getTemperature()
        );

        log.info("Calling LLM provider={} model={} tenantId={}", provider, client.getModel(), tenantId);

        long start = System.currentTimeMillis();
        try {
            LlmResponse response = client.complete(request);
            long latency = System.currentTimeMillis() - start;

            log.info("LLM response received provider={} tokens={} latencyMs={}",
                    provider, response.tokensUsed(), latency);

            return new LlmResponse(
                    response.content(),
                    response.tokensUsed(),
                    provider,
                    client.getModel(),
                    latency
            );
        } catch (Exception e) {
            long latency = System.currentTimeMillis() - start;
            log.error("LLM call failed provider={} latencyMs={} error={}", provider, latency, e.getMessage());
            throw new LlmUnavailableException("AI service temporarily unavailable", e);
        }
    }
}
