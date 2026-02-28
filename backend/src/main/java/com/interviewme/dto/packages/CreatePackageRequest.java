package com.interviewme.dto.packages;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreatePackageRequest(
    @NotBlank(message = "Name is required")
    @Size(min = 1, max = 100, message = "Name must be between 1 and 100 characters")
    String name,

    @Size(max = 500, message = "Description must not exceed 500 characters")
    String description,

    @Size(min = 3, max = 50, message = "Slug must be between 3 and 50 characters")
    String slug
) {
}
