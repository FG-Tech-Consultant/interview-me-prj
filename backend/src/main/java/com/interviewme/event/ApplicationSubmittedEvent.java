package com.interviewme.event;

import java.time.Instant;

public record ApplicationSubmittedEvent(
    Long applicationId,
    Long tenantId,
    Long jobPostingId,
    String candidateName,
    String candidateEmail,
    Integer fitScore,
    String jobTitle,
    Instant timestamp
) {}
