package com.interviewme.dto.skill;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record UpdateUserSkillDto(
    @Min(value = 0, message = "Years of experience must be at least 0")
    @Max(value = 70, message = "Years of experience must not exceed 70")
    Integer yearsOfExperience,

    @Min(value = 1, message = "Proficiency must be at least 1")
    @Max(value = 5, message = "Proficiency must not exceed 5")
    Integer proficiencyDepth,

    LocalDate lastUsedDate,

    String confidenceLevel,

    @Size(max = 20, message = "Maximum 20 tags allowed")
    List<String> tags,

    String visibility
) {
}
