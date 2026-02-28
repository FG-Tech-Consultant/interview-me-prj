package com.interviewme.dto.packages;

import com.interviewme.dto.experience.ProjectResponse;
import com.interviewme.dto.experience.StoryResponse;
import com.interviewme.dto.skill.UserSkillDto;

import java.util.List;

public record PublicPackageResponse(
    String name,
    String description,
    String slug,
    String profileName,
    String profileHeadline,
    List<UserSkillDto> skills,
    List<ProjectResponse> projects,
    List<StoryResponse> stories
) {
}
