package com.interviewme.linkedin.dto;

public record LinkedInProjectData(
    String title,
    String description,
    String url,
    String startDate,
    String endDate
) {}
