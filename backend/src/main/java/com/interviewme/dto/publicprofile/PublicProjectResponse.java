package com.interviewme.dto.publicprofile;

import java.util.List;
import java.util.Map;

public record PublicProjectResponse(
    String title,
    String context,
    String role,
    Integer teamSize,
    List<String> techStack,
    String architectureType,
    Map<String, Object> metrics,
    String outcomes,
    List<String> linkedSkills,
    List<PublicStoryResponse> stories
) {
}
