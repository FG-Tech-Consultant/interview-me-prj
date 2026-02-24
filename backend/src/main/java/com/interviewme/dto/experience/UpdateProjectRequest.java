package com.interviewme.dto.experience;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Map;

public record UpdateProjectRequest(
    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title must not exceed 255 characters")
    String title,

    @Size(max = 5000, message = "Context must not exceed 5000 characters")
    String context,

    @Size(max = 255, message = "Role must not exceed 255 characters")
    String role,

    @Min(value = 1, message = "Team size must be at least 1")
    @Max(value = 1000, message = "Team size must not exceed 1000")
    Integer teamSize,

    @Size(max = 30, message = "Tech stack must not exceed 30 items")
    List<String> techStack,

    @Size(max = 100, message = "Architecture type must not exceed 100 characters")
    String architectureType,

    Map<String, Object> metrics,

    @Size(max = 5000, message = "Outcomes must not exceed 5000 characters")
    String outcomes,

    String visibility,

    @NotNull(message = "Version is required for optimistic locking")
    Long version
) {
}
