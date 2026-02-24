package com.interviewme.aichat.dto;

public record QuotaInfo(
    int freeRemaining,
    int freeLimit,
    boolean usingCoins
) {}
