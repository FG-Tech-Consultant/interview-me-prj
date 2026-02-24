package com.interviewme.mapper;

import com.interviewme.dto.experience.CreateProjectRequest;
import com.interviewme.dto.experience.ProjectResponse;
import com.interviewme.dto.experience.UpdateProjectRequest;
import com.interviewme.model.ExperienceProject;

public class ExperienceProjectMapper {

    public static ExperienceProject toEntity(CreateProjectRequest request) {
        ExperienceProject project = new ExperienceProject();
        project.setTitle(request.title());
        project.setContext(request.context());
        project.setRole(request.role());
        project.setTeamSize(request.teamSize());
        project.setTechStack(request.techStack());
        project.setArchitectureType(request.architectureType());
        project.setMetrics(request.metrics());
        project.setOutcomes(request.outcomes());
        project.setVisibility(request.visibility() != null ? request.visibility() : "private");
        return project;
    }

    public static ProjectResponse toResponse(ExperienceProject entity, int storyCount) {
        return new ProjectResponse(
            entity.getId(),
            entity.getJobExperienceId(),
            entity.getTitle(),
            entity.getContext(),
            entity.getRole(),
            entity.getTeamSize(),
            entity.getTechStack(),
            entity.getArchitectureType(),
            entity.getMetrics(),
            entity.getOutcomes(),
            entity.getVisibility(),
            storyCount,
            entity.getCreatedAt(),
            entity.getUpdatedAt(),
            entity.getVersion()
        );
    }

    public static void updateEntity(ExperienceProject entity, UpdateProjectRequest request) {
        entity.setTitle(request.title());
        entity.setContext(request.context());
        entity.setRole(request.role());
        entity.setTeamSize(request.teamSize());
        entity.setTechStack(request.techStack());
        entity.setArchitectureType(request.architectureType());
        entity.setMetrics(request.metrics());
        entity.setOutcomes(request.outcomes());
        if (request.visibility() != null) {
            entity.setVisibility(request.visibility());
        }
    }
}
