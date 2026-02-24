package com.interviewme.aichat.dto;

public record ChatAnalyticsResponse(
    long totalSessions,
    long totalMessages,
    long sessionsThisMonth,
    long messagesThisMonth
) {}
