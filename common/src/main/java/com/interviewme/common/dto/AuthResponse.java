package com.interviewme.common.dto;

public record AuthResponse(
    String token,
    String email,
    Long tenantId
) {}
