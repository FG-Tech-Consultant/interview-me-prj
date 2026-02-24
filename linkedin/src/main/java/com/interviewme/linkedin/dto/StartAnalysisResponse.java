package com.interviewme.linkedin.dto;

public record StartAnalysisResponse(
    Long analysisId,
    String status,
    String message
) {}
