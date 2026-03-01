package com.interviewme.linkedin.dto;

public record LinkedInProfileData(
    String firstName,
    String lastName,
    String headline,
    String summary,
    String location,
    String emailAddress
) {}
