package com.interviewme.aichat.client;

public record LlmChatMessage(
    String role,
    String content
) {}
