package com.interviewme.dto.matching;

import java.util.List;

public record MatchResult(
        Long profileId,
        String fullName,
        String headline,
        String location,
        double score,
        double graphScore,
        double vectorScore,
        List<MatchedSkill> matchedSkills
) {}
