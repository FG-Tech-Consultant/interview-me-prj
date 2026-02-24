package com.interviewme.common.dto.ai;

/**
 * Simple LLM client interface for services that use prompt-based requests.
 */
public interface LlmClient {
    LlmResponse complete(LlmRequest request);
}
