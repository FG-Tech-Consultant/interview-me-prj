package com.interviewme.service;

import com.interviewme.common.exception.DuplicateSkillException;
import com.interviewme.common.exception.OptimisticLockException;
import com.interviewme.common.exception.SkillNotFoundException;
import com.interviewme.common.exception.ValidationException;
import com.interviewme.common.util.TenantContext;
import com.interviewme.dto.skill.AddUserSkillDto;
import com.interviewme.dto.skill.UpdateUserSkillDto;
import com.interviewme.dto.skill.UserSkillDto;
import com.interviewme.event.ContentChangedEventListener;
import com.interviewme.model.Skill;
import com.interviewme.model.UserSkill;
import com.interviewme.repository.SkillRepository;
import com.interviewme.repository.UserSkillRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserSkillService")
class UserSkillServiceTest {

    @Mock private UserSkillRepository userSkillRepository;
    @Mock private SkillRepository skillRepository;
    @Mock private ContentChangedEventListener contentChangedEventListener;

    @InjectMocks
    private UserSkillService userSkillService;

    private MockedStatic<TenantContext> tenantContextMock;

    private static final Long TENANT_ID = 100L;
    private static final Long PROFILE_ID = 10L;

    @BeforeEach
    void setUp() {
        tenantContextMock = mockStatic(TenantContext.class);
        tenantContextMock.when(TenantContext::getCurrentTenantId).thenReturn(TENANT_ID);
    }

    @AfterEach
    void tearDown() {
        tenantContextMock.close();
    }

    private Skill buildCatalogSkill(Long id, String name, boolean active) {
        Skill s = new Skill();
        s.setId(id);
        s.setName(name);
        s.setCategory("Language");
        s.setIsActive(active);
        return s;
    }

    private UserSkill buildUserSkill(Long id, Long profileId, Skill skill) {
        UserSkill us = new UserSkill();
        us.setId(id);
        us.setTenantId(TENANT_ID);
        us.setProfileId(profileId);
        us.setSkillId(skill.getId());
        us.setSkill(skill);
        us.setYearsOfExperience(5);
        us.setProficiencyDepth(3);
        us.setConfidenceLevel("MEDIUM");
        us.setVisibility("private");
        return us;
    }

    @Nested
    @DisplayName("addSkill")
    class AddSkill {

        @Test
        @DisplayName("adds skill successfully with defaults")
        void addsSkillWithDefaults() {
            Skill catalog = buildCatalogSkill(1L, "Java", true);
            when(skillRepository.findById(1L)).thenReturn(Optional.of(catalog));
            when(userSkillRepository.findByProfileIdAndSkillIdAndDeletedAtIsNull(PROFILE_ID, 1L))
                    .thenReturn(Optional.empty());

            UserSkill saved = buildUserSkill(50L, PROFILE_ID, catalog);
            when(userSkillRepository.save(any(UserSkill.class))).thenReturn(saved);
            when(userSkillRepository.findByIdAndTenantIdAndDeletedAtIsNull(50L, TENANT_ID))
                    .thenReturn(Optional.of(saved));

            AddUserSkillDto dto = new AddUserSkillDto(1L, null, 3, null, null, null, null);

            UserSkillDto result = userSkillService.addSkill(PROFILE_ID, dto);

            assertThat(result).isNotNull();
            verify(userSkillRepository).save(argThat(us ->
                    us.getYearsOfExperience() == 0 &&
                    us.getConfidenceLevel().equals("MEDIUM") &&
                    us.getVisibility().equals("private")
            ));
        }

        @Test
        @DisplayName("throws SkillNotFoundException when catalog skill missing")
        void throwsWhenCatalogSkillMissing() {
            when(skillRepository.findById(99L)).thenReturn(Optional.empty());

            AddUserSkillDto dto = new AddUserSkillDto(99L, 5, 3, null, null, null, null);

            assertThatThrownBy(() -> userSkillService.addSkill(PROFILE_ID, dto))
                    .isInstanceOf(SkillNotFoundException.class);
        }

        @Test
        @DisplayName("throws ValidationException when catalog skill is inactive")
        void throwsWhenSkillInactive() {
            Skill inactive = buildCatalogSkill(1L, "COBOL", false);
            when(skillRepository.findById(1L)).thenReturn(Optional.of(inactive));

            AddUserSkillDto dto = new AddUserSkillDto(1L, 5, 3, null, null, null, null);

            assertThatThrownBy(() -> userSkillService.addSkill(PROFILE_ID, dto))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("no longer available");
        }

