package com.interviewme.dto.visitor;

import java.time.Instant;

public record VisitorResponse(
    Long id,
    String name,
    String company,
    String jobRole,
    String linkedinUrl,
    String contactEmail,
    String contactWhatsapp,
    Instant createdAt,
    int sessionCount,
    int totalMessages,
    boolean isRevealed
) {}
