package com.interviewme.service;

import com.interviewme.common.exception.OptimisticLockException;
import com.interviewme.common.exception.ProjectNotFoundException;
import com.interviewme.common.exception.ValidationException;
import com.interviewme.dto.experience.CreateProjectRequest;
import com.interviewme.dto.experience.ProjectResponse;
import com.interviewme.dto.experience.UpdateProjectRequest;
import com.interviewme.model.ExperienceProject;
import com.interviewme.model.Story;
import com.interviewme.event.ContentChangedEventListener;
import com.interviewme.mapper.ExperienceProjectMapper;
import com.interviewme.repository.ExperienceProjectRepository;
import com.interviewme.repository.ExperienceProjectSkillRepository;
import com.interviewme.repository.StoryRepository;
import com.interviewme.repository.StorySkillRepository;
import com.interviewme.repository.JobExperienceRepository;
import com.interviewme.common.util.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExperienceProjectService {

    private final ExperienceProjectRepository projectRepository;
    private final ExperienceProjectSkillRepository projectSkillRepository;
    private final StoryRepository storyRepository;
    private final StorySkillRepository storySkillRepository;
    private final JobExperienceRepository jobExperienceRepository;
    private final ContentChangedEventListener contentChangedEventListener;

    @Transactional
    public ProjectResponse createProject(Long jobExperienceId, CreateProjectRequest request) {
        Long tenantId = TenantContext.getCurrentTenantId();
        log.info("Creating project for jobExperienceId: {}, tenantId: {}", jobExperienceId, tenantId);

        // Verify job experience exists and is not deleted
        jobExperienceRepository.findByIdAndDeletedAtIsNull(jobExperienceId)
            .orElseThrow(() -> new ValidationException("jobExperienceId", "Job experience not found"));

        ExperienceProject project = ExperienceProjectMapper.toEntity(request);
        project.setTenantId(tenantId);
        project.setJobExperienceId(jobExperienceId);

        ExperienceProject saved = projectRepository.save(project);
        log.info("Project created with id: {}", saved.getId());
        triggerEmbeddingUpdate(saved);

        return ExperienceProjectMapper.toResponse(saved, 0);
    }

    @Transactional
    public ProjectResponse updateProject(Long projectId, UpdateProjectRequest request) {
        Long tenantId = TenantContext.getCurrentTenantId();
        log.info("Updating project id: {}, tenantId: {}", projectId, tenantId);

        ExperienceProject project = projectRepository.findByIdAndDeletedAtIsNull(projectId)
            .orElseThrow(() -> new ProjectNotFoundException(projectId));

        try {
            ExperienceProjectMapper.updateEntity(project, request);
            ExperienceProject updated = projectRepository.save(project);
            int storyCount = storyRepository.countByExperienceProjectIdAndDeletedAtIsNull(projectId);
            log.info("Project updated successfully: {}", projectId);
            triggerEmbeddingUpdate(updated);
            return ExperienceProjectMapper.toResponse(updated, storyCount);
        } catch (OptimisticLockingFailureException ex) {
            log.error("Optimistic lock failure for project: {}", projectId, ex);
            throw new OptimisticLockException("Project was modified by another user. Please refresh and try again.");
        }
    }

    @Transactional
    public void deleteProject(Long projectId) {
        Long tenantId = TenantContext.getCurrentTenantId();
        log.info("Soft deleting project id: {}, tenantId: {}", projectId, tenantId);

        ExperienceProject project = projectRepository.findByIdAndDeletedAtIsNull(projectId)
            .orElseThrow(() -> new ProjectNotFoundException(projectId));

        Instant now = Instant.now();

        // Cascade soft delete to stories
        List<Story> stories = storyRepository.findByExperienceProjectIdAndDeletedAtIsNullOrderByCreatedAtDesc(projectId);
        for (Story story : stories) {
            // Clean up story skill associations
            storySkillRepository.deleteByStoryId(story.getId());
            story.setDeletedAt(now);
            storyRepository.save(story);
        }

        // Clean up project skill associations
        projectSkillRepository.deleteByExperienceProjectId(projectId);

        // Soft delete the project
        project.setDeletedAt(now);
        projectRepository.save(project);
        triggerEmbeddingUpdate(project);
        for (Story story : stories) {
            try {
                contentChangedEventListener.onStoryChanged(story);
            } catch (Exception e) {
                log.warn("Failed to update embedding for cascaded story {}: {}", story.getId(), e.getMessage());
            }
        }
        log.info("Project soft deleted with {} stories cascaded: {}", stories.size(), projectId);
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> getProjectsByJobExperience(Long jobExperienceId) {
        Long tenantId = TenantContext.getCurrentTenantId();
        log.debug("Fetching projects for jobExperienceId: {}, tenantId: {}", jobExperienceId, tenantId);

        // Verify job experience exists
        jobExperienceRepository.findByIdAndDeletedAtIsNull(jobExperienceId)
            .orElseThrow(() -> new ValidationException("jobExperienceId", "Job experience not found"));

        List<ExperienceProject> projects = projectRepository
            .findByJobExperienceIdAndDeletedAtIsNullOrderByCreatedAtDesc(jobExperienceId);

        return projects.stream()
            .map(p -> {
                int storyCount = storyRepository.countByExperienceProjectIdAndDeletedAtIsNull(p.getId());
                return ExperienceProjectMapper.toResponse(p, storyCount);
            })
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProjectResponse getProjectById(Long projectId) {
        Long tenantId = TenantContext.getCurrentTenantId();
        log.debug("Fetching project id: {}, tenantId: {}", projectId, tenantId);

        ExperienceProject project = projectRepository.findByIdAndDeletedAtIsNull(projectId)
            .orElseThrow(() -> new ProjectNotFoundException(projectId));

        int storyCount = storyRepository.countByExperienceProjectIdAndDeletedAtIsNull(projectId);
        return ExperienceProjectMapper.toResponse(project, storyCount);
    }

    private void triggerEmbeddingUpdate(ExperienceProject project) {
        try {
            contentChangedEventListener.onProjectChanged(project);
        } catch (Exception e) {
            log.warn("Failed to update embedding for project {}: {}", project.getId(), e.getMessage());
        }
    }
}
