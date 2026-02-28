package com.interviewme.mapper;

import com.interviewme.dto.experience.ProjectResponse;
import com.interviewme.dto.experience.StoryResponse;
import com.interviewme.dto.packages.*;
import com.interviewme.dto.skill.UserSkillDto;
import com.interviewme.model.*;

import java.util.List;

public class PackageMapper {

    public static ContentPackage toEntity(CreatePackageRequest request) {
        ContentPackage pkg = new ContentPackage();
        pkg.setName(request.name());
        pkg.setDescription(request.description());
        return pkg;
    }

    public static PackageResponse toResponse(ContentPackage entity) {
        return new PackageResponse(
            entity.getId(),
            entity.getProfileId(),
            entity.getName(),
            entity.getDescription(),
            entity.getSlug(),
            entity.getAccessToken(),
            entity.getTokenExpiresAt(),
            entity.getIsActive(),
            entity.getViewCount(),
            entity.getCreatedAt(),
            entity.getUpdatedAt(),
            entity.getVersion()
        );
    }

    public static PackageDetailResponse toDetailResponse(
            ContentPackage entity,
            List<PackageItemResponse<UserSkillDto>> skills,
            List<PackageItemResponse<ProjectResponse>> projects,
            List<PackageItemResponse<StoryResponse>> stories) {
        return new PackageDetailResponse(
            entity.getId(),
            entity.getProfileId(),
            entity.getName(),
            entity.getDescription(),
            entity.getSlug(),
            entity.getAccessToken(),
            entity.getTokenExpiresAt(),
            entity.getIsActive(),
            entity.getViewCount(),
            skills,
            projects,
            stories,
            entity.getCreatedAt(),
            entity.getUpdatedAt(),
            entity.getVersion()
        );
    }

    public static PublicPackageResponse toPublicResponse(
            ContentPackage entity,
            String profileName,
            String profileHeadline,
            List<UserSkillDto> skills,
            List<ProjectResponse> projects,
            List<StoryResponse> stories) {
        return new PublicPackageResponse(
            entity.getName(),
            entity.getDescription(),
            entity.getSlug(),
            profileName,
            profileHeadline,
            skills,
            projects,
            stories
        );
    }

    public static void updateEntity(ContentPackage entity, UpdatePackageRequest request) {
        entity.setName(request.name());
        if (request.description() != null) {
            entity.setDescription(request.description());
        }
        if (request.isActive() != null) {
            entity.setIsActive(request.isActive());
        }
    }
}
