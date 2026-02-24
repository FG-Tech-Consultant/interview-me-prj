package com.interviewme.exports.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ExportResumeRequest(
    @NotNull Long templateId,
    @NotBlank @Size(max = 200) String targetRole,
    @Size(max = 200) String location,
    @NotBlank String seniority,
    String language
) {}
