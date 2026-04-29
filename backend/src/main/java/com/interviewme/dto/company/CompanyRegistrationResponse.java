package com.interviewme.dto.company;

public record CompanyRegistrationResponse(
    Long companyId,
    String token,
    String message
) {}
