package com.interviewme.exports.dto;

import java.time.Instant;

public record ExportStatusResponse(
    Long id,
    String status,
    String errorMessage,
    Integer retryCount,
    Instant completedAt
) {}
