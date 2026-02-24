package com.interviewme.exports.event;

import java.time.Instant;

public record ExportCompletedEvent(
    Long tenantId,
    Long exportId,
    String type,
    String fileUrl,
    Integer coinsSpent,
    Instant timestamp
) {}
