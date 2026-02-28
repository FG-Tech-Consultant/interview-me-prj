package com.interviewme.service;

import com.interviewme.common.exception.DuplicateSkillException;
import com.interviewme.common.exception.OptimisticLockException;
import com.interviewme.common.exception.SkillNotFoundException;
import com.interviewme.common.exception.ValidationException;
import com.interviewme.common.util.TenantContext;
import com.interviewme.dto.skill.AddUserSkillDto;
import com.interviewme.dto.skill.UpdateUserSkillDto;
import com.interviewme.dto.skill.UserSkillDto;
import com.interviewme.model.Skill;
import com.interviewme.model.UserSkill;
import com.interviewme.mapper.SkillMapper;
import com.interviewme.event.ContentChangedEventListener;
import com.interviewme.repository.SkillRepository;
import com.interviewme.repository.UserSkillRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserSkillService {

    private final UserSkillRepository userSkillRepository;
    private final SkillRepository skillRepository;
    private final ContentChangedEventListener contentChangedEventListener;

    @Transactional
    public UserSkillDto addSkill(Long profileId, AddUserSkillDto dto) {
        Long tenantId = TenantContext.getCurrentTenantId();
        log.info("Adding skill {} to profile {} for tenant {}", dto.skillId(), profileId, tenantId);

        // Validate skill exists and is active
        Skill skill = skillRepository.findById(dto.skillId())
                .orElseThrow(() -> new SkillNotFoundException(dto.skillId()));

        if (!skill.getIsActive()) {
            throw new ValidationException("skillId", "Skill '" + skill.getName() + "' is no longer available");
        }

        // Check for duplicate
        userSkillRepository.findByProfileIdAndSkillIdAndDeletedAtIsNull(profileId, dto.skillId())
                .ifPresent(existing -> {
                    throw new DuplicateSkillException("You already have '" + skill.getName() + "' in your profile");
                });

        // Validate last used date
        if (dto.lastUsedDate() != null && dto.lastUsedDate().isAfter(LocalDate.now())) {
            throw new ValidationException("lastUsedDate", "Last used date cannot be in the future");
        }

        // Validate confidence level
        String confidence = dto.confidenceLevel() != null ? dto.confidenceLevel() : "MEDIUM";
        if (!List.of("LOW", "MEDIUM", "HIGH").contains(confidence)) {
            throw new ValidationException("confidenceLevel", "Confidence level must be LOW, MEDIUM, or HIGH");
        }

        // Validate visibility
        String visibility = dto.visibility() != null ? dto.visibility() : "private";
        if (!List.of("public", "private").contains(visibility)) {
            throw new ValidationException("visibility", "Visibility must be 'public' or 'private'");
        }

        UserSkill userSkill = new UserSkill();
        userSkill.setTenantId(tenantId);
        userSkill.setProfileId(profileId);
        userSkill.setSkillId(dto.skillId());
        userSkill.setYearsOfExperience(dto.yearsOfExperience() != null ? dto.yearsOfExperience() : 0);
        userSkill.setProficiencyDepth(dto.proficiencyDepth());
        userSkill.setLastUsedDate(dto.lastUsedDate());
        userSkill.setConfidenceLevel(confidence);
        userSkill.setTags(dto.tags());
        userSkill.setVisibility(visibility);

        UserSkill saved = userSkillRepository.save(userSkill);
        saved.setSkill(skill);
        log.info("User skill created with id: {} for profile: {}", saved.getId(), profileId);
        triggerEmbeddingUpdate(saved);
        return SkillMapper.toUserSkillDto(saved);
    }

    @Transactional
    public UserSkillDto updateSkill(Long userSkillId, UpdateUserSkillDto dto) {
        Long tenantId = TenantContext.getCurrentTenantId();
        log.info("Updating user skill {} for tenant {}", userSkillId, tenantId);

        UserSkill userSkill = userSkillRepository.findByIdAndTenantIdAndDeletedAtIsNull(userSkillId, tenantId)
                .orElseThrow(() -> new SkillNotFoundException("User skill not found with id: " + userSkillId));

        // Validate last used date
        if (dto.lastUsedDate() != null && dto.lastUsedDate().isAfter(LocalDate.now())) {
            throw new ValidationException("lastUsedDate", "Last used date cannot be in the future");
        }

        // Validate confidence level
        if (dto.confidenceLevel() != null && !List.of("LOW", "MEDIUM", "HIGH").contains(dto.confidenceLevel())) {
            throw new ValidationException("confidenceLevel", "Confidence level must be LOW, MEDIUM, or HIGH");
        }

        // Validate visibility
        if (dto.visibility() != null && !List.of("public", "private").contains(dto.visibility())) {
            throw new ValidationException("visibility", "Visibility must be 'public' or 'private'");
        }

        try {
            if (dto.yearsOfExperience() != null) {
                userSkill.setYearsOfExperience(dto.yearsOfExperience());
            }
            if (dto.proficiencyDepth() != null) {
                userSkill.setProficiencyDepth(dto.proficiencyDepth());
            }
            if (dto.lastUsedDate() != null) {
                userSkill.setLastUsedDate(dto.lastUsedDate());
            }
            if (dto.confidenceLevel() != null) {
                userSkill.setConfidenceLevel(dto.confidenceLevel());
            }
            if (dto.tags() != null) {
                userSkill.setTags(dto.tags());
            }
            if (dto.visibility() != null) {
                userSkill.setVisibility(dto.visibility());
            }

            UserSkill updated = userSkillRepository.save(userSkill);
            log.info("User skill updated: {}", userSkillId);
            triggerEmbeddingUpdate(updated);
            return SkillMapper.toUserSkillDto(updated);
        } catch (OptimisticLockingFailureException ex) {
            log.error("Optimistic lock failure for user skill: {}", userSkillId, ex);
            throw new OptimisticLockException("Skill was modified by another user. Please refresh and try again.");
        }
    }

    @Transactional
    public void deleteSkill(Long userSkillId) {
        Long tenantId = TenantContext.getCurrentTenantId();
        log.info("Soft deleting user skill {} for tenant {}", userSkillId, tenantId);

        UserSkill userSkill = userSkillRepository.findByIdAndTenantIdAndDeletedAtIsNull(userSkillId, tenantId)
                .orElseThrow(() -> new SkillNotFoundException("User skill not found with id: " + userSkillId));

        userSkill.setDeletedAt(Instant.now());
        userSkillRepository.save(userSkill);
        triggerEmbeddingUpdate(userSkill);
        log.info("User skill soft deleted: {}", userSkillId);
    }

    @Transactional(readOnly = true)
    public Map<String, List<UserSkillDto>> getSkillsByProfile(Long profileId) {
        Long tenantId = TenantContext.getCurrentTenantId();
        log.debug("Fetching skills for profile {} tenant {}", profileId, tenantId);

        List<UserSkill> skills = userSkillRepository.findByProfileIdAndTenantId(profileId, tenantId);

        return skills.stream()
                .map(SkillMapper::toUserSkillDto)
                .collect(Collectors.groupingBy(
                        dto -> dto.skill() != null ? dto.skill().category() : "Other",
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
    }

    @Transactional(readOnly = true)
    public UserSkillDto getSkillById(Long userSkillId) {
        Long tenantId = TenantContext.getCurrentTenantId();
        log.debug("Fetching user skill {} for tenant {}", userSkillId, tenantId);

        UserSkill userSkill = userSkillRepository.findByIdAndTenantIdAndDeletedAtIsNull(userSkillId, tenantId)
                .orElseThrow(() -> new SkillNotFoundException("User skill not found with id: " + userSkillId));

        return SkillMapper.toUserSkillDto(userSkill);
    }

    @Transactional(readOnly = true)
    public List<UserSkillDto> getPublicSkillsByProfile(Long profileId) {
        log.debug("Fetching public skills for profile {}", profileId);

        return userSkillRepository.findByProfileIdAndVisibilityAndDeletedAtIsNull(profileId, "public")
                .stream()
                .map(SkillMapper::toUserSkillDto)
                .toList();
    }

    private void triggerEmbeddingUpdate(UserSkill skill) {
        try {
            contentChangedEventListener.onSkillChanged(skill);
        } catch (Exception e) {
            log.warn("Failed to update embedding for skill {}: {}", skill.getId(), e.getMessage());
        }
    }
}
