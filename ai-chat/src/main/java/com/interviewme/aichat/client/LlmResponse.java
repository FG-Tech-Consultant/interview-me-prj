package com.interviewme.aichat.client;

public record LlmResponse(
    String content,
    int tokensUsed,
    String provider,
    String model,
    long latencyMs
) {}
