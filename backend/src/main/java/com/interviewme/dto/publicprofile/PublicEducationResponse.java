package com.interviewme.dto.publicprofile;

import java.time.LocalDate;

public record PublicEducationResponse(
    String degree,
    String institution,
    LocalDate startDate,
    LocalDate endDate,
    String fieldOfStudy
) {
}
