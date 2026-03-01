package com.interviewme.dto.visitor;

public record VisitorStatsResponse(
    long profileViews,
    long totalVisitors,
    long chatVisitors
) {}
