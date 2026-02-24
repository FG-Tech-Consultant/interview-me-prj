package com.interviewme.aichat.event;

import java.time.Instant;

public record ChatMessageEvent(
    Long tenantId,
    Long sessionId,
    Long messageId,
    String llmProvider,
    int tokensUsed,
    Instant timestamp
) {}
