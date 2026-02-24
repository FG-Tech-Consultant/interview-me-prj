package com.interviewme.aichat.client;

import java.util.List;

public record LlmRequest(
    String systemPrompt,
    List<LlmChatMessage> messages,
    int maxTokens,
    double temperature
) {}
