package com.interviewme.common.dto.ai;

/**
 * Simple LLM response DTO.
 */
public record LlmResponse(
    String content,
    int totalTokens,
    long latencyMs
) {}
