package com.interviewme.aichat.dto;

import java.util.UUID;

public record ChatResponse(
    String message,
    UUID sessionToken,
    Long messageId,
    QuotaInfo quotaInfo
) {}
