package com.interviewme.dto.job;

import java.util.List;

public record AiJobDescriptionResponse(
    String description,
    String requirements,
    String benefits,
    List<String> suggestedSkills,
    List<String> suggestedNiceToHave
) {}
