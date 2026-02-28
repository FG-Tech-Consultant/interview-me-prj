package com.interviewme.mapper;

import com.interviewme.dto.profile.CreateProfileRequest;
import com.interviewme.dto.profile.ProfileResponse;
import com.interviewme.dto.profile.UpdateProfileRequest;
import com.interviewme.model.Education;
import com.interviewme.model.JobExperience;
import com.interviewme.model.Profile;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class ProfileMapperTest {

    @Nested
    @DisplayName("toEntity")
    class ToEntity {

        @Test
        @DisplayName("should map all fields from CreateProfileRequest")
        void shouldMapAllFields() {
            CreateProfileRequest request = new CreateProfileRequest(
                    "John Doe", "Software Engineer", "Summary text",
                    "New York", List.of("English", "Spanish"),
                    Map.of("github", "https://github.com/john"),
                    Map.of("remote", true), "public"
            );

            Profile entity = ProfileMapper.toEntity(request);

            assertThat(entity.getFullName()).isEqualTo("John Doe");
            assertThat(entity.getHeadline()).isEqualTo("Software Engineer");
            assertThat(entity.getSummary()).isEqualTo("Summary text");
            assertThat(entity.getLocation()).isEqualTo("New York");
            assertThat(entity.getLanguages()).containsExactly("English", "Spanish");
            assertThat(entity.getProfessionalLinks()).containsEntry("github", "https://github.com/john");
            assertThat(entity.getCareerPreferences()).containsEntry("remote", true);
            assertThat(entity.getDefaultVisibility()).isEqualTo("public");
        }

        @Test
        @DisplayName("should default visibility to private when null")
        void shouldDefaultVisibilityToPrivate() {
            CreateProfileRequest request = new CreateProfileRequest(
                    "John Doe", "Engineer", null, null, null, null, null, null
            );

            Profile entity = ProfileMapper.toEntity(request);

            assertThat(entity.getDefaultVisibility()).isEqualTo("private");
        }

        @Test
        @DisplayName("should handle null optional fields")
        void shouldHandleNullOptionalFields() {
            CreateProfileRequest request = new CreateProfileRequest(
                    "John Doe", "Engineer", null, null, null, null, null, "public"
            );

            Profile entity = ProfileMapper.toEntity(request);

            assertThat(entity.getSummary()).isNull();
            assertThat(entity.getLocation()).isNull();
            assertThat(entity.getLanguages()).isNull();
            assertThat(entity.getProfessionalLinks()).isNull();
            assertThat(entity.getCareerPreferences()).isNull();
        }
    }

    @Nested
    @DisplayName("toResponse")
    class ToResponse {

        @Test
        @DisplayName("should map all fields from Profile entity")
        void shouldMapAllFields() {
            Profile entity = createTestProfile();

            ProfileResponse response = ProfileMapper.toResponse(entity);

            assertThat(response.id()).isEqualTo(1L);
            assertThat(response.tenantId()).isEqualTo(10L);
            assertThat(response.userId()).isEqualTo(100L);
            assertThat(response.fullName()).isEqualTo("John Doe");
            assertThat(response.headline()).isEqualTo("Engineer");
            assertThat(response.slug()).isEqualTo("john-doe");
            assertThat(response.slugChangeCount()).isEqualTo(0);
            assertThat(response.viewCount()).isEqualTo(5L);
            assertThat(response.defaultVisibility()).isEqualTo("public");
        }

        @Test
        @DisplayName("should return empty list when job experiences are null")
        void shouldReturnEmptyListForNullJobs() {
            Profile entity = createTestProfile();
            entity.setJobExperiences(null);

            ProfileResponse response = ProfileMapper.toResponse(entity);

            assertThat(response.jobs()).isEmpty();
        }

        @Test
        @DisplayName("should return empty list when education is null")
        void shouldReturnEmptyListForNullEducation() {
            Profile entity = createTestProfile();
            entity.setEducation(null);

            ProfileResponse response = ProfileMapper.toResponse(entity);

            assertThat(response.education()).isEmpty();
        }

        @Test
        @DisplayName("should filter out soft-deleted job experiences")
        void shouldFilterDeletedJobs() {
            Profile entity = createTestProfile();
            JobExperience activeJob = new JobExperience();
            activeJob.setId(1L);
            activeJob.setCompany("Active Corp");
            activeJob.setRole("Engineer");
            activeJob.setStartDate(LocalDate.of(2020, 1, 1));
            activeJob.setDeletedAt(null);

            JobExperience deletedJob = new JobExperience();
            deletedJob.setId(2L);
            deletedJob.setCompany("Deleted Corp");
            deletedJob.setRole("Manager");
            deletedJob.setStartDate(LocalDate.of(2019, 1, 1));
            deletedJob.setDeletedAt(Instant.now());

            entity.setJobExperiences(List.of(activeJob, deletedJob));

            ProfileResponse response = ProfileMapper.toResponse(entity);

            assertThat(response.jobs()).hasSize(1);
            assertThat(response.jobs().get(0).company()).isEqualTo("Active Corp");
        }

        @Test
        @DisplayName("should filter out soft-deleted education entries")
        void shouldFilterDeletedEducation() {
            Profile entity = createTestProfile();
            Education active = new Education();
            active.setId(1L);
            active.setDegree("BS");
            active.setInstitution("MIT");
            active.setEndDate(LocalDate.of(2020, 6, 1));
            active.setDeletedAt(null);

            Education deleted = new Education();
            deleted.setId(2L);
            deleted.setDegree("MS");
            deleted.setInstitution("Stanford");
            deleted.setEndDate(LocalDate.of(2022, 6, 1));
            deleted.setDeletedAt(Instant.now());

            entity.setEducation(List.of(active, deleted));

            ProfileResponse response = ProfileMapper.toResponse(entity);

            assertThat(response.education()).hasSize(1);
            assertThat(response.education().get(0).institution()).isEqualTo("MIT");
        }
    }

    @Nested
    @DisplayName("updateEntity")
    class UpdateEntity {

        @Test
        @DisplayName("should update all fields")
        void shouldUpdateAllFields() {
            Profile entity = createTestProfile();
            UpdateProfileRequest request = new UpdateProfileRequest(
                    "Jane Doe", "Senior Engineer", "New summary",
                    "San Francisco", List.of("French"),
                    Map.of("linkedin", "https://linkedin.com/in/jane"),
                    Map.of("remote", false), "private", 0L
            );

            ProfileMapper.updateEntity(entity, request);

            assertThat(entity.getFullName()).isEqualTo("Jane Doe");
            assertThat(entity.getHeadline()).isEqualTo("Senior Engineer");
            assertThat(entity.getSummary()).isEqualTo("New summary");
            assertThat(entity.getLocation()).isEqualTo("San Francisco");
            assertThat(entity.getLanguages()).containsExactly("French");
            assertThat(entity.getDefaultVisibility()).isEqualTo("private");
        }

        @Test
        @DisplayName("should not update visibility when null in request")
        void shouldNotUpdateVisibilityWhenNull() {
            Profile entity = createTestProfile();
            entity.setDefaultVisibility("public");
            UpdateProfileRequest request = new UpdateProfileRequest(
                    "John Doe", "Engineer", null, null, null, null, null, null, 0L
            );

            ProfileMapper.updateEntity(entity, request);

            assertThat(entity.getDefaultVisibility()).isEqualTo("public");
        }
    }

    private Profile createTestProfile() {
        Profile profile = new Profile();
        profile.setId(1L);
        profile.setTenantId(10L);
        profile.setUserId(100L);
        profile.setFullName("John Doe");
        profile.setHeadline("Engineer");
        profile.setSummary("A summary");
        profile.setLocation("New York");
        profile.setLanguages(List.of("English"));
        profile.setProfessionalLinks(Map.of("github", "https://github.com/john"));
        profile.setCareerPreferences(Map.of("remote", true));
        profile.setDefaultVisibility("public");
        profile.setSlug("john-doe");
        profile.setSlugChangeCount(0);
        profile.setViewCount(5L);
        profile.setCreatedAt(Instant.now());
        profile.setUpdatedAt(Instant.now());
        profile.setVersion(0L);
        profile.setJobExperiences(Collections.emptyList());
        profile.setEducation(Collections.emptyList());
        return profile;
    }
}
