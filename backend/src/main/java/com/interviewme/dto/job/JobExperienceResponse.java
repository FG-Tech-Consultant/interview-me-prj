package com.interviewme.dto.job;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

public record JobExperienceResponse(
    Long id,
    Long tenantId,
    Long profileId,
    String company,
    String role,
    LocalDate startDate,
    LocalDate endDate,
    Boolean isCurrent,
    String location,
    String employmentType,
    String responsibilities,
    String achievements,
    Map<String, Object> metrics,
    String workLanguage,
    String visibility,
    Instant createdAt,
    Instant updatedAt,
    Long version
) {
}
