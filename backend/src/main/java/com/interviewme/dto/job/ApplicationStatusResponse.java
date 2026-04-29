package com.interviewme.dto.job;

import java.time.OffsetDateTime;
import java.util.List;

public record ApplicationStatusResponse(
    Long id,
    String jobTitle,
    String candidateName,
    String candidateEmail,
    String status,
    Integer fitScore,
    List<String> matchedSkills,
    List<String> gapSkills,
    OffsetDateTime appliedAt
) {}
