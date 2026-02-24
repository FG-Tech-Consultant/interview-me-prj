package com.interviewme.dto.job;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.Map;

public record UpdateJobExperienceRequest(
    @NotBlank(message = "Company is required")
    @Size(max = 255, message = "Company must not exceed 255 characters")
    String company,

    @NotBlank(message = "Role is required")
    @Size(max = 255, message = "Role must not exceed 255 characters")
    String role,

    @NotNull(message = "Start date is required")
    LocalDate startDate,

    LocalDate endDate,

    Boolean isCurrent,

    @Size(max = 255, message = "Location must not exceed 255 characters")
    String location,

    @Size(max = 50, message = "Employment type must not exceed 50 characters")
    String employmentType,

    @Size(max = 10000, message = "Responsibilities must not exceed 10000 characters")
    String responsibilities,

    @Size(max = 10000, message = "Achievements must not exceed 10000 characters")
    String achievements,

    Map<String, Object> metrics,

    String visibility,

    @NotNull(message = "Version is required for optimistic locking")
    Long version
) {
}
