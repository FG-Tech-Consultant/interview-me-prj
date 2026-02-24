package com.interviewme.service;

import com.interviewme.common.exception.DuplicateProfileException;
import com.interviewme.common.exception.OptimisticLockException;
import com.interviewme.common.exception.ProfileNotFoundException;
import com.interviewme.common.exception.ValidationException;
import com.interviewme.util.SlugValidator;
import com.interviewme.dto.profile.CreateProfileRequest;
import com.interviewme.dto.profile.ProfileResponse;
import com.interviewme.dto.profile.UpdateProfileRequest;
import com.interviewme.mapper.ProfileMapper;
import com.interviewme.model.Profile;
import com.interviewme.repository.ProfileRepository;
import com.interviewme.common.util.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProfileService {

    private final ProfileRepository profileRepository;

    @Transactional(readOnly = true)
    public ProfileResponse getProfileByUserId(Long userId) {
        Long tenantId = TenantContext.getCurrentTenantId();
        log.debug("Fetching profile for userId: {}, tenantId: {}", userId, tenantId);

        Profile profile = profileRepository.findByUserIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ProfileNotFoundException("Profile not found for user: " + userId));

        return ProfileMapper.toResponse(profile);
    }

    @Transactional(readOnly = true)
    public ProfileResponse getProfileById(Long profileId) {
        Long tenantId = TenantContext.getCurrentTenantId();
        log.debug("Fetching profile by id: {}, tenantId: {}", profileId, tenantId);

        Profile profile = profileRepository.findByIdAndTenantId(profileId, tenantId)
                .orElseThrow(() -> new ProfileNotFoundException(profileId));

        return ProfileMapper.toResponse(profile);
    }

    @Transactional
    public ProfileResponse createProfile(Long userId, CreateProfileRequest request) {
        Long tenantId = TenantContext.getCurrentTenantId();
        log.info("Creating profile for userId: {}, tenantId: {}", userId, tenantId);

        // Check for duplicate profile
        if (profileRepository.existsByUserIdAndDeletedAtIsNull(userId)) {
            throw new DuplicateProfileException("Profile already exists for user: " + userId);
        }

        Profile profile = ProfileMapper.toEntity(request);
        profile.setUserId(userId);
        profile.setTenantId(tenantId);

        Profile savedProfile = profileRepository.save(profile);
        log.info("Profile created with id: {}", savedProfile.getId());

        return ProfileMapper.toResponse(savedProfile);
    }

    @Transactional
    public ProfileResponse updateProfile(Long profileId, UpdateProfileRequest request) {
        Long tenantId = TenantContext.getCurrentTenantId();
        log.info("Updating profile id: {}, tenantId: {}", profileId, tenantId);

        Profile profile = profileRepository.findByIdAndTenantId(profileId, tenantId)
                .orElseThrow(() -> new ProfileNotFoundException(profileId));

        try {
            ProfileMapper.updateEntity(profile, request);
            Profile updatedProfile = profileRepository.save(profile);
            log.info("Profile updated successfully: {}", profileId);

            return ProfileMapper.toResponse(updatedProfile);
        } catch (OptimisticLockingFailureException ex) {
            log.error("Optimistic lock failure for profile: {}", profileId, ex);
            throw new OptimisticLockException("Profile was modified by another user. Please refresh and try again.");
        }
    }

    @Transactional
    public void deleteProfile(Long profileId) {
        Long tenantId = TenantContext.getCurrentTenantId();
        log.info("Soft deleting profile id: {}, tenantId: {}", profileId, tenantId);

        Profile profile = profileRepository.findByIdAndTenantId(profileId, tenantId)
                .orElseThrow(() -> new ProfileNotFoundException(profileId));

        profile.setDeletedAt(Instant.now());
        profileRepository.save(profile);
        log.info("Profile soft deleted successfully: {}", profileId);
    }

    @Transactional(readOnly = true)
    public boolean profileExists(Long userId) {
        return profileRepository.existsByUserIdAndDeletedAtIsNull(userId);
    }

    @Transactional
    public ProfileResponse updateSlug(Long profileId, String slug) {
        Long tenantId = TenantContext.getCurrentTenantId();
        log.info("Updating slug for profile id: {}, tenantId: {}, slug: {}", profileId, tenantId, slug);

        String normalized = SlugValidator.normalizeSlug(slug);

        if (!SlugValidator.isValidSlug(normalized)) {
            throw new ValidationException("slug", "Slug must be 3-50 characters, lowercase alphanumeric and hyphens only, no consecutive hyphens");
        }

        if (SlugValidator.isReservedSlug(normalized)) {
            throw new ValidationException("slug", "This slug is reserved and cannot be used");
        }

        if (profileRepository.existsBySlug(normalized)) {
            throw new DuplicateProfileException("Slug '" + normalized + "' is already in use");
        }

        Profile profile = profileRepository.findByIdAndTenantId(profileId, tenantId)
                .orElseThrow(() -> new ProfileNotFoundException(profileId));

        profile.setSlug(normalized);
        Profile updatedProfile = profileRepository.save(profile);
        log.info("Slug updated successfully for profile: {}", profileId);

        return ProfileMapper.toResponse(updatedProfile);
    }
}
