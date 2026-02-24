package com.interviewme.dto.experience;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record ProjectResponse(
    Long id,
    Long jobExperienceId,
    String title,
    String context,
    String role,
    Integer teamSize,
    List<String> techStack,
    String architectureType,
    Map<String, Object> metrics,
    String outcomes,
    String visibility,
    int storyCount,
    Instant createdAt,
    Instant updatedAt,
    Long version
) {
}
