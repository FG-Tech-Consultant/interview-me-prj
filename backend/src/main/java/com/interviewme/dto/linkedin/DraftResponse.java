package com.interviewme.dto.linkedin;

import java.time.Instant;

public record DraftResponse(
    Long id,
    String originalMessage,
    String category,
    String suggestedReply,
    String tone,
    String status,
    Instant createdAt,
    Instant updatedAt
) {}
