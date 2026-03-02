package com.interviewme.dto.profile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Map;

public record CreateProfileRequest(
    @NotBlank(message = "Full name is required")
    @Size(max = 255, message = "Full name must not exceed 255 characters")
    String fullName,

    @Size(max = 255, message = "Headline must not exceed 255 characters")
    String headline,

    @Size(max = 5000, message = "Summary must not exceed 5000 characters")
    String summary,

    @Size(max = 255, message = "Location must not exceed 255 characters")
    String location,

    List<String> languages,

    Map<String, String> professionalLinks,

    Map<String, Object> careerPreferences,

    String defaultVisibility
) {
}
