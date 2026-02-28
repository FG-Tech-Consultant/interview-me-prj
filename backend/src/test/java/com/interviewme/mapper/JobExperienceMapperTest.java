package com.interviewme.mapper;

import com.interviewme.dto.job.CreateJobExperienceRequest;
import com.interviewme.dto.job.JobExperienceResponse;
import com.interviewme.dto.job.UpdateJobExperienceRequest;
import com.interviewme.model.JobExperience;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class JobExperienceMapperTest {

    @Nested
    @DisplayName("toEntity")
    class ToEntity {

        @Test
        @DisplayName("should map all fields from CreateJobExperienceRequest")
        void shouldMapAllFields() {
            CreateJobExperienceRequest request = new CreateJobExperienceRequest(
                    "Acme Corp", "Software Engineer",
                    LocalDate.of(2020, 1, 1), LocalDate.of(2023, 6, 30),
                    false, "Remote", "Full-time",
                    "Built microservices", "Increased uptime by 99.9%",
                    Map.of("uptime", 99.9), "public"
            );

            JobExperience entity = JobExperienceMapper.toEntity(request);

            assertThat(entity.getCompany()).isEqualTo("Acme Corp");
            assertThat(entity.getRole()).isEqualTo("Software Engineer");
            assertThat(entity.getStartDate()).isEqualTo(LocalDate.of(2020, 1, 1));
            assertThat(entity.getEndDate()).isEqualTo(LocalDate.of(2023, 6, 30));
            assertThat(entity.getIsCurrent()).isFalse();
            assertThat(entity.getLocation()).isEqualTo("Remote");
            assertThat(entity.getEmploymentType()).isEqualTo("Full-time");
            assertThat(entity.getResponsibilities()).isEqualTo("Built microservices");
            assertThat(entity.getAchievements()).isEqualTo("Increased uptime by 99.9%");
            assertThat(entity.getMetrics()).containsEntry("uptime", 99.9);
            assertThat(entity.getVisibility()).isEqualTo("public");
        }

        @Test
        @DisplayName("should default isCurrent to false when null")
        void shouldDefaultIsCurrentToFalse() {
            CreateJobExperienceRequest request = new CreateJobExperienceRequest(
                    "Acme Corp", "Engineer", LocalDate.now(), null,
                    null, null, null, null, null, null, null
            );

            JobExperience entity = JobExperienceMapper.toEntity(request);

            assertThat(entity.getIsCurrent()).isFalse();
        }

        @Test
        @DisplayName("should default visibility to private when null")
        void shouldDefaultVisibilityToPrivate() {
            CreateJobExperienceRequest request = new CreateJobExperienceRequest(
                    "Acme Corp", "Engineer", LocalDate.now(), null,
                    null, null, null, null, null, null, null
            );

            JobExperience entity = JobExperienceMapper.toEntity(request);

            assertThat(entity.getVisibility()).isEqualTo("private");
        }
    }

    @Nested
    @DisplayName("toResponse")
    class ToResponse {

        @Test
        @DisplayName("should map all fields from entity")
        void shouldMapAllFields() {
            JobExperience entity = createTestJobExperience();

            JobExperienceResponse response = JobExperienceMapper.toResponse(entity);

            assertThat(response.id()).isEqualTo(1L);
            assertThat(response.tenantId()).isEqualTo(10L);
            assertThat(response.profileId()).isEqualTo(100L);
            assertThat(response.company()).isEqualTo("Acme Corp");
            assertThat(response.role()).isEqualTo("Engineer");
            assertThat(response.startDate()).isEqualTo(LocalDate.of(2020, 1, 1));
            assertThat(response.endDate()).isEqualTo(LocalDate.of(2023, 6, 30));
            assertThat(response.isCurrent()).isFalse();
            assertThat(response.visibility()).isEqualTo("public");
        }
    }

    @Nested
    @DisplayName("updateEntity")
    class UpdateEntity {

        @Test
        @DisplayName("should update all fields")
        void shouldUpdateAllFields() {
            JobExperience entity = createTestJobExperience();
            UpdateJobExperienceRequest request = new UpdateJobExperienceRequest(
                    "New Corp", "Senior Engineer",
                    LocalDate.of(2021, 1, 1), null,
                    true, "On-site", "Contract",
                    "New responsibilities", "New achievements",
                    Map.of("revenue", 1000000), "private", 0L
            );

            JobExperienceMapper.updateEntity(entity, request);

            assertThat(entity.getCompany()).isEqualTo("New Corp");
            assertThat(entity.getRole()).isEqualTo("Senior Engineer");
            assertThat(entity.getIsCurrent()).isTrue();
            assertThat(entity.getLocation()).isEqualTo("On-site");
            assertThat(entity.getVisibility()).isEqualTo("private");
        }

        @Test
        @DisplayName("should default isCurrent to false when null")
        void shouldDefaultIsCurrentWhenNull() {
            JobExperience entity = createTestJobExperience();
            entity.setIsCurrent(true);
            UpdateJobExperienceRequest request = new UpdateJobExperienceRequest(
                    "Corp", "Eng", LocalDate.now(), null,
                    null, null, null, null, null, null, null, 0L
            );

            JobExperienceMapper.updateEntity(entity, request);

            assertThat(entity.getIsCurrent()).isFalse();
        }

        @Test
        @DisplayName("should not update visibility when null")
        void shouldNotUpdateVisibilityWhenNull() {
            JobExperience entity = createTestJobExperience();
            entity.setVisibility("public");
            UpdateJobExperienceRequest request = new UpdateJobExperienceRequest(
                    "Corp", "Eng", LocalDate.now(), null,
                    false, null, null, null, null, null, null, 0L
            );

            JobExperienceMapper.updateEntity(entity, request);

            assertThat(entity.getVisibility()).isEqualTo("public");
        }
    }

    private JobExperience createTestJobExperience() {
        JobExperience job = new JobExperience();
        job.setId(1L);
        job.setTenantId(10L);
        job.setProfileId(100L);
        job.setCompany("Acme Corp");
        job.setRole("Engineer");
        job.setStartDate(LocalDate.of(2020, 1, 1));
        job.setEndDate(LocalDate.of(2023, 6, 30));
        job.setIsCurrent(false);
        job.setLocation("Remote");
        job.setEmploymentType("Full-time");
        job.setResponsibilities("Building things");
        job.setAchievements("Built things");
        job.setMetrics(Map.of("uptime", 99.9));
        job.setVisibility("public");
        job.setCreatedAt(Instant.now());
        job.setUpdatedAt(Instant.now());
        job.setVersion(0L);
        return job;
    }
}
