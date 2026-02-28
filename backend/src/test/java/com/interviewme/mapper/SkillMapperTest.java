package com.interviewme.mapper;

import com.interviewme.dto.skill.CreateSkillDto;
import com.interviewme.dto.skill.SkillDto;
import com.interviewme.dto.skill.UpdateSkillDto;
import com.interviewme.dto.skill.UserSkillDto;
import com.interviewme.model.Skill;
import com.interviewme.model.UserSkill;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class SkillMapperTest {

    @Nested
    @DisplayName("toDto")
    class ToDto {

        @Test
        @DisplayName("should map all fields from Skill entity")
        void shouldMapAllFields() {
            Skill skill = createTestSkill();

            SkillDto dto = SkillMapper.toDto(skill);

            assertThat(dto.id()).isEqualTo(1L);
            assertThat(dto.name()).isEqualTo("Java");
            assertThat(dto.category()).isEqualTo("Programming Language");
            assertThat(dto.description()).isEqualTo("A versatile language");
            assertThat(dto.tags()).containsExactly("backend", "enterprise");
            assertThat(dto.isActive()).isTrue();
        }

        @Test
        @DisplayName("should handle null tags")
        void shouldHandleNullTags() {
            Skill skill = createTestSkill();
            skill.setTags(null);

            SkillDto dto = SkillMapper.toDto(skill);

            assertThat(dto.tags()).isNull();
        }
    }

    @Nested
    @DisplayName("toEntity")
    class ToEntity {

        @Test
        @DisplayName("should map all fields from CreateSkillDto")
        void shouldMapAllFields() {
            CreateSkillDto dto = new CreateSkillDto(
                    " Java ", " Programming Language ",
                    "A versatile language", List.of("backend", "enterprise")
            );

            Skill entity = SkillMapper.toEntity(dto);

            assertThat(entity.getName()).isEqualTo("Java");
            assertThat(entity.getCategory()).isEqualTo("Programming Language");
            assertThat(entity.getDescription()).isEqualTo("A versatile language");
            assertThat(entity.getTags()).containsExactly("backend", "enterprise");
            assertThat(entity.getIsActive()).isTrue();
        }

        @Test
        @DisplayName("should trim name and category")
        void shouldTrimNameAndCategory() {
            CreateSkillDto dto = new CreateSkillDto("  Python  ", "  Language  ", null, null);

            Skill entity = SkillMapper.toEntity(dto);

            assertThat(entity.getName()).isEqualTo("Python");
            assertThat(entity.getCategory()).isEqualTo("Language");
        }
    }

    @Nested
    @DisplayName("updateEntity")
    class UpdateEntity {

        @Test
        @DisplayName("should update only non-null fields")
        void shouldUpdateOnlyNonNullFields() {
            Skill entity = createTestSkill();
            UpdateSkillDto dto = new UpdateSkillDto("Python", null, null, null);

            SkillMapper.updateEntity(entity, dto);

            assertThat(entity.getName()).isEqualTo("Python");
            assertThat(entity.getCategory()).isEqualTo("Programming Language"); // unchanged
            assertThat(entity.getDescription()).isEqualTo("A versatile language"); // unchanged
            assertThat(entity.getTags()).containsExactly("backend", "enterprise"); // unchanged
        }

        @Test
        @DisplayName("should update all fields when all provided")
        void shouldUpdateAllFields() {
            Skill entity = createTestSkill();
            UpdateSkillDto dto = new UpdateSkillDto(
                    " Python ", " AI/ML ", "Machine learning", List.of("ml", "ai")
            );

            SkillMapper.updateEntity(entity, dto);

            assertThat(entity.getName()).isEqualTo("Python");
            assertThat(entity.getCategory()).isEqualTo("AI/ML");
            assertThat(entity.getDescription()).isEqualTo("Machine learning");
            assertThat(entity.getTags()).containsExactly("ml", "ai");
        }

        @Test
        @DisplayName("should not change any fields when all are null")
        void shouldNotChangeWhenAllNull() {
            Skill entity = createTestSkill();
            UpdateSkillDto dto = new UpdateSkillDto(null, null, null, null);

            SkillMapper.updateEntity(entity, dto);

            assertThat(entity.getName()).isEqualTo("Java");
            assertThat(entity.getCategory()).isEqualTo("Programming Language");
        }
    }

    @Nested
    @DisplayName("toUserSkillDto")
    class ToUserSkillDto {

        @Test
        @DisplayName("should map all fields from UserSkill entity")
        void shouldMapAllFields() {
            UserSkill userSkill = createTestUserSkill();

            UserSkillDto dto = SkillMapper.toUserSkillDto(userSkill);

            assertThat(dto.id()).isEqualTo(1L);
            assertThat(dto.skill()).isNotNull();
            assertThat(dto.skill().name()).isEqualTo("Java");
            assertThat(dto.yearsOfExperience()).isEqualTo(5);
            assertThat(dto.proficiencyDepth()).isEqualTo(8);
            assertThat(dto.lastUsedDate()).isEqualTo(LocalDate.of(2024, 1, 1));
            assertThat(dto.confidenceLevel()).isEqualTo("HIGH");
            assertThat(dto.tags()).containsExactly("backend");
            assertThat(dto.visibility()).isEqualTo("public");
        }

        @Test
        @DisplayName("should handle null skill reference")
        void shouldHandleNullSkill() {
            UserSkill userSkill = createTestUserSkill();
            userSkill.setSkill(null);

            UserSkillDto dto = SkillMapper.toUserSkillDto(userSkill);

            assertThat(dto.skill()).isNull();
        }
    }

    private Skill createTestSkill() {
        Skill skill = new Skill();
        skill.setId(1L);
        skill.setName("Java");
        skill.setCategory("Programming Language");
        skill.setDescription("A versatile language");
        skill.setTags(List.of("backend", "enterprise"));
        skill.setIsActive(true);
        skill.setCreatedAt(Instant.now());
        skill.setUpdatedAt(Instant.now());
        return skill;
    }

    private UserSkill createTestUserSkill() {
        UserSkill userSkill = new UserSkill();
        userSkill.setId(1L);
        userSkill.setTenantId(10L);
        userSkill.setProfileId(100L);
        userSkill.setSkill(createTestSkill());
        userSkill.setSkillId(1L);
        userSkill.setYearsOfExperience(5);
        userSkill.setProficiencyDepth(8);
        userSkill.setLastUsedDate(LocalDate.of(2024, 1, 1));
        userSkill.setConfidenceLevel("HIGH");
        userSkill.setTags(List.of("backend"));
        userSkill.setVisibility("public");
        userSkill.setCreatedAt(Instant.now());
        userSkill.setUpdatedAt(Instant.now());
        userSkill.setVersion(0L);
        return userSkill;
    }
}
