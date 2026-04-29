package com.interviewme.dto.company;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CompanyUpdateRequest(
    @NotBlank(message = "Company name is required")
    @Size(max = 255)
    String name,

    String cnpj,
    String website,
    String sector,
    String size,
    String description,
    String logoUrl,
    String country,
    String city
) {}
