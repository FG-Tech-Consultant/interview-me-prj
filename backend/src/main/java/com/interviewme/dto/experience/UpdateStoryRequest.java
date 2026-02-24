package com.interviewme.dto.experience;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Map;

public record UpdateStoryRequest(
    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title must not exceed 255 characters")
    String title,

    @NotBlank(message = "Situation is required")
    @Size(max = 5000, message = "Situation must not exceed 5000 characters")
    String situation,

    @NotBlank(message = "Task is required")
    @Size(max = 5000, message = "Task must not exceed 5000 characters")
    String task,

    @NotBlank(message = "Action is required")
    @Size(max = 5000, message = "Action must not exceed 5000 characters")
    String action,

    @NotBlank(message = "Result is required")
    @Size(max = 5000, message = "Result must not exceed 5000 characters")
    String result,

    Map<String, Object> metrics,

    String visibility,

    @NotNull(message = "Version is required for optimistic locking")
    Long version
) {
}
