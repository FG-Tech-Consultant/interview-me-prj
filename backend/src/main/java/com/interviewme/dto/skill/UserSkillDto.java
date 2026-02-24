package com.interviewme.dto.skill;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record UserSkillDto(
    Long id,
    SkillDto skill,
    Integer yearsOfExperience,
    Integer proficiencyDepth,
    LocalDate lastUsedDate,
    String confidenceLevel,
    List<String> tags,
    String visibility,
    Instant createdAt,
    Instant updatedAt
) {
}
