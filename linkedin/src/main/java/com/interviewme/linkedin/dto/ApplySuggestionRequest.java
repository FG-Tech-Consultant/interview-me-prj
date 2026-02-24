package com.interviewme.linkedin.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ApplySuggestionRequest(
    @NotNull @Min(0) Integer suggestionIndex
) {}
