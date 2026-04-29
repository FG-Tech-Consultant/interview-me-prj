package com.interviewme.dto.job;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record JobFitChatRequest(
    @NotBlank String message,
    UUID sessionToken
) {}
