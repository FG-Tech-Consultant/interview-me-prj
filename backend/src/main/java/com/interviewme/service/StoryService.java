package com.interviewme.service;

import com.interviewme.common.exception.OptimisticLockException;
import com.interviewme.common.exception.ProjectNotFoundException;
import com.interviewme.common.exception.StoryNotFoundException;
import com.interviewme.dto.experience.CreateStoryRequest;
import com.interviewme.dto.experience.StoryResponse;
import com.interviewme.dto.experience.UpdateStoryRequest;
import com.interviewme.model.Story;
import com.interviewme.event.ContentChangedEventListener;
import com.interviewme.mapper.StoryMapper;
import com.interviewme.repository.ExperienceProjectRepository;
import com.interviewme.repository.StoryRepository;
import com.interviewme.repository.StorySkillRepository;
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
public class StoryService {

    private final StoryRepository storyRepository;
    private final StorySkillRepository storySkillRepository;
    private final ExperienceProjectRepository projectRepository;
    private final ContentChangedEventListener contentChangedEventListener;

    @Transactional
    public StoryResponse createStory(Long projectId, CreateStoryRequest request) {
        Long tenantId = TenantContext.getCurrentTenantId();
        log.info("Creating story for projectId: {}, tenantId: {}", projectId, tenantId);

        // Verify project exists and is not deleted
        projectRepository.findByIdAndDeletedAtIsNull(projectId)
            .orElseThrow(() -> new ProjectNotFoundException(projectId));

        Story story = StoryMapper.toEntity(request);
        story.setTenantId(tenantId);
        story.setExperienceProjectId(projectId);

        Story saved = storyRepository.save(story);
        log.info("Story created with id: {}", saved.getId());
        triggerEmbeddingUpdate(saved);

        return StoryMapper.toResponse(saved);
    }

    @Transactional
    public StoryResponse updateStory(Long storyId, UpdateStoryRequest request) {
        Long tenantId = TenantContext.getCurrentTenantId();
        log.info("Updating story id: {}, tenantId: {}", storyId, tenantId);

        Story story = storyRepository.findByIdAndDeletedAtIsNull(storyId)
            .orElseThrow(() -> new StoryNotFoundException(storyId));

        try {
            StoryMapper.updateEntity(story, request);
            Story updated = storyRepository.save(story);
            log.info("Story updated successfully: {}", storyId);
            triggerEmbeddingUpdate(updated);
            return StoryMapper.toResponse(updated);
        } catch (OptimisticLockingFailureException ex) {
            log.error("Optimistic lock failure for story: {}", storyId, ex);
            throw new OptimisticLockException("Story was modified by another user. Please refresh and try again.");
        }
    }

    @Transactional
    public void deleteStory(Long storyId) {
        Long tenantId = TenantContext.getCurrentTenantId();
        log.info("Soft deleting story id: {}, tenantId: {}", storyId, tenantId);

        Story story = storyRepository.findByIdAndDeletedAtIsNull(storyId)
            .orElseThrow(() -> new StoryNotFoundException(storyId));

        // Clean up skill associations
        storySkillRepository.deleteByStoryId(storyId);

        story.setDeletedAt(Instant.now());
        storyRepository.save(story);
        triggerEmbeddingUpdate(story);
        log.info("Story soft deleted successfully: {}", storyId);
    }

    @Transactional(readOnly = true)
    public List<StoryResponse> getStoriesByProject(Long projectId) {
        Long tenantId = TenantContext.getCurrentTenantId();
        log.debug("Fetching stories for projectId: {}, tenantId: {}", projectId, tenantId);

        // Verify project exists
        projectRepository.findByIdAndDeletedAtIsNull(projectId)
            .orElseThrow(() -> new ProjectNotFoundException(projectId));

        List<Story> stories = storyRepository
            .findByExperienceProjectIdAndDeletedAtIsNullOrderByCreatedAtDesc(projectId);

        return stories.stream()
            .map(StoryMapper::toResponse)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public StoryResponse getStoryById(Long storyId) {
        Long tenantId = TenantContext.getCurrentTenantId();
        log.debug("Fetching story id: {}, tenantId: {}", storyId, tenantId);

        Story story = storyRepository.findByIdAndDeletedAtIsNull(storyId)
            .orElseThrow(() -> new StoryNotFoundException(storyId));

        return StoryMapper.toResponse(story);
    }

    @Transactional(readOnly = true)
    public List<StoryResponse> getPublicStoriesByProfile(Long profileId) {
        log.debug("Fetching public stories for profileId: {}", profileId);

        List<Story> stories = storyRepository.findPublicStoriesByProfileId(profileId);
        return stories.stream()
            .map(StoryMapper::toResponse)
            .collect(Collectors.toList());
    }

    private void triggerEmbeddingUpdate(Story story) {
        try {
            contentChangedEventListener.onStoryChanged(story);
        } catch (Exception e) {
            log.warn("Failed to update embedding for story {}: {}", story.getId(), e.getMessage());
        }
    }
}
