package com.interviewme.mapper;

import com.interviewme.dto.job.CreateJobExperienceRequest;
import com.interviewme.dto.job.JobExperienceResponse;
import com.interviewme.dto.job.UpdateJobExperienceRequest;
import com.interviewme.model.JobExperience;

public class JobExperienceMapper {

    public static JobExperience toEntity(CreateJobExperienceRequest request) {
        JobExperience jobExperience = new JobExperience();
        jobExperience.setCompany(request.company());
        jobExperience.setRole(request.role());
        jobExperience.setStartDate(request.startDate());
        jobExperience.setEndDate(request.endDate());
        jobExperience.setIsCurrent(request.isCurrent() != null ? request.isCurrent() : false);
        jobExperience.setLocation(request.location());
        jobExperience.setEmploymentType(request.employmentType());
        jobExperience.setResponsibilities(request.responsibilities());
        jobExperience.setAchievements(request.achievements());
        jobExperience.setMetrics(request.metrics());
        jobExperience.setWorkLanguage(request.workLanguage());
        jobExperience.setVisibility(request.visibility() != null ? request.visibility() : "private");
        return jobExperience;
    }

    public static JobExperienceResponse toResponse(JobExperience entity) {
        return new JobExperienceResponse(
            entity.getId(),
            entity.getTenantId(),
            entity.getProfileId(),
            entity.getCompany(),
            entity.getRole(),
            entity.getStartDate(),
            entity.getEndDate(),
            entity.getIsCurrent(),
            entity.getLocation(),
            entity.getEmploymentType(),
            entity.getResponsibilities(),
            entity.getAchievements(),
            entity.getMetrics(),
            entity.getWorkLanguage(),
            entity.getVisibility(),
            entity.getCreatedAt(),
            entity.getUpdatedAt(),
            entity.getVersion()
        );
    }

    public static void updateEntity(JobExperience entity, UpdateJobExperienceRequest request) {
        entity.setCompany(request.company());
        entity.setRole(request.role());
        entity.setStartDate(request.startDate());
        entity.setEndDate(request.endDate());
        entity.setIsCurrent(request.isCurrent() != null ? request.isCurrent() : false);
        entity.setLocation(request.location());
        entity.setEmploymentType(request.employmentType());
        entity.setResponsibilities(request.responsibilities());
        entity.setAchievements(request.achievements());
        entity.setMetrics(request.metrics());
        entity.setWorkLanguage(request.workLanguage());
        if (request.visibility() != null) {
            entity.setVisibility(request.visibility());
        }
    }
}
