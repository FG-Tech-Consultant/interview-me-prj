package com.interviewme.common.dto.ai;

/**
 * Simple LLM request DTO for services that build prompt strings.
 */
public record LlmRequest(
    String prompt,
    String actionType
) {}
