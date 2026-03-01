package com.interviewme.linkedin.dto;

public record LinkedInPositionData(
    String companyName,
    String title,
    String description,
    String location,
    String startDate,
    String endDate
) {}
