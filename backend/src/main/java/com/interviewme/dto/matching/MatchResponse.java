package com.interviewme.dto.matching;

import java.util.List;

public record MatchResponse(
        List<MatchResult> candidates,
        int totalFound,
        List<String> expandedSkills
) {}
