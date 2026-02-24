package com.interviewme.billing.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdminGrantRequest(
    @NotNull @Min(1) Integer amount,
    @Size(max = 500) String description
) {}
