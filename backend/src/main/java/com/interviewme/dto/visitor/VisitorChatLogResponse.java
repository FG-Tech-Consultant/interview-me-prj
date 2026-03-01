package com.interviewme.dto.visitor;

import java.time.Instant;

public record VisitorChatLogResponse(
    Long id,
    String role,
    String content,
    Integer tokensUsed,
    Instant createdAt
) {}
