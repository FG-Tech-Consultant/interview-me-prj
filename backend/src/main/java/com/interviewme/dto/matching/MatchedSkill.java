package com.interviewme.dto.matching;

public record MatchedSkill(
        Long skillId,
        String skillName,
        String matchType,
        int yearsOfExperience,
        int proficiencyDepth
) {}
