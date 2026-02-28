package com.interviewme.service;

import com.interviewme.common.exception.DuplicateProfileException;
import com.interviewme.common.exception.OptimisticLockException;
import com.interviewme.common.exception.ProfileNotFoundException;
import com.interviewme.common.exception.ValidationException;
import com.interviewme.event.ContentChangedEventListener;
import com.interviewme.util.SlugGenerator;
import com.interviewme.util.SlugValidator;
import com.interviewme.billing.config.BillingProperties;
import com.interviewme.billing.model.RefType;
import com.interviewme.billing.service.CoinWalletService;
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
    private final CoinWalletService coinWalletService;
    private final BillingProperties billingProperties;
    private final ContentChangedEventListener contentChangedEventListener;

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

        // Auto-generate unique slug from full name (global check bypasses tenant filter)
        String slug = SlugGenerator.generateUniqueSlug(
            request.fullName(),
            profileRepository::existsBySlugGlobally
        );
        profile.setSlug(slug);
        log.info("Auto-generated slug: {} for userId: {}", slug, userId);

        Profile savedProfile = profileRepository.save(profile);
        log.info("Profile created with id: {}", savedProfile.getId());
        triggerEmbeddingUpdate(savedProfile);

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
            triggerEmbeddingUpdate(updatedProfile);

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
        triggerEmbeddingUpdate(profile);
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

        if (profileRepository.existsBySlugGlobally(normalized)) {
            throw new DuplicateProfileException("Slug '" + normalized + "' is already in use");
        }

        Profile profile = profileRepository.findByIdAndTenantId(profileId, tenantId)
                .orElseThrow(() -> new ProfileNotFoundException(profileId));

        // Charge coins if user has already changed their slug before (first change is free)
        if (profile.getSlugChangeCount() > 0) {
            int cost = billingProperties.getCosts().getOrDefault("SLUG_CHANGE", 5);
            coinWalletService.spend(tenantId, cost, RefType.SLUG_CHANGE, String.valueOf(profileId), "Slug change: " + normalized);
            log.info("Charged {} coins for slug change on profile: {}", cost, profileId);
        }

        profile.setSlug(normalized);
        profile.setSlugChangeCount(profile.getSlugChangeCount() + 1);
        Profile updatedProfile = profileRepository.save(profile);
        log.info("Slug updated successfully for profile: {}", profileId);

        return ProfileMapper.toResponse(updatedProfile);
    }

    private void triggerEmbeddingUpdate(Profile profile) {
        try {
            contentChangedEventListener.onProfileChanged(profile);
        } catch (Exception e) {
            log.warn("Failed to update embedding for profile {}: {}", profile.getId(), e.getMessage());
        }
    }
}
