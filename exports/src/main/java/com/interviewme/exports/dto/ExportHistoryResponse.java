package com.interviewme.exports.dto;

import java.time.Instant;
import java.util.Map;

public record ExportHistoryResponse(
    Long id,
    ExportTemplateResponse template,
    String type,
    String status,
    Map<String, Object> parameters,
    Integer coinsSpent,
    String errorMessage,
    Integer retryCount,
    Instant createdAt,
    Instant completedAt
) {}
