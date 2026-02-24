package com.interviewme.dto.publicprofile;

import java.time.LocalDate;

public record PublicSkillResponse(
    String skillName,
    String category,
    int proficiencyDepth,
    int yearsOfExperience,
    LocalDate lastUsedDate
) {
}
