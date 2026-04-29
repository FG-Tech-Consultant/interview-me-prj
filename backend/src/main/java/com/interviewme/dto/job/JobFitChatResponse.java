package com.interviewme.dto.job;

import java.util.List;
import java.util.UUID;

public record JobFitChatResponse(
    String message,
    UUID sessionToken,
    FitScore fitScore
) {
    public record FitScore(
        int overallScore,
        List<String> matchedSkills,
        List<String> gapSkills,
        String recommendation
    ) {}
}
