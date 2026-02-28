package com.interviewme.exports.dto;

import jakarta.validation.constraints.NotNull;

public record ExportBackgroundDeckRequest(
    @NotNull Long templateId
) {}