        @Test
        @DisplayName("throws DuplicateSkillException when skill already added")
        void throwsForDuplicate() {
            Skill catalog = buildCatalogSkill(1L, "Java", true);
            when(skillRepository.findById(1L)).thenReturn(Optional.of(catalog));
            UserSkill existing = buildUserSkill(50L, PROFILE_ID, catalog);
            when(userSkillRepository.findByProfileIdAndSkillIdAndDeletedAtIsNull(PROFILE_ID, 1L))
                    .thenReturn(Optional.of(existing));

            AddUserSkillDto dto = new AddUserSkillDto(1L, 5, 3, null, null, null, null);

            assertThatThrownBy(() -> userSkillService.addSkill(PROFILE_ID, dto))
                    .isInstanceOf(DuplicateSkillException.class);
        }

        @Test
        @DisplayName("throws ValidationException when lastUsedDate is in the future")
        void throwsForFutureLastUsedDate() {
            Skill catalog = buildCatalogSkill(1L, "Java", true);
            when(skillRepository.findById(1L)).thenReturn(Optional.of(catalog));
            when(userSkillRepository.findByProfileIdAndSkillIdAndDeletedAtIsNull(PROFILE_ID, 1L))
                    .thenReturn(Optional.empty());

            AddUserSkillDto dto = new AddUserSkillDto(1L, 5, 3, LocalDate.now().plusDays(1), null, null, null);

            assertThatThrownBy(() -> userSkillService.addSkill(PROFILE_ID, dto))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("future");
        }

        @Test
        @DisplayName("throws ValidationException for invalid confidence level")
        void throwsForInvalidConfidence() {
            Skill catalog = buildCatalogSkill(1L, "Java", true);
            when(skillRepository.findById(1L)).thenReturn(Optional.of(catalog));
            when(userSkillRepository.findByProfileIdAndSkillIdAndDeletedAtIsNull(PROFILE_ID, 1L))
                    .thenReturn(Optional.empty());

            AddUserSkillDto dto = new AddUserSkillDto(1L, 5, 3, null, "ULTRA", null, null);

            assertThatThrownBy(() -> userSkillService.addSkill(PROFILE_ID, dto))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("Confidence level");
        }

        @Test
        @DisplayName("throws ValidationException for invalid visibility")
        void throwsForInvalidVisibility() {
            Skill catalog = buildCatalogSkill(1L, "Java", true);
            when(skillRepository.findById(1L)).thenReturn(Optional.of(catalog));
            when(userSkillRepository.findByProfileIdAndSkillIdAndDeletedAtIsNull(PROFILE_ID, 1L))
                    .thenReturn(Optional.empty());

            AddUserSkillDto dto = new AddUserSkillDto(1L, 5, 3, null, null, null, "hidden");

            assertThatThrownBy(() -> userSkillService.addSkill(PROFILE_ID, dto))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("Visibility");
        }
    }

    @Nested
    @DisplayName("updateSkill")
    class UpdateSkill {

        @Test
        @DisplayName("updates skill with partial fields")
        void updatesPartialFields() {
            Skill catalog = buildCatalogSkill(1L, "Java", true);
            UserSkill existing = buildUserSkill(50L, PROFILE_ID, catalog);
            when(userSkillRepository.findByIdAndTenantIdAndDeletedAtIsNull(50L, TENANT_ID))
                    .thenReturn(Optional.of(existing));
            when(userSkillRepository.save(any(UserSkill.class))).thenReturn(existing);

            UpdateUserSkillDto dto = new UpdateUserSkillDto(10, null, null, "HIGH", null, null);

            UserSkillDto result = userSkillService.updateSkill(50L, dto);

            assertThat(existing.getYearsOfExperience()).isEqualTo(10);
            assertThat(existing.getConfidenceLevel()).isEqualTo("HIGH");
            assertThat(existing.getProficiencyDepth()).isEqualTo(3); // unchanged
        }

