package com.interviewme.aichat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record ChatRequest(
    @NotBlank @Size(max = 500) String message,
    UUID sessionToken,
    String visitorToken
) {}
