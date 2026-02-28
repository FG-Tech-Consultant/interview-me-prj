package com.interviewme.exports.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ExportCoverLetterRequest(
    @NotNull Long templateId,
    @NotBlank @Size(max = 200) String targetCompany,
    @NotBlank @Size(max = 200) String targetRole,
    @Size(max = 5000) String jobDescription,
    @NotBlank String market
) {}
