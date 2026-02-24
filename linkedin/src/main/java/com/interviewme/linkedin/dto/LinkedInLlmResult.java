package com.interviewme.linkedin.dto;

import java.util.List;

public record LinkedInLlmResult(
    int overallScore,
    List<SectionResult> sections
) {
    public record SectionResult(
        String sectionName,
        int score,
        String explanation,
        String suggestion
    ) {}
}
