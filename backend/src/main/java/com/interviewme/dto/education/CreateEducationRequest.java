package com.interviewme.dto.education;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateEducationRequest(
    @NotBlank(message = "Degree is required")
    @Size(max = 255, message = "Degree must not exceed 255 characters")
    String degree,

    @NotBlank(message = "Institution is required")
    @Size(max = 255, message = "Institution must not exceed 255 characters")
    String institution,

    LocalDate startDate,

    @NotNull(message = "End date is required")
    LocalDate endDate,

    @Size(max = 255, message = "Field of study must not exceed 255 characters")
    String fieldOfStudy,

    @Size(max = 50, message = "GPA must not exceed 50 characters")
    String gpa,

    @Size(max = 2000, message = "Notes must not exceed 2000 characters")
    String notes,

    String visibility
) {
}
