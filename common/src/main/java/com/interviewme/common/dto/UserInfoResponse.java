package com.interviewme.common.dto;

public record UserInfoResponse(
    Long id,
    String email,
    Long tenantId,
    String role,
    String createdAt
) {}
