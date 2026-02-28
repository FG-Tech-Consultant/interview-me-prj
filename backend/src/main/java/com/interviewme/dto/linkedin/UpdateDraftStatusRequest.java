package com.interviewme.dto.linkedin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateDraftStatusRequest(
    @NotBlank @Pattern(regexp = "SENT|ARCHIVED", message = "Status must be SENT or ARCHIVED") String status
) {}
