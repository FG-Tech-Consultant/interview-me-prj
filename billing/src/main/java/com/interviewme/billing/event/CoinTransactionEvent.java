package com.interviewme.billing.event;

import java.time.Instant;

public record CoinTransactionEvent(
    Long tenantId,
    Long walletId,
    Long transactionId,
    String type,
    int amount,
    String refType,
    String refId,
    Instant timestamp
) {}
