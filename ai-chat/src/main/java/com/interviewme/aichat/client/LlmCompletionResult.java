package com.interviewme.aichat.client;

/**
 * Wraps both the LLM request that was sent and the response that was received,
 * enabling audit logging of the full prompt/messages sent to the LLM provider.
 */
public record LlmCompletionResult(
    LlmRequest request,
    LlmResponse response
) {}
