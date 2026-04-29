package com.interviewme.dto.job;

import jakarta.validation.constraints.Size;

import java.util.List;

public record UpdateJobPostingRequest(
    @Size(max = 255) String title,
    String description,
    String requirements,
    String benefits,
    @Size(max = 255) String location,
    @Size(max = 50) String workModel,
    @Size(max = 100) String salaryRange,
    @Size(max = 50) String experienceLevel,
    String status,
    List<String> requiredSkills,
    List<String> niceToHaveSkills
) {}
