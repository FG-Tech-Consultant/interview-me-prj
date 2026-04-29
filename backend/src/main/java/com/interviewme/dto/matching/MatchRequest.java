package com.interviewme.dto.matching;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record MatchRequest(
        @NotEmpty(message = "At least one skill ID is required")
        List<Long> skillIds,
        String description,
        int topK,
        double threshold
) {
    public MatchRequest {
        if (topK <= 0) topK = 20;
        if (threshold <= 0) threshold = 0.3;
    }
}
