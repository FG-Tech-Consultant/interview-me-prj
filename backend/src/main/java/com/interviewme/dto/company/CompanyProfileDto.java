package com.interviewme.dto.company;

import java.time.OffsetDateTime;

public record CompanyProfileDto(
    Long id,
    String name,
    String cnpj,
    String website,
    String sector,
    String size,
    String description,
    String logoUrl,
    String country,
    String city,
    Boolean active,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {}
