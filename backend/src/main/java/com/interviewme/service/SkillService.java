package com.interviewme.service;

import com.interviewme.common.exception.DuplicateSkillException;
import com.interviewme.common.exception.SkillNotFoundException;
import com.interviewme.dto.skill.CreateSkillDto;
import com.interviewme.dto.skill.SkillDto;
import com.interviewme.dto.skill.UpdateSkillDto;
import com.interviewme.graph.GraphSkillSyncService;
import com.interviewme.model.Skill;
import com.interviewme.mapper.SkillMapper;
import com.interviewme.repository.SkillRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SkillService {

    private final SkillRepository skillRepository;
    private final GraphSkillSyncService graphSkillSyncService;

    @Transactional(readOnly = true)
    public List<SkillDto> searchActive(String query) {
        log.debug("Searching active skills with query: {}", query);
        if (query == null || query.trim().isEmpty()) {
            return List.of();
        }
        return skillRepository.findTop10ByNameContainingIgnoreCaseAndIsActiveTrueOrderByName(query.trim())
                .stream()
                .map(SkillMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public SkillDto findById(Long id) {
        log.debug("Fetching skill by id: {}", id);
        Skill skill = skillRepository.findById(id)
                .orElseThrow(() -> new SkillNotFoundException(id));
        return SkillMapper.toDto(skill);
    }

    @Transactional(readOnly = true)
    public List<SkillDto> findAll() {
        log.debug("Fetching all catalog skills");
        return skillRepository.findAll()
                .stream()
                .map(SkillMapper::toDto)
                .toList();
    }

    @Transactional
    public SkillDto createSkill(CreateSkillDto dto) {
        log.info("Creating catalog skill: {}", dto.name());

        skillRepository.findByNameIgnoreCase(dto.name().trim())
                .ifPresent(existing -> {
                    throw new DuplicateSkillException("Skill '" + existing.getName() + "' already exists in catalog");
                });

        Skill skill = SkillMapper.toEntity(dto);
        Skill saved = skillRepository.save(skill);
        log.info("Catalog skill created with id: {}", saved.getId());
        graphSkillSyncService.syncSkill(saved);
        return SkillMapper.toDto(saved);
    }

    @Transactional
    public SkillDto updateSkill(Long id, UpdateSkillDto dto) {
        log.info("Updating catalog skill id: {}", id);

        Skill skill = skillRepository.findById(id)
                .orElseThrow(() -> new SkillNotFoundException(id));

        if (dto.name() != null && !dto.name().trim().equalsIgnoreCase(skill.getName())) {
            skillRepository.findByNameIgnoreCase(dto.name().trim())
                    .ifPresent(existing -> {
                        throw new DuplicateSkillException("Skill '" + existing.getName() + "' already exists in catalog");
                    });
        }

        SkillMapper.updateEntity(skill, dto);
        Skill updated = skillRepository.save(skill);
        log.info("Catalog skill updated: {}", id);
        graphSkillSyncService.syncSkill(updated);
        return SkillMapper.toDto(updated);
    }

    @Transactional
    public SkillDto deactivateSkill(Long id) {
        log.info("Deactivating catalog skill id: {}", id);

        Skill skill = skillRepository.findById(id)
                .orElseThrow(() -> new SkillNotFoundException(id));

        skill.setIsActive(false);
        Skill updated = skillRepository.save(skill);
        log.info("Catalog skill deactivated: {}", id);
        return SkillMapper.toDto(updated);
    }

    @Transactional
    public SkillDto reactivateSkill(Long id) {
        log.info("Reactivating catalog skill id: {}", id);

        Skill skill = skillRepository.findById(id)
                .orElseThrow(() -> new SkillNotFoundException(id));

        skill.setIsActive(true);
        Skill updated = skillRepository.save(skill);
        log.info("Catalog skill reactivated: {}", id);
        return SkillMapper.toDto(updated);
    }
}
