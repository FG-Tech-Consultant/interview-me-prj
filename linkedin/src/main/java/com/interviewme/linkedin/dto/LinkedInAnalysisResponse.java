package com.interviewme.linkedin.dto;

import java.time.Instant;
import java.util.List;

public record LinkedInAnalysisResponse(
    Long id,
    Long profileId,
    String status,
    Integer overallScore,
    String errorMessage,
    String pdfFilename,
    Instant analyzedAt,
    List<SectionScoreResponse> sections,
    Instant createdAt
) {}
