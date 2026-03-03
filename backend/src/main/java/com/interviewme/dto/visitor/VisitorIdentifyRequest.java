package com.interviewme.dto.visitor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VisitorIdentifyRequest(
    @NotBlank @Size(max = 200) String name,
    @NotBlank @Size(max = 200) String company,
    @NotBlank @Size(max = 200) String jobRole,
    @Size(max = 500) String linkedinUrl,
    @Size(max = 300) String contactEmail,
    @Size(max = 50) String contactWhatsapp,
    @Size(max = 10) String locale
) {}
