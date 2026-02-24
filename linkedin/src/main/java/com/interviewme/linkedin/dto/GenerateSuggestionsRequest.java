package com.interviewme.linkedin.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record GenerateSuggestionsRequest(
    @Min(1) @Max(5) Integer count
) {
    public GenerateSuggestionsRequest {
        if (count == null) {
            count = 3;
        }
    }
}
