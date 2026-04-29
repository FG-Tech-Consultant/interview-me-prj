package com.interviewme.dto.notification;

import java.time.OffsetDateTime;

public record NotificationDto(
    Long id,
    String type,
    String title,
    String message,
    String referenceType,
    Long referenceId,
    boolean read,
    OffsetDateTime createdAt
) {}
