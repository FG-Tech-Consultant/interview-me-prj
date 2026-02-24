package com.interviewme.dto.profile;

import com.interviewme.dto.job.JobExperienceResponse;
import com.interviewme.dto.education.EducationResponse;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record ProfileResponse(
    Long id,
    Long tenantId,
    Long userId,
    String fullName,
    String headline,
    String summary,
    String location,
    List<String> languages,
    Map<String, String> professionalLinks,
    Map<String, Object> careerPreferences,
    String defaultVisibility,
    String slug,
    Long viewCount,
    Instant createdAt,
    Instant updatedAt,
    Long version,
    List<JobExperienceResponse> jobs,
    List<EducationResponse> education
) {
}
