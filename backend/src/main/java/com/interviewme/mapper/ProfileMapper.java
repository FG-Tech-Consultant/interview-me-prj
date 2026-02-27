package com.interviewme.mapper;

import com.interviewme.dto.profile.CreateProfileRequest;
import com.interviewme.dto.profile.ProfileResponse;
import com.interviewme.dto.profile.UpdateProfileRequest;
import com.interviewme.model.Profile;

import java.util.Collections;
import java.util.stream.Collectors;

public class ProfileMapper {

    public static Profile toEntity(CreateProfileRequest request) {
        Profile profile = new Profile();
        profile.setFullName(request.fullName());
        profile.setHeadline(request.headline());
        profile.setSummary(request.summary());
        profile.setLocation(request.location());
        profile.setLanguages(request.languages());
        profile.setProfessionalLinks(request.professionalLinks());
        profile.setCareerPreferences(request.careerPreferences());
        profile.setDefaultVisibility(request.defaultVisibility() != null ? request.defaultVisibility() : "private");
        return profile;
    }

    public static ProfileResponse toResponse(Profile entity) {
        return new ProfileResponse(
            entity.getId(),
            entity.getTenantId(),
            entity.getUserId(),
            entity.getFullName(),
            entity.getHeadline(),
            entity.getSummary(),
            entity.getLocation(),
            entity.getLanguages(),
            entity.getProfessionalLinks(),
            entity.getCareerPreferences(),
            entity.getDefaultVisibility(),
            entity.getSlug(),
            entity.getSlugChangeCount(),
            entity.getViewCount(),
            entity.getCreatedAt(),
            entity.getUpdatedAt(),
            entity.getVersion(),
            entity.getJobExperiences() != null
                ? entity.getJobExperiences().stream()
                    .filter(job -> job.getDeletedAt() == null)
                    .map(JobExperienceMapper::toResponse)
                    .collect(Collectors.toList())
                : Collections.emptyList(),
            entity.getEducation() != null
                ? entity.getEducation().stream()
                    .filter(edu -> edu.getDeletedAt() == null)
                    .map(EducationMapper::toResponse)
                    .collect(Collectors.toList())
                : Collections.emptyList()
        );
    }

    public static void updateEntity(Profile entity, UpdateProfileRequest request) {
        entity.setFullName(request.fullName());
        entity.setHeadline(request.headline());
        entity.setSummary(request.summary());
        entity.setLocation(request.location());
        entity.setLanguages(request.languages());
        entity.setProfessionalLinks(request.professionalLinks());
        entity.setCareerPreferences(request.careerPreferences());
        if (request.defaultVisibility() != null) {
            entity.setDefaultVisibility(request.defaultVisibility());
        }
    }
}
