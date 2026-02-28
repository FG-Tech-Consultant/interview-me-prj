package com.interviewme.service;

import com.interviewme.billing.config.BillingProperties;
import com.interviewme.common.exception.PublicProfileNotFoundException;
import com.interviewme.dto.publicprofile.PublicProfileResponse;
import com.interviewme.dto.publicprofile.SlugCheckResponse;
import com.interviewme.model.*;
import com.interviewme.repository.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PublicProfileService")
class PublicProfileServiceTest {

    @Mock private ProfileRepository profileRepository;
    @Mock private JobExperienceRepository jobExperienceRepository;
    @Mock private EducationRepository educationRepository;
    @Mock private UserSkillRepository userSkillRepository;
    @Mock private ExperienceProjectRepository experienceProjectRepository;
    @Mock private StoryRepository storyRepository;
    @Mock private ExperienceProjectSkillRepository experienceProjectSkillRepository;
    @Mock private StorySkillRepository storySkillRepository;
    @Mock private BillingProperties billingProperties;

    @InjectMocks
    private PublicProfileService publicProfileService;

    private Profile buildProfile(Long id, String slug) {
        Profile p = new Profile();
        p.setId(id);
        p.setTenantId(100L);
        p.setSlug(slug);
        p.setFullName("John Doe");
        p.setHeadline("Senior Developer");
        p.setSummary("Experienced engineer");
        p.setLocation("NYC");
        p.setLanguages(List.of("English"));
        p.setProfessionalLinks(Map.of("github", "https://github.com/john"));
        p.setViewCount(10L);
        return p;
    }

    @Nested
    @DisplayName("getPublicProfile")
    class GetPublicProfile {

        @Test
        @DisplayName("returns assembled public profile with all sections")
        void returnsFullPublicProfile() {
            Profile profile = buildProfile(10L, "john-doe");
            when(profileRepository.findBySlugAndDeletedAtIsNull("john-doe")).thenReturn(Optional.of(profile));

            // Public jobs
            JobExperience job = new JobExperience();
            job.setId(1L);
            job.setProfileId(10L);
            job.setCompany("Acme");
            job.setRole("Dev");
            job.setStartDate(LocalDate.of(2020, 1, 1));
            job.setEndDate(LocalDate.of(2023, 1, 1));
            job.setIsCurrent(false);
            when(jobExperienceRepository.findByProfileIdAndVisibilityAndDeletedAtIsNull(10L, "public"))
                    .thenReturn(List.of(job));

            when(educationRepository.findByProfileIdAndVisibilityAndDeletedAtIsNull(10L, "public"))
                    .thenReturn(Collections.emptyList());

            // Public skills
            Skill catalogSkill = new Skill();
            catalogSkill.setId(1L);
            catalogSkill.setName("Java");
            catalogSkill.setCategory("Language");
            UserSkill userSkill = new UserSkill();
            userSkill.setId(50L);
            userSkill.setSkill(catalogSkill);
            userSkill.setProficiencyDepth(4);
            userSkill.setYearsOfExperience(5);
            when(userSkillRepository.findByProfileIdAndVisibilityAndDeletedAtIsNull(10L, "public"))
                    .thenReturn(List.of(userSkill));

            when(experienceProjectRepository.findByJobExperienceIdInAndVisibilityAndDeletedAtIsNull(anyList(), eq("public")))
                    .thenReturn(Collections.emptyList());

            // Save for view count increment
            when(profileRepository.findById(10L)).thenReturn(Optional.of(profile));
            when(profileRepository.save(any(Profile.class))).thenReturn(profile);

            PublicProfileResponse result = publicProfileService.getPublicProfile("john-doe");

            assertThat(result.slug()).isEqualTo("john-doe");
            assertThat(result.fullName()).isEqualTo("John Doe");
            assertThat(result.skills()).hasSize(1);
            assertThat(result.jobs()).hasSize(1);
            assertThat(result.seo()).isNotNull();
            assertThat(result.seo().canonicalUrl()).contains("john-doe");
        }

        @Test
        @DisplayName("throws PublicProfileNotFoundException when slug not found")
        void throwsWhenSlugNotFound() {
            when(profileRepository.findBySlugAndDeletedAtIsNull("nonexistent")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> publicProfileService.getPublicProfile("nonexistent"))
                    .isInstanceOf(PublicProfileNotFoundException.class);
        }

