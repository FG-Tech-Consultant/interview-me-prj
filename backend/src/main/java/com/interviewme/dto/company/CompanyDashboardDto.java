package com.interviewme.dto.company;

public record CompanyDashboardDto(
    String companyName,
    long activeJobs,
    long totalCandidates,
    long profileViews
) {}
