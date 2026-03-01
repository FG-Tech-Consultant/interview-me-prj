package com.interviewme.dto.admin;

public record GlobalStatsResponse(
    long totalAccounts,
    long totalProfileViews,
    long totalVisitors,
    long totalInterviews
) {}
