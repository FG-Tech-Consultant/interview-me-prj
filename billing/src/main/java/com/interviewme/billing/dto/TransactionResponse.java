package com.interviewme.billing.dto;

import java.time.Instant;

public record TransactionResponse(
    Long id,
    String type,
    int amount,
    String description,
    String refType,
    String refId,
    Instant createdAt
) {}
