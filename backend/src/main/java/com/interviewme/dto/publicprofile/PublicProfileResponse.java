package com.interviewme.dto.publicprofile;

import java.util.List;
import java.util.Map;

public record PublicProfileResponse(
    String slug,
    String fullName,
    String headline,
    String summary,
    String location,
    List<String> languages,
    Map<String, String> professionalLinks,
    List<PublicSkillResponse> skills,
    List<PublicJobResponse> jobs,
    List<PublicEducationResponse> education,
    SeoMetadata seo
) {
}
