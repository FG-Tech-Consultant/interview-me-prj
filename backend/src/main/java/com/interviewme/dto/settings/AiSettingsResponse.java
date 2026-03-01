package com.interviewme.dto.settings;

import java.util.List;

public record AiSettingsResponse(
    String provider,
    String chatModel,
    List<AvailableProvider> availableProviders
) {
    public record AvailableProvider(String name, String defaultModel) {}
}
