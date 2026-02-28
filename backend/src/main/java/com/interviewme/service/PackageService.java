package com.interviewme.service;

import com.interviewme.common.exception.OptimisticLockException;
import com.interviewme.common.exception.PackageNotFoundException;
import com.interviewme.common.exception.ProfileNotFoundException;
import com.interviewme.common.exception.ValidationException;
import com.interviewme.dto.experience.ProjectResponse;
import com.interviewme.dto.experience.StoryResponse;
import com.interviewme.dto.packages.*;
import com.interviewme.dto.skill.UserSkillDto;
import com.interviewme.mapper.ExperienceProjectMapper;
import com.interviewme.mapper.PackageMapper;
import com.interviewme.mapper.SkillMapper;
import com.interviewme.mapper.StoryMapper;
import com.interviewme.model.*;
import com.interviewme.repository.*;
import com.interviewme.util.SlugGenerator;
import com.interviewme.util.SlugValidator;
import com.interviewme.common.util.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PackageService {

    private static final int MAX_SKILLS_PER_PACKAGE = 20;
    private static final int MAX_PROJECTS_PER_PACKAGE = 10;
    private static final int MAX_STORIES_PER_PACKAGE = 10;

    private final ContentPackageRepository packageRepository;
    private final PackageSkillRepository packageSkillRepository;
    private final PackageProjectRepository packageProjectRepository;
    private final PackageStoryRepository packageStoryRepository;
    private final ProfileRepository profileRepository;
    private final UserSkillRepository userSkillRepository;
    private final ExperienceProjectRepository experienceProjectRepository;
    private final StoryRepository storyRepository;

    @Transactional
    public PackageResponse createPackage(Long profileId, CreatePackageRequest request) {
        Long tenantId = TenantContext.getCurrentTenantId();
        log.info("Creating package for profileId: {}, tenantId: {}", profileId, tenantId);

        profileRepository.findByIdAndTenantId(profileId, tenantId)
            .orElseThrow(() -> new ProfileNotFoundException(profileId));

        ContentPackage pkg = PackageMapper.toEntity(request);
        pkg.setTenantId(tenantId);
        pkg.setProfileId(profileId);

        // Handle slug
        if (request.slug() != null && !request.slug().isBlank()) {
            String normalized = SlugValidator.normalizeSlug(request.slug());
            if (!SlugValidator.isValidSlug(normalized)) {
                throw new ValidationException("slug", "Slug must be 3-50 characters, lowercase alphanumeric and hyphens only");
            }
            if (SlugValidator.isReservedSlug(normalized)) {
                throw new ValidationException("slug", "This slug is reserved and cannot be used");
            }
            if (packageRepository.existsBySlugGlobally(normalized)) {
                throw new ValidationException("slug", "Slug '" + normalized + "' is already in use");
            }
            pkg.setSlug(normalized);
        } else {
            String slug = SlugGenerator.generateUniqueSlug(request.name(), packageRepository::existsBySlugGlobally);
            pkg.setSlug(slug);
        }

        // Generate access token
        pkg.setAccessToken(UUID.randomUUID().toString());

        ContentPackage saved = packageRepository.save(pkg);
        log.info("Package created with id: {}", saved.getId());

        return PackageMapper.toResponse(saved);
    }

    @Transactional
    public PackageResponse updatePackage(Long packageId, UpdatePackageRequest request) {
        Long tenantId = TenantContext.getCurrentTenantId();
        log.info("Updating package id: {}, tenantId: {}", packageId, tenantId);

        ContentPackage pkg = packageRepository.findByIdAndDeletedAtIsNull(packageId)
            .orElseThrow(() -> new PackageNotFoundException(packageId));

        try {
            PackageMapper.updateEntity(pkg, request);
            ContentPackage updated = packageRepository.save(pkg);
            log.info("Package updated successfully: {}", packageId);
            return PackageMapper.toResponse(updated);
        } catch (OptimisticLockingFailureException ex) {
            log.error("Optimistic lock failure for package: {}", packageId, ex);
            throw new OptimisticLockException("Package was modified by another user. Please refresh and try again.");
        }
    }

    @Transactional
    public void deletePackage(Long packageId) {
        Long tenantId = TenantContext.getCurrentTenantId();
        log.info("Soft deleting package id: {}, tenantId: {}", packageId, tenantId);

        ContentPackage pkg = packageRepository.findByIdAndDeletedAtIsNull(packageId)
            .orElseThrow(() -> new PackageNotFoundException(packageId));

        // Clean up associations
        packageSkillRepository.deleteByPackageId(packageId);
        packageProjectRepository.deleteByPackageId(packageId);
        packageStoryRepository.deleteByPackageId(packageId);

        pkg.setDeletedAt(Instant.now());
        packageRepository.save(pkg);
        log.info("Package soft deleted successfully: {}", packageId);
    }

    @Transactional(readOnly = true)
    public List<PackageResponse> getPackagesByProfile(Long profileId) {
        Long tenantId = TenantContext.getCurrentTenantId();
        log.debug("Fetching packages for profileId: {}, tenantId: {}", profileId, tenantId);

        List<ContentPackage> packages = packageRepository
            .findByProfileIdAndDeletedAtIsNullOrderByCreatedAtDesc(profileId);

        return packages.stream()
            .map(PackageMapper::toResponse)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PackageDetailResponse getPackageDetail(Long packageId) {
        Long tenantId = TenantContext.getCurrentTenantId();
        log.debug("Fetching package detail id: {}, tenantId: {}", packageId, tenantId);

        ContentPackage pkg = packageRepository.findByIdAndDeletedAtIsNull(packageId)
            .orElseThrow(() -> new PackageNotFoundException(packageId));

        return buildDetailResponse(pkg);
    }

    @Transactional
    public PackageResponse regenerateToken(Long packageId) {
        Long tenantId = TenantContext.getCurrentTenantId();
        log.info("Regenerating token for package id: {}, tenantId: {}", packageId, tenantId);

        ContentPackage pkg = packageRepository.findByIdAndDeletedAtIsNull(packageId)
            .orElseThrow(() -> new PackageNotFoundException(packageId));

        pkg.setAccessToken(UUID.randomUUID().toString());
        ContentPackage updated = packageRepository.save(pkg);
        log.info("Token regenerated for package: {}", packageId);

        return PackageMapper.toResponse(updated);
    }

    // --- Skill management ---

    @Transactional
    public void addSkill(Long packageId, AddPackageItemRequest request) {
        Long tenantId = TenantContext.getCurrentTenantId();
        log.info("Adding skill {} to package {}", request.itemId(), packageId);

        ContentPackage pkg = packageRepository.findByIdAndDeletedAtIsNull(packageId)
            .orElseThrow(() -> new PackageNotFoundException(packageId));

        int count = packageSkillRepository.countByPackageId(packageId);
        if (count >= MAX_SKILLS_PER_PACKAGE) {
            throw new ValidationException("skills", "Maximum " + MAX_SKILLS_PER_PACKAGE + " skills per package");
        }

        UserSkill skill = userSkillRepository.findByIdAndTenantIdAndDeletedAtIsNull(request.itemId(), tenantId)
            .orElseThrow(() -> new ValidationException("itemId", "User skill not found: " + request.itemId()));

        if (!"public".equalsIgnoreCase(skill.getVisibility())) {
            throw new ValidationException("itemId", "Only public skills can be added to a package");
        }

        if (packageSkillRepository.findByPackageIdAndUserSkillId(packageId, request.itemId()).isPresent()) {
            throw new ValidationException("itemId", "Skill already exists in this package");
        }

        PackageSkill ps = new PackageSkill();
        ps.setTenantId(tenantId);
        ps.setPackageId(packageId);
        ps.setUserSkillId(request.itemId());
        ps.setDisplayOrder(request.displayOrder() != null ? request.displayOrder() : count);
        packageSkillRepository.save(ps);
        log.info("Skill {} added to package {}", request.itemId(), packageId);
    }

    @Transactional
    public void removeSkill(Long packageId, Long skillId) {
        log.info("Removing skill {} from package {}", skillId, packageId);
        packageRepository.findByIdAndDeletedAtIsNull(packageId)
            .orElseThrow(() -> new PackageNotFoundException(packageId));

        int deleted = packageSkillRepository.deleteByPackageIdAndUserSkillId(packageId, skillId);
        if (deleted == 0) {
            throw new ValidationException("skillId", "Skill not found in this package");
        }
        log.info("Skill {} removed from package {}", skillId, packageId);
    }

    // --- Project management ---

    @Transactional
    public void addProject(Long packageId, AddPackageItemRequest request) {
        Long tenantId = TenantContext.getCurrentTenantId();
        log.info("Adding project {} to package {}", request.itemId(), packageId);

        packageRepository.findByIdAndDeletedAtIsNull(packageId)
            .orElseThrow(() -> new PackageNotFoundException(packageId));

        int count = packageProjectRepository.countByPackageId(packageId);
        if (count >= MAX_PROJECTS_PER_PACKAGE) {
            throw new ValidationException("projects", "Maximum " + MAX_PROJECTS_PER_PACKAGE + " projects per package");
        }

        ExperienceProject project = experienceProjectRepository.findByIdAndDeletedAtIsNull(request.itemId())
            .orElseThrow(() -> new ValidationException("itemId", "Project not found: " + request.itemId()));

        if (!"public".equalsIgnoreCase(project.getVisibility())) {
            throw new ValidationException("itemId", "Only public projects can be added to a package");
        }

        if (packageProjectRepository.findByPackageIdAndExperienceProjectId(packageId, request.itemId()).isPresent()) {
            throw new ValidationException("itemId", "Project already exists in this package");
        }

        PackageProject pp = new PackageProject();
        pp.setTenantId(tenantId);
        pp.setPackageId(packageId);
        pp.setExperienceProjectId(request.itemId());
        pp.setDisplayOrder(request.displayOrder() != null ? request.displayOrder() : count);
        packageProjectRepository.save(pp);
        log.info("Project {} added to package {}", request.itemId(), packageId);
    }

    @Transactional
    public void removeProject(Long packageId, Long projectId) {
        log.info("Removing project {} from package {}", projectId, packageId);
        packageRepository.findByIdAndDeletedAtIsNull(packageId)
            .orElseThrow(() -> new PackageNotFoundException(packageId));

        int deleted = packageProjectRepository.deleteByPackageIdAndExperienceProjectId(packageId, projectId);
        if (deleted == 0) {
            throw new ValidationException("projectId", "Project not found in this package");
        }
        log.info("Project {} removed from package {}", projectId, packageId);
    }

    // --- Story management ---

    @Transactional
    public void addStory(Long packageId, AddPackageItemRequest request) {
        Long tenantId = TenantContext.getCurrentTenantId();
        log.info("Adding story {} to package {}", request.itemId(), packageId);

        packageRepository.findByIdAndDeletedAtIsNull(packageId)
            .orElseThrow(() -> new PackageNotFoundException(packageId));

        int count = packageStoryRepository.countByPackageId(packageId);
        if (count >= MAX_STORIES_PER_PACKAGE) {
            throw new ValidationException("stories", "Maximum " + MAX_STORIES_PER_PACKAGE + " stories per package");
        }

        Story story = storyRepository.findByIdAndDeletedAtIsNull(request.itemId())
            .orElseThrow(() -> new ValidationException("itemId", "Story not found: " + request.itemId()));

        if (!"public".equalsIgnoreCase(story.getVisibility())) {
            throw new ValidationException("itemId", "Only public stories can be added to a package");
        }

        if (packageStoryRepository.findByPackageIdAndStoryId(packageId, request.itemId()).isPresent()) {
            throw new ValidationException("itemId", "Story already exists in this package");
        }

        PackageStory ps = new PackageStory();
        ps.setTenantId(tenantId);
        ps.setPackageId(packageId);
        ps.setStoryId(request.itemId());
        ps.setDisplayOrder(request.displayOrder() != null ? request.displayOrder() : count);
        packageStoryRepository.save(ps);
        log.info("Story {} added to package {}", request.itemId(), packageId);
    }

    @Transactional
    public void removeStory(Long packageId, Long storyId) {
        log.info("Removing story {} from package {}", storyId, packageId);
        packageRepository.findByIdAndDeletedAtIsNull(packageId)
            .orElseThrow(() -> new PackageNotFoundException(packageId));

        int deleted = packageStoryRepository.deleteByPackageIdAndStoryId(packageId, storyId);
        if (deleted == 0) {
            throw new ValidationException("storyId", "Story not found in this package");
        }
        log.info("Story {} removed from package {}", storyId, packageId);
    }

    // --- Public access ---

    @Transactional
    public PublicPackageResponse getPublicPackage(String slug, String token) {
        log.info("Fetching public package by slug: {}", slug);

        ContentPackage pkg = packageRepository.findBySlugGlobally(slug)
            .orElseThrow(() -> new PackageNotFoundException("Package not found: " + slug));

        if (!pkg.getIsActive()) {
            throw new PackageNotFoundException("Package not found: " + slug);
        }

        // Validate token if package has one
        if (pkg.getAccessToken() != null) {
            if (token == null || !token.equals(pkg.getAccessToken())) {
                throw new PackageNotFoundException("Package not found: " + slug);
            }
            // Check expiry
            if (pkg.getTokenExpiresAt() != null && Instant.now().isAfter(pkg.getTokenExpiresAt())) {
                throw new PackageNotFoundException("Package access has expired");
            }
        }

        // Increment view count
        pkg.setViewCount(pkg.getViewCount() + 1);
        packageRepository.save(pkg);

        // Get profile info
        Profile profile = profileRepository.findByIdAndDeletedAtIsNull(pkg.getProfileId())
            .orElseThrow(() -> new PackageNotFoundException("Package profile not found"));

        // Get items
        List<UserSkillDto> skills = packageSkillRepository.findByPackageIdOrderByDisplayOrderAsc(pkg.getId())
            .stream()
            .filter(ps -> ps.getUserSkill() != null)
            .map(ps -> SkillMapper.toUserSkillDto(ps.getUserSkill()))
            .collect(Collectors.toList());

        List<ProjectResponse> projects = packageProjectRepository.findByPackageIdOrderByDisplayOrderAsc(pkg.getId())
            .stream()
            .filter(pp -> pp.getExperienceProject() != null)
            .map(pp -> ExperienceProjectMapper.toResponse(pp.getExperienceProject(), 0))
            .collect(Collectors.toList());

        List<StoryResponse> stories = packageStoryRepository.findByPackageIdOrderByDisplayOrderAsc(pkg.getId())
            .stream()
            .filter(ps -> ps.getStory() != null)
            .map(ps -> StoryMapper.toResponse(ps.getStory()))
            .collect(Collectors.toList());

        return PackageMapper.toPublicResponse(pkg, profile.getFullName(), profile.getHeadline(),
            skills, projects, stories);
    }

    // --- Private helpers ---

    private PackageDetailResponse buildDetailResponse(ContentPackage pkg) {
        List<PackageItemResponse<UserSkillDto>> skills = packageSkillRepository
            .findByPackageIdOrderByDisplayOrderAsc(pkg.getId())
            .stream()
            .map(ps -> new PackageItemResponse<>(
                ps.getId(),
                ps.getUserSkill() != null ? SkillMapper.toUserSkillDto(ps.getUserSkill()) : null,
                ps.getDisplayOrder()))
            .collect(Collectors.toList());

        List<PackageItemResponse<ProjectResponse>> projects = packageProjectRepository
            .findByPackageIdOrderByDisplayOrderAsc(pkg.getId())
            .stream()
            .map(pp -> new PackageItemResponse<>(
                pp.getId(),
                pp.getExperienceProject() != null ? ExperienceProjectMapper.toResponse(pp.getExperienceProject(), 0) : null,
                pp.getDisplayOrder()))
            .collect(Collectors.toList());

        List<PackageItemResponse<StoryResponse>> stories = packageStoryRepository
            .findByPackageIdOrderByDisplayOrderAsc(pkg.getId())
            .stream()
            .map(ps -> new PackageItemResponse<>(
                ps.getId(),
                ps.getStory() != null ? StoryMapper.toResponse(ps.getStory()) : null,
                ps.getDisplayOrder()))
            .collect(Collectors.toList());

        return PackageMapper.toDetailResponse(pkg, skills, projects, stories);
    }
}
