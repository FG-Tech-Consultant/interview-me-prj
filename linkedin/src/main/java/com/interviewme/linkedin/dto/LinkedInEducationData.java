package com.interviewme.linkedin.dto;

public record LinkedInEducationData(
    String schoolName,
    String degreeName,
    String fieldOfStudy,
    String startDate,
    String endDate,
    String notes,
    String activities
) {}