        @Test
        @DisplayName("throws SkillNotFoundException when user skill missing")
        void throwsWhenMissing() {
            when(userSkillRepository.findByIdAndTenantIdAndDeletedAtIsNull(99L, TENANT_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> userSkillService.updateSkill(99L,
                    new UpdateUserSkillDto(null, null, null, null, null, null)))
                    .isInstanceOf(SkillNotFoundException.class);
        }

        @Test
        @DisplayName("throws OptimisticLockException on concurrent modification")
        void throwsOnOptimisticLock() {
            Skill catalog = buildCatalogSkill(1L, "Java", true);
            UserSkill existing = buildUserSkill(50L, PROFILE_ID, catalog);
            when(userSkillRepository.findByIdAndTenantIdAndDeletedAtIsNull(50L, TENANT_ID))
                    .thenReturn(Optional.of(existing));
            when(userSkillRepository.save(any(UserSkill.class)))
                    .thenThrow(new OptimisticLockingFailureException("conflict"));

            assertThatThrownBy(() -> userSkillService.updateSkill(50L,
                    new UpdateUserSkillDto(10, null, null, null, null, null)))
                    .isInstanceOf(OptimisticLockException.class);
        }

        @Test
        @DisplayName("throws ValidationException for future lastUsedDate")
        void throwsForFutureDate() {
            Skill catalog = buildCatalogSkill(1L, "Java", true);
            UserSkill existing = buildUserSkill(50L, PROFILE_ID, catalog);
            when(userSkillRepository.findByIdAndTenantIdAndDeletedAtIsNull(50L, TENANT_ID))
                    .thenReturn(Optional.of(existing));

            assertThatThrownBy(() -> userSkillService.updateSkill(50L,
                    new UpdateUserSkillDto(null, null, LocalDate.now().plusDays(1), null, null, null)))
                    .isInstanceOf(ValidationException.class);
        }

        @Test
        @DisplayName("throws ValidationException for invalid confidence level")
        void throwsForBadConfidence() {
            Skill catalog = buildCatalogSkill(1L, "Java", true);
            UserSkill existing = buildUserSkill(50L, PROFILE_ID, catalog);
            when(userSkillRepository.findByIdAndTenantIdAndDeletedAtIsNull(50L, TENANT_ID))
                    .thenReturn(Optional.of(existing));

            assertThatThrownBy(() -> userSkillService.updateSkill(50L,
                    new UpdateUserSkillDto(null, null, null, "SUPER", null, null)))
                    .isInstanceOf(ValidationException.class);
        }
    }

    @Nested
    @DisplayName("deleteSkill")
    class DeleteSkill {

        @Test
        @DisplayName("soft deletes user skill")
        void softDeletesSkill() {
            Skill catalog = buildCatalogSkill(1L, "Java", true);
            UserSkill existing = buildUserSkill(50L, PROFILE_ID, catalog);
            when(userSkillRepository.findByIdAndTenantIdAndDeletedAtIsNull(50L, TENANT_ID))
                    .thenReturn(Optional.of(existing));
            when(userSkillRepository.save(any(UserSkill.class))).thenReturn(existing);

            userSkillService.deleteSkill(50L);

            assertThat(existing.getDeletedAt()).isNotNull();
            verify(userSkillRepository).save(existing);
        }

        @Test
        @DisplayName("throws SkillNotFoundException when user skill missing")
        void throwsWhenMissing() {
            when(userSkillRepository.findByIdAndTenantIdAndDeletedAtIsNull(99L, TENANT_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> userSkillService.deleteSkill(99L))
                    .isInstanceOf(SkillNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("getSkillsByProfile")
    class GetSkillsByProfile {

        @Test
        @DisplayName("returns skills grouped by category")
        void returnsGroupedByCategory() {
            Skill java = buildCatalogSkill(1L, "Java", true);
            java.setCategory("Language");
            Skill react = buildCatalogSkill(2L, "React", true);
            react.setCategory("Frontend");

            UserSkill us1 = buildUserSkill(50L, PROFILE_ID, java);
            UserSkill us2 = buildUserSkill(51L, PROFILE_ID, react);

            when(userSkillRepository.findByProfileIdAndTenantId(PROFILE_ID, TENANT_ID))
                    .thenReturn(List.of(us1, us2));

            Map<String, List<UserSkillDto>> result = userSkillService.getSkillsByProfile(PROFILE_ID);

            assertThat(result).containsKeys("Language", "Frontend");
            assertThat(result.get("Language")).hasSize(1);
            assertThat(result.get("Frontend")).hasSize(1);
        }
    }

    @Nested
    @DisplayName("getPublicSkillsByProfile")
    class GetPublicSkills {

        @Test
        @DisplayName("returns only public skills")
        void returnsPublicSkills() {
            Skill catalog = buildCatalogSkill(1L, "Java", true);
            UserSkill us = buildUserSkill(50L, PROFILE_ID, catalog);
            us.setVisibility("public");

            when(userSkillRepository.findByProfileIdAndVisibilityAndDeletedAtIsNull(PROFILE_ID, "public"))
                    .thenReturn(List.of(us));

            List<UserSkillDto> result = userSkillService.getPublicSkillsByProfile(PROFILE_ID);

            assertThat(result).hasSize(1);
        }
    }
}