        @Test
        @DisplayName("increments view count on access")
        void incrementsViewCount() {
            Profile profile = buildProfile(10L, "john-doe");
            when(profileRepository.findBySlugAndDeletedAtIsNull("john-doe")).thenReturn(Optional.of(profile));
            when(jobExperienceRepository.findByProfileIdAndVisibilityAndDeletedAtIsNull(10L, "public"))
                    .thenReturn(Collections.emptyList());
            when(educationRepository.findByProfileIdAndVisibilityAndDeletedAtIsNull(10L, "public"))
                    .thenReturn(Collections.emptyList());
            when(userSkillRepository.findByProfileIdAndVisibilityAndDeletedAtIsNull(10L, "public"))
                    .thenReturn(Collections.emptyList());
            when(profileRepository.findById(10L)).thenReturn(Optional.of(profile));
            when(profileRepository.save(any(Profile.class))).thenReturn(profile);

            publicProfileService.getPublicProfile("john-doe");

            verify(profileRepository).save(argThat(p -> p.getViewCount() == 11L));
        }
    }

    @Nested
    @DisplayName("checkSlugAvailability")
    class CheckSlugAvailability {

        @Test
        @DisplayName("returns available for valid unused slug")
        void returnsAvailableForUnusedSlug() {
            Map<String, Integer> costs = Map.of("SLUG_CHANGE", 5);
            when(billingProperties.getCosts()).thenReturn(costs);
            when(profileRepository.existsBySlugGlobally("john-doe")).thenReturn(false);

            SlugCheckResponse result = publicProfileService.checkSlugAvailability("john-doe");

            assertThat(result.available()).isTrue();
            assertThat(result.slug()).isEqualTo("john-doe");
            assertThat(result.changeCost()).isEqualTo(5);
        }

        @Test
        @DisplayName("returns unavailable with suggestions for taken slug")
        void returnsUnavailableForTakenSlug() {
            Map<String, Integer> costs = Map.of("SLUG_CHANGE", 5);
            when(billingProperties.getCosts()).thenReturn(costs);
            when(profileRepository.existsBySlugGlobally("john-doe")).thenReturn(true);
            // Suggestions: john-doe-1, john-doe-2, john-doe-3
            when(profileRepository.existsBySlugGlobally("john-doe-1")).thenReturn(false);
            when(profileRepository.existsBySlugGlobally("john-doe-2")).thenReturn(false);
            when(profileRepository.existsBySlugGlobally("john-doe-3")).thenReturn(false);

            SlugCheckResponse result = publicProfileService.checkSlugAvailability("john-doe");

            assertThat(result.available()).isFalse();
            assertThat(result.suggestions()).isNotEmpty();
        }

        @Test
        @DisplayName("returns unavailable for invalid slug format")
        void returnsUnavailableForInvalidSlug() {
            Map<String, Integer> costs = Map.of("SLUG_CHANGE", 5);
            when(billingProperties.getCosts()).thenReturn(costs);

            SlugCheckResponse result = publicProfileService.checkSlugAvailability("AB");

            assertThat(result.available()).isFalse();
            assertThat(result.suggestions()).isEmpty();
        }

        @Test
        @DisplayName("returns unavailable for reserved slug")
        void returnsUnavailableForReservedSlug() {
            Map<String, Integer> costs = Map.of("SLUG_CHANGE", 5);
            when(billingProperties.getCosts()).thenReturn(costs);

            SlugCheckResponse result = publicProfileService.checkSlugAvailability("admin");

            assertThat(result.available()).isFalse();
        }
    }

    @Nested
    @DisplayName("incrementViewCount")
    class IncrementViewCount {

        @Test
        @DisplayName("increments and saves view count")
        void incrementsViewCount() {
            Profile profile = buildProfile(10L, "john-doe");
            when(profileRepository.findById(10L)).thenReturn(Optional.of(profile));
            when(profileRepository.save(any(Profile.class))).thenReturn(profile);

            publicProfileService.incrementViewCount(10L);

            verify(profileRepository).save(argThat(p -> p.getViewCount() == 11L));
        }

        @Test
        @DisplayName("does not throw when profile not found")
        void doesNotThrowWhenMissing() {
            when(profileRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatCode(() -> publicProfileService.incrementViewCount(99L))
                    .doesNotThrowAnyException();
        }
    }
}
