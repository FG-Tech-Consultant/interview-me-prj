package com.interviewme.dto.experience;

import java.time.Instant;
import java.util.Map;

public record StoryResponse(
    Long id,
    Long experienceProjectId,
    String title,
    String situation,
    String task,
    String action,
    String result,
    Map<String, Object> metrics,
    String visibility,
    Instant createdAt,
    Instant updatedAt,
    Long version
) {
}
