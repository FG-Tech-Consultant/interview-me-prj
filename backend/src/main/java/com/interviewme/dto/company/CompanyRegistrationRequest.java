package com.interviewme.dto.company;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CompanyRegistrationRequest(
    @NotBlank(message = "Company name is required")
    @Size(max = 255)
    String companyName,

    String sector,
    String size,
    String website,
    String country,
    String city,
    String description,

    @NotBlank(message = "Admin name is required")
    @Size(max = 255)
    String adminName,

    @NotBlank(message = "Email is required")
    @Email(message = "Valid email is required")
    String email,

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    String password
) {}
