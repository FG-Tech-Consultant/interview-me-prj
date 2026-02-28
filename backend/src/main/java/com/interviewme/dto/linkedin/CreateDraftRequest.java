package com.interviewme.dto.linkedin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateDraftRequest(
    @NotBlank @Size(max = 5000) String originalMessage,
    @Size(max = 50) String tone
) {}
