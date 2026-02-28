package com.interviewme.dto.packages;

import java.time.Instant;

public record PackageResponse(
    Long id,
    Long profileId,
    String name,
    String description,
    String slug,
    String accessToken,
    Instant tokenExpiresAt,
    Boolean isActive,
    Integer viewCount,
    Instant createdAt,
    Instant updatedAt,
    Integer version
) {
}
