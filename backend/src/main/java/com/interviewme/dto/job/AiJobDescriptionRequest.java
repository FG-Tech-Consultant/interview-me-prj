package com.interviewme.dto.job;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record AiJobDescriptionRequest(
    @NotBlank String title,
    String experienceLevel,
    String workModel,
    List<String> requiredSkills,
    String additionalContext
) {}
