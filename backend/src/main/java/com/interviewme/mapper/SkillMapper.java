package com.interviewme.mapper;

import com.interviewme.dto.skill.CreateSkillDto;
import com.interviewme.dto.skill.SkillDto;
import com.interviewme.dto.skill.UpdateSkillDto;
import com.interviewme.dto.skill.UserSkillDto;
import com.interviewme.model.Skill;
import com.interviewme.model.UserSkill;

public class SkillMapper {

    public static SkillDto toDto(Skill entity) {
        return new SkillDto(
            entity.getId(),
            entity.getName(),
            entity.getCategory(),
            entity.getDescription(),
            entity.getTags(),
            entity.getIsActive()
        );
    }

    public static Skill toEntity(CreateSkillDto dto) {
        Skill skill = new Skill();
        skill.setName(dto.name().trim());
        skill.setCategory(dto.category().trim());
        skill.setDescription(dto.description());
        skill.setTags(dto.tags());
        skill.setIsActive(true);
        return skill;
    }

    public static void updateEntity(Skill entity, UpdateSkillDto dto) {
        if (dto.name() != null) {
            entity.setName(dto.name().trim());
        }
        if (dto.category() != null) {
            entity.setCategory(dto.category().trim());
        }
        if (dto.description() != null) {
            entity.setDescription(dto.description());
        }
        if (dto.tags() != null) {
            entity.setTags(dto.tags());
        }
    }

    public static UserSkillDto toUserSkillDto(UserSkill entity) {
        return new UserSkillDto(
            entity.getId(),
            entity.getSkill() != null ? toDto(entity.getSkill()) : null,
            entity.getYearsOfExperience(),
            entity.getProficiencyDepth(),
            entity.getLastUsedDate(),
            entity.getConfidenceLevel(),
            entity.getTags(),
            entity.getVisibility(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }
}
