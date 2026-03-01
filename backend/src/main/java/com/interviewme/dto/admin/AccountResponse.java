package com.interviewme.dto.admin;

import java.time.OffsetDateTime;

public record AccountResponse(
    Long id,
    String email,
    String fullName,
    String role,
    String slug,
    String publicProfileUrl,
    OffsetDateTime createdAt
) {}
