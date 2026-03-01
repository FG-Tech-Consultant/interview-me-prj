package com.interviewme.linkedin.dto;

import java.util.List;

public record LinkedInImportData(
    LinkedInProfileData profile,
    List<LinkedInPositionData> positions,
    List<LinkedInEducationData> education,
    List<String> skills,
    List<String> languages,
    List<LinkedInProjectData> projects,
    List<LinkedInCertificationData> certifications,
    List<String> warnings
) {}
