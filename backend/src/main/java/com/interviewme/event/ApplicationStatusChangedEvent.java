package com.interviewme.event;

import java.time.Instant;

public record ApplicationStatusChangedEvent(
    Long applicationId,
    Long tenantId,
    Long userId,
    String candidateEmail,
    String jobTitle,
    String oldStatus,
    String newStatus,
    Instant timestamp
) {}
