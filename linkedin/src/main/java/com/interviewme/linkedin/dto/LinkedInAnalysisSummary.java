package com.interviewme.linkedin.dto;

import java.time.Instant;

public record LinkedInAnalysisSummary(
    Long id,
    String status,
    Integer overallScore,
    String pdfFilename,
    Instant analyzedAt,
    Instant createdAt
) {}
