package com.interviewme.event;

import java.time.Instant;

public record UserRegisteredEvent(
        Long userId,
        Long tenantId,
        String email,
        String tenantName,
        Instant timestamp
) {}
