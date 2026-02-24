package com.interviewme.linkedin.dto;

import java.util.List;

public record SectionScoreResponse(
    Long id,
    String sectionName,
    Integer sectionScore,
    String qualityExplanation,
    List<String> suggestions,
    boolean canApplyToProfile
) {}
