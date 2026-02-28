package com.interviewme.dto.packages;

import com.interviewme.dto.experience.ProjectResponse;
import com.interviewme.dto.experience.StoryResponse;
import com.interviewme.dto.skill.UserSkillDto;

import java.time.Instant;
import java.util.List;

public record PackageDetailResponse(
    Long id,
    Long profileId,
    String name,
    String description,
    String slug,
    String accessToken,
    Instant tokenExpiresAt,
    Boolean isActive,
    Integer viewCount,
    List<PackageItemResponse<UserSkillDto>> skills,
    List<PackageItemResponse<ProjectResponse>> projects,
    List<PackageItemResponse<StoryResponse>> stories,
    Instant createdAt,
    Instant updatedAt,
    Integer version
) {
}
