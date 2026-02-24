package com.interviewme.dto.education;

import java.time.Instant;
import java.time.LocalDate;

public record EducationResponse(
    Long id,
    Long tenantId,
    Long profileId,
    String degree,
    String institution,
    LocalDate startDate,
    LocalDate endDate,
    String fieldOfStudy,
    String gpa,
    String notes,
    String visibility,
    Instant createdAt,
    Instant updatedAt,
    Long version
) {
}
