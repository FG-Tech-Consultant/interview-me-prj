package com.interviewme.dto.job;

import java.util.List;
import java.util.UUID;

public record ApplicationChatResponse(
    String message,
    UUID sessionToken,
    String phase,
    boolean applicationComplete,
    ApplicationSummary summary
) {
    public record ApplicationSummary(
        String candidateName,
        String candidateEmail,
        Integer fitScore,
        List<String> matchedSkills,
        List<String> gapSkills,
        String recommendation
    ) {}
}
