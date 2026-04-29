package com.interviewme.dto.job;

import java.util.List;
import java.util.UUID;

public record LinkedInUploadResponse(
    boolean success,
    UUID sessionToken,
    String candidateName,
    String headline,
    List<String> extractedSkills,
    String summary,
    String message
) {}
