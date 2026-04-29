package com.interviewme.dto.job;

import java.time.OffsetDateTime;
import java.util.List;

public record JobPostingDto(
    Long id,
    String title,
    String slug,
    String description,
    String requirements,
    String benefits,
    String location,
    String workModel,
    String salaryRange,
    String experienceLevel,
    String status,
    List<String> requiredSkills,
    List<String> niceToHaveSkills,
    OffsetDateTime createdAt
) {}
