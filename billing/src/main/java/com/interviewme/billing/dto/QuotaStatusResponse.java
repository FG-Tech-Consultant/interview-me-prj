package com.interviewme.billing.dto;

public record QuotaStatusResponse(
    String featureType,
    int used,
    int limit,
    boolean quotaExceeded,
    String yearMonth
) {}
