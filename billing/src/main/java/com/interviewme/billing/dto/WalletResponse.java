package com.interviewme.billing.dto;

import java.time.Instant;

public record WalletResponse(
    Long id,
    Long tenantId,
    long balance,
    Instant createdAt,
    Instant updatedAt
) {}
