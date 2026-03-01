package com.interviewme.aichat.service;

import com.interviewme.aichat.client.LlmChatMessage;
import com.interviewme.aichat.client.LlmClient;
import com.interviewme.aichat.client.LlmCompletionResult;
import com.interviewme.aichat.client.LlmRequest;
import com.interviewme.aichat.client.LlmResponse;
import com.interviewme.aichat.config.AiProperties;
import com.interviewme.aichat.exception.LlmUnavailableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
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

    /**
     * @deprecated Use {@link #completeWithRequest(Long, String, List)} instead to capture the full LLM request for audit logging.
     */
    @Deprecated
    public LlmResponse complete(Long tenantId, String systemPrompt, List<LlmChatMessage> history) {
        return completeWithRequest(tenantId, systemPrompt, history).response();
    }

    /**
     * Sends a chat completion request to the default LLM provider and returns both
     * the request that was sent and the response that was received.
     */
    public LlmCompletionResult completeWithRequest(Long tenantId, String systemPrompt, List<LlmChatMessage> history) {
        return completeWithRequest(tenantId, null, null, systemPrompt, history);
    }

    /**
     * Sends a chat completion request to the specified LLM provider (or default if null)
     * with an optional model override, and returns both the request and response.
     */
    public LlmCompletionResult completeWithRequest(Long tenantId, String providerOverride, String modelOverride,
                                                     String systemPrompt, List<LlmChatMessage> history) {
        String provider = (providerOverride != null) ? providerOverride : aiProperties.getDefaultProvider();
        LlmClient client = clients.get(provider);

        if (client == null) {
            log.warn("Provider {} not available, falling back to default: {}", provider, aiProperties.getDefaultProvider());
            provider = aiProperties.getDefaultProvider();
            client = clients.get(provider);
        }

        if (client == null) {
            throw new LlmUnavailableException("No LLM client configured for provider: " + provider);
        }

        LlmRequest request = new LlmRequest(
                systemPrompt,
                history,
                aiProperties.getChat().getMaxTokens(),
                aiProperties.getChat().getTemperature(),
                modelOverride
        );

        String effectiveModel = modelOverride != null ? modelOverride : client.getModel();
        log.info("Calling LLM provider={} model={} tenantId={}", provider, effectiveModel, tenantId);

        long start = System.currentTimeMillis();
        try {
            LlmResponse response = client.complete(request);
            long latency = System.currentTimeMillis() - start;

            log.info("LLM response received provider={} tokens={} latencyMs={}",
                    provider, response.tokensUsed(), latency);

            LlmResponse enrichedResponse = new LlmResponse(
                    response.content(),
                    response.tokensUsed(),
                    provider,
                    effectiveModel,
                    latency
            );

            return new LlmCompletionResult(request, enrichedResponse);
        } catch (Exception e) {
            long latency = System.currentTimeMillis() - start;
            log.error("LLM call failed provider={} latencyMs={} error={}", provider, latency, e.getMessage());
            throw new LlmUnavailableException("AI service temporarily unavailable", e);
        }
    }

    /**
     * Returns the list of available (configured) LLM provider names.
     */
    public List<String> getAvailableProviders() {
        return new ArrayList<>(clients.keySet());
    }

    /**
     * Returns the default model name for the given provider, or null if the provider is not available.
     */
    public String getDefaultModel(String provider) {
        LlmClient client = clients.get(provider);
        return client != null ? client.getModel() : null;
    }
}
