package com.interviewme.dto.settings;

public record AiSettingsRequest(
    String provider,
    String chatModel
) {}
