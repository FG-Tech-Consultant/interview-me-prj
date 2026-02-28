package com.interviewme.service;

import com.interviewme.common.exception.OptimisticLockException;
import com.interviewme.common.exception.ProfileNotFoundException;
import com.interviewme.common.exception.ValidationException;
import com.interviewme.dto.job.CreateJobExperienceRequest;
import com.interviewme.dto.job.JobExperienceResponse;
import com.interviewme.dto.job.UpdateJobExperienceRequest;
import com.interviewme.event.ContentChangedEventListener;
import com.interviewme.mapper.JobExperienceMapper;
import com.interviewme.model.JobExperience;
import com.interviewme.model.Profile;
import com.interviewme.repository.JobExperienceRepository;
import com.interviewme.repository.ProfileRepository;
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
public class JobExperienceService {

    private final JobExperienceRepository jobExperienceRepository;
    private final ProfileRepository profileRepository;
    private final ContentChangedEventListener contentChangedEventListener;

    @Transactional(readOnly = true)
    public List<JobExperienceResponse> getJobExperiencesByProfileId(Long profileId) {
        Long tenantId = TenantContext.getCurrentTenantId();
        log.debug("Fetching job experiences for profileId: {}, tenantId: {}", profileId, tenantId);

        // Verify profile exists and belongs to tenant
        profileRepository.findByIdAndTenantId(profileId, tenantId)
                .orElseThrow(() -> new ProfileNotFoundException(profileId));

        List<JobExperience> experiences = jobExperienceRepository.findByProfileIdAndDeletedAtIsNullOrderByStartDateDesc(profileId);
        return experiences.stream()
                .map(JobExperienceMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public JobExperienceResponse getJobExperienceById(Long profileId, Long experienceId) {
        Long tenantId = TenantContext.getCurrentTenantId();
        log.debug("Fetching job experience id: {} for profileId: {}, tenantId: {}", experienceId, profileId, tenantId);

        JobExperience experience = jobExperienceRepository.findByIdAndProfileIdAndDeletedAtIsNull(experienceId, profileId)
                .orElseThrow(() -> new ValidationException("jobExperienceId", "Job experience not found"));

        // Verify profile belongs to tenant
        profileRepository.findByIdAndTenantId(profileId, tenantId)
                .orElseThrow(() -> new ProfileNotFoundException(profileId));

        return JobExperienceMapper.toResponse(experience);
    }

    @Transactional
    public JobExperienceResponse createJobExperience(Long profileId, CreateJobExperienceRequest request) {
        Long tenantId = TenantContext.getCurrentTenantId();
        log.info("Creating job experience for profileId: {}, tenantId: {}", profileId, tenantId);

        Profile profile = profileRepository.findByIdAndTenantId(profileId, tenantId)
                .orElseThrow(() -> new ProfileNotFoundException(profileId));

        // Validate dates
        if (request.endDate() != null && request.endDate().isBefore(request.startDate())) {
            throw new ValidationException("endDate", "End date cannot be before start date");
        }

        JobExperience experience = JobExperienceMapper.toEntity(request);
        experience.setProfileId(profile.getId());
        experience.setTenantId(tenantId);

        JobExperience savedExperience = jobExperienceRepository.save(experience);
        log.info("Job experience created with id: {}", savedExperience.getId());
        triggerEmbeddingUpdate(savedExperience);

        return JobExperienceMapper.toResponse(savedExperience);
    }

    @Transactional
    public JobExperienceResponse updateJobExperience(Long profileId, Long experienceId, UpdateJobExperienceRequest request) {
        Long tenantId = TenantContext.getCurrentTenantId();
        log.info("Updating job experience id: {} for profileId: {}, tenantId: {}", experienceId, profileId, tenantId);

        // Verify profile belongs to tenant
        profileRepository.findByIdAndTenantId(profileId, tenantId)
                .orElseThrow(() -> new ProfileNotFoundException(profileId));

        JobExperience experience = jobExperienceRepository.findByIdAndProfileIdAndDeletedAtIsNull(experienceId, profileId)
                .orElseThrow(() -> new ValidationException("jobExperienceId", "Job experience not found"));

        try {
            JobExperienceMapper.updateEntity(experience, request);

            // Validate dates after update
            if (experience.getEndDate() != null && experience.getEndDate().isBefore(experience.getStartDate())) {
                throw new ValidationException("endDate", "End date cannot be before start date");
            }

            JobExperience updatedExperience = jobExperienceRepository.save(experience);
            log.info("Job experience updated successfully: {}", experienceId);
            triggerEmbeddingUpdate(updatedExperience);

            return JobExperienceMapper.toResponse(updatedExperience);
        } catch (OptimisticLockingFailureException ex) {
            log.error("Optimistic lock failure for job experience: {}", experienceId, ex);
            throw new OptimisticLockException("Job experience was modified by another user. Please refresh and try again.");
        }
    }

    @Transactional
    public void deleteJobExperience(Long profileId, Long experienceId) {
        Long tenantId = TenantContext.getCurrentTenantId();
        log.info("Soft deleting job experience id: {} for profileId: {}, tenantId: {}", experienceId, profileId, tenantId);

        // Verify profile belongs to tenant
        profileRepository.findByIdAndTenantId(profileId, tenantId)
                .orElseThrow(() -> new ProfileNotFoundException(profileId));

        JobExperience experience = jobExperienceRepository.findByIdAndProfileIdAndDeletedAtIsNull(experienceId, profileId)
                .orElseThrow(() -> new ValidationException("jobExperienceId", "Job experience not found"));

        experience.setDeletedAt(Instant.now());
        jobExperienceRepository.save(experience);
        triggerEmbeddingUpdate(experience);
        log.info("Job experience soft deleted successfully: {}", experienceId);
    }

    private void triggerEmbeddingUpdate(JobExperience job) {
        try {
            contentChangedEventListener.onJobChanged(job);
        } catch (Exception e) {
            log.warn("Failed to update embedding for job {}: {}", job.getId(), e.getMessage());
        }
    }
}
