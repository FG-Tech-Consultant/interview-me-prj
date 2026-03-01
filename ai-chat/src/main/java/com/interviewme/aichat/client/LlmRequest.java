package com.interviewme.aichat.client;

import java.util.List;

public record LlmRequest(
    String systemPrompt,
    List<LlmChatMessage> messages,
    int maxTokens,
    double temperature,
    String modelOverride
) {
    /**
     * Backward-compatible constructor without model override.
     */
    public LlmRequest(String systemPrompt, List<LlmChatMessage> messages, int maxTokens, double temperature) {
        this(systemPrompt, messages, maxTokens, temperature, null);
    }
}
