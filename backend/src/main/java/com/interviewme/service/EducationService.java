package com.interviewme.service;

import com.interviewme.common.exception.OptimisticLockException;
import com.interviewme.common.exception.ProfileNotFoundException;
import com.interviewme.common.exception.ValidationException;
import com.interviewme.dto.education.CreateEducationRequest;
import com.interviewme.dto.education.EducationResponse;
import com.interviewme.dto.education.UpdateEducationRequest;
import com.interviewme.event.ContentChangedEventListener;
import com.interviewme.mapper.EducationMapper;
import com.interviewme.model.Education;
import com.interviewme.model.Profile;
import com.interviewme.repository.EducationRepository;
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
public class EducationService {

    private final EducationRepository educationRepository;
    private final ProfileRepository profileRepository;
    private final ContentChangedEventListener contentChangedEventListener;

    @Transactional(readOnly = true)
    public List<EducationResponse> getEducationsByProfileId(Long profileId) {
        Long tenantId = TenantContext.getCurrentTenantId();
        log.debug("Fetching education records for profileId: {}, tenantId: {}", profileId, tenantId);

        // Verify profile exists and belongs to tenant
        profileRepository.findByIdAndTenantId(profileId, tenantId)
                .orElseThrow(() -> new ProfileNotFoundException(profileId));

        List<Education> educations = educationRepository.findByProfileIdAndDeletedAtIsNullOrderByStartDateDesc(profileId);
        return educations.stream()
                .map(EducationMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public EducationResponse getEducationById(Long profileId, Long educationId) {
        Long tenantId = TenantContext.getCurrentTenantId();
        log.debug("Fetching education id: {} for profileId: {}, tenantId: {}", educationId, profileId, tenantId);

        Education education = educationRepository.findByIdAndProfileIdAndDeletedAtIsNull(educationId, profileId)
                .orElseThrow(() -> new ValidationException("educationId", "Education record not found"));

        // Verify profile belongs to tenant
        profileRepository.findByIdAndTenantId(profileId, tenantId)
                .orElseThrow(() -> new ProfileNotFoundException(profileId));

        return EducationMapper.toResponse(education);
    }

    @Transactional
    public EducationResponse createEducation(Long profileId, CreateEducationRequest request) {
        Long tenantId = TenantContext.getCurrentTenantId();
        log.info("Creating education record for profileId: {}, tenantId: {}", profileId, tenantId);

        Profile profile = profileRepository.findByIdAndTenantId(profileId, tenantId)
                .orElseThrow(() -> new ProfileNotFoundException(profileId));

        // Validate dates if both provided
        if (request.endDate() != null && request.endDate().isBefore(request.startDate())) {
            throw new ValidationException("endDate", "End date cannot be before start date");
        }

        Education education = EducationMapper.toEntity(request);
        education.setProfileId(profile.getId());
        education.setTenantId(tenantId);

        Education savedEducation = educationRepository.save(education);
        log.info("Education record created with id: {}", savedEducation.getId());
        triggerEmbeddingUpdate(savedEducation);

        return EducationMapper.toResponse(savedEducation);
    }

    @Transactional
    public EducationResponse updateEducation(Long profileId, Long educationId, UpdateEducationRequest request) {
        Long tenantId = TenantContext.getCurrentTenantId();
        log.info("Updating education id: {} for profileId: {}, tenantId: {}", educationId, profileId, tenantId);

        // Verify profile belongs to tenant
        profileRepository.findByIdAndTenantId(profileId, tenantId)
                .orElseThrow(() -> new ProfileNotFoundException(profileId));

        Education education = educationRepository.findByIdAndProfileIdAndDeletedAtIsNull(educationId, profileId)
                .orElseThrow(() -> new ValidationException("educationId", "Education record not found"));

        try {
            EducationMapper.updateEntity(education, request);

            // Validate dates after update if both provided
            if (education.getEndDate() != null && education.getEndDate().isBefore(education.getStartDate())) {
                throw new ValidationException("endDate", "End date cannot be before start date");
            }

            Education updatedEducation = educationRepository.save(education);
            log.info("Education record updated successfully: {}", educationId);
            triggerEmbeddingUpdate(updatedEducation);

            return EducationMapper.toResponse(updatedEducation);
        } catch (OptimisticLockingFailureException ex) {
            log.error("Optimistic lock failure for education: {}", educationId, ex);
            throw new OptimisticLockException("Education record was modified by another user. Please refresh and try again.");
        }
    }

    @Transactional
    public void deleteEducation(Long profileId, Long educationId) {
        Long tenantId = TenantContext.getCurrentTenantId();
        log.info("Soft deleting education id: {} for profileId: {}, tenantId: {}", educationId, profileId, tenantId);

        // Verify profile belongs to tenant
        profileRepository.findByIdAndTenantId(profileId, tenantId)
                .orElseThrow(() -> new ProfileNotFoundException(profileId));

        Education education = educationRepository.findByIdAndProfileIdAndDeletedAtIsNull(educationId, profileId)
                .orElseThrow(() -> new ValidationException("educationId", "Education record not found"));

        education.setDeletedAt(Instant.now());
        educationRepository.save(education);
        triggerEmbeddingUpdate(education);
        log.info("Education record soft deleted successfully: {}", educationId);
    }

    private void triggerEmbeddingUpdate(Education education) {
        try {
            contentChangedEventListener.onEducationChanged(education);
        } catch (Exception e) {
            log.warn("Failed to update embedding for education {}: {}", education.getId(), e.getMessage());
        }
    }
}
