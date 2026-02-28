package com.interviewme.service;

import com.interviewme.common.exception.DuplicateSkillException;
import com.interviewme.common.exception.SkillNotFoundException;
import com.interviewme.dto.skill.CreateSkillDto;
import com.interviewme.dto.skill.SkillDto;
import com.interviewme.dto.skill.UpdateSkillDto;
import com.interviewme.model.Skill;
import com.interviewme.repository.SkillRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SkillService")
class SkillServiceTest {

    @Mock
    private SkillRepository skillRepository;

    @InjectMocks
    private SkillService skillService;

    private Skill buildSkill(Long id, String name, String category, boolean active) {
        Skill s = new Skill();
        s.setId(id);
        s.setName(name);
        s.setCategory(category);
        s.setIsActive(active);
        s.setDescription("desc");
        s.setTags(List.of("tag1"));
        return s;
    }

    @Nested
    @DisplayName("searchActive")
    class SearchActive {

        @Test
        @DisplayName("returns matching active skills")
        void returnsMatchingActiveSkills() {
            Skill skill = buildSkill(1L, "Java", "Language", true);
            when(skillRepository.findTop10ByNameContainingIgnoreCaseAndIsActiveTrueOrderByName("Jav"))
                    .thenReturn(List.of(skill));

            List<SkillDto> result = skillService.searchActive("Jav");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).name()).isEqualTo("Java");
        }

        @Test
        @DisplayName("returns empty list for null query")
        void returnsEmptyForNullQuery() {
            List<SkillDto> result = skillService.searchActive(null);
            assertThat(result).isEmpty();
            verifyNoInteractions(skillRepository);
        }

        @Test
        @DisplayName("returns empty list for blank query")
        void returnsEmptyForBlankQuery() {
            List<SkillDto> result = skillService.searchActive("   ");
            assertThat(result).isEmpty();
            verifyNoInteractions(skillRepository);
        }

        @Test
        @DisplayName("trims query whitespace")
        void trimsQueryWhitespace() {
            when(skillRepository.findTop10ByNameContainingIgnoreCaseAndIsActiveTrueOrderByName("Java"))
                    .thenReturn(List.of());

            skillService.searchActive("  Java  ");

            verify(skillRepository).findTop10ByNameContainingIgnoreCaseAndIsActiveTrueOrderByName("Java");
        }
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("returns skill when found")
        void returnsSkillWhenFound() {
            Skill skill = buildSkill(1L, "Java", "Language", true);
            when(skillRepository.findById(1L)).thenReturn(Optional.of(skill));

            SkillDto result = skillService.findById(1L);

            assertThat(result.id()).isEqualTo(1L);
            assertThat(result.name()).isEqualTo("Java");
        }

        @Test
        @DisplayName("throws SkillNotFoundException when not found")
        void throwsWhenNotFound() {
            when(skillRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> skillService.findById(99L))
                    .isInstanceOf(SkillNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("findAll")
    class FindAll {

        @Test
        @DisplayName("returns all catalog skills")
        void returnsAllSkills() {
            Skill s1 = buildSkill(1L, "Java", "Language", true);
            Skill s2 = buildSkill(2L, "Python", "Language", false);
            when(skillRepository.findAll()).thenReturn(List.of(s1, s2));

            List<SkillDto> result = skillService.findAll();

            assertThat(result).hasSize(2);
        }
    }

    @Nested
    @DisplayName("createSkill")
    class CreateSkill {

        @Test
        @DisplayName("creates skill successfully")
        void createsSkillSuccessfully() {
            CreateSkillDto dto = new CreateSkillDto("React", "Frontend", "UI library", List.of("ui"));
            when(skillRepository.findByNameIgnoreCase("React")).thenReturn(Optional.empty());
            Skill saved = buildSkill(10L, "React", "Frontend", true);
            when(skillRepository.save(any(Skill.class))).thenReturn(saved);

            SkillDto result = skillService.createSkill(dto);

            assertThat(result.name()).isEqualTo("React");
            verify(skillRepository).save(any(Skill.class));
        }

        @Test
        @DisplayName("throws DuplicateSkillException for existing name")
        void throwsForDuplicateName() {
            CreateSkillDto dto = new CreateSkillDto("Java", "Language", null, null);
            Skill existing = buildSkill(1L, "Java", "Language", true);
            when(skillRepository.findByNameIgnoreCase("Java")).thenReturn(Optional.of(existing));

            assertThatThrownBy(() -> skillService.createSkill(dto))
                    .isInstanceOf(DuplicateSkillException.class)
                    .hasMessageContaining("already exists");
        }

        @Test
        @DisplayName("trims skill name before duplicate check")
        void trimsNameBeforeDuplicateCheck() {
            CreateSkillDto dto = new CreateSkillDto("  React  ", "Frontend", null, null);
            when(skillRepository.findByNameIgnoreCase("React")).thenReturn(Optional.empty());
            Skill saved = buildSkill(10L, "React", "Frontend", true);
            when(skillRepository.save(any(Skill.class))).thenReturn(saved);

            skillService.createSkill(dto);

            verify(skillRepository).findByNameIgnoreCase("React");
        }
    }

    @Nested
    @DisplayName("updateSkill")
    class UpdateSkill {

        @Test
        @DisplayName("updates skill successfully")
        void updatesSkillSuccessfully() {
            Skill existing = buildSkill(1L, "Java", "Language", true);
            when(skillRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(skillRepository.save(any(Skill.class))).thenReturn(existing);

            UpdateSkillDto dto = new UpdateSkillDto("Java 21", null, null, null);
            when(skillRepository.findByNameIgnoreCase("Java 21")).thenReturn(Optional.empty());

            SkillDto result = skillService.updateSkill(1L, dto);

            assertThat(result).isNotNull();
            verify(skillRepository).save(existing);
        }

        @Test
        @DisplayName("throws SkillNotFoundException for missing skill")
        void throwsForMissingSkill() {
            when(skillRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> skillService.updateSkill(99L, new UpdateSkillDto(null, null, null, null)))
                    .isInstanceOf(SkillNotFoundException.class);
        }

        @Test
        @DisplayName("throws DuplicateSkillException when renaming to existing name")
        void throwsWhenRenamingToDuplicate() {
            Skill existing = buildSkill(1L, "Java", "Language", true);
            when(skillRepository.findById(1L)).thenReturn(Optional.of(existing));
            Skill other = buildSkill(2L, "Python", "Language", true);
            when(skillRepository.findByNameIgnoreCase("Python")).thenReturn(Optional.of(other));

            assertThatThrownBy(() -> skillService.updateSkill(1L, new UpdateSkillDto("Python", null, null, null)))
                    .isInstanceOf(DuplicateSkillException.class);
        }

        @Test
        @DisplayName("allows update when name is unchanged (case-insensitive)")
        void allowsUpdateWithSameName() {
            Skill existing = buildSkill(1L, "Java", "Language", true);
            when(skillRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(skillRepository.save(any(Skill.class))).thenReturn(existing);

            SkillDto result = skillService.updateSkill(1L, new UpdateSkillDto("java", "Backend", null, null));

            assertThat(result).isNotNull();
            verify(skillRepository, never()).findByNameIgnoreCase(anyString());
        }
    }

    @Nested
    @DisplayName("deactivateSkill")
    class DeactivateSkill {

        @Test
        @DisplayName("deactivates an active skill")
        void deactivatesActiveSkill() {
            Skill skill = buildSkill(1L, "Java", "Language", true);
            when(skillRepository.findById(1L)).thenReturn(Optional.of(skill));
            when(skillRepository.save(any(Skill.class))).thenAnswer(inv -> inv.getArgument(0));

            SkillDto result = skillService.deactivateSkill(1L);

            assertThat(skill.getIsActive()).isFalse();
        }

        @Test
        @DisplayName("throws SkillNotFoundException for missing skill")
        void throwsForMissingSkill() {
            when(skillRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> skillService.deactivateSkill(99L))
                    .isInstanceOf(SkillNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("reactivateSkill")
    class ReactivateSkill {

        @Test
        @DisplayName("reactivates an inactive skill")
        void reactivatesInactiveSkill() {
            Skill skill = buildSkill(1L, "Java", "Language", false);
            when(skillRepository.findById(1L)).thenReturn(Optional.of(skill));
            when(skillRepository.save(any(Skill.class))).thenAnswer(inv -> inv.getArgument(0));

            SkillDto result = skillService.reactivateSkill(1L);

            assertThat(skill.getIsActive()).isTrue();
        }
    }
}
