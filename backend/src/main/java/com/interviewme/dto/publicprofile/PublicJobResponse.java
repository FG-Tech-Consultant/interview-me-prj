package com.interviewme.dto.publicprofile;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record PublicJobResponse(
    String company,
    String role,
    LocalDate startDate,
    LocalDate endDate,
    boolean isCurrent,
    String location,
    String employmentType,
    String responsibilities,
    String achievements,
    Map<String, Object> metrics,
    List<PublicProjectResponse> projects
) {
}
