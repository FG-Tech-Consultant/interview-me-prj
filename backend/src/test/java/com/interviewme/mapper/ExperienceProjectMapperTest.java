package com.interviewme.mapper;

import com.interviewme.dto.experience.CreateProjectRequest;
import com.interviewme.dto.experience.ProjectResponse;
import com.interviewme.dto.experience.UpdateProjectRequest;
import com.interviewme.model.ExperienceProject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class ExperienceProjectMapperTest {

    @Nested
    @DisplayName("toEntity")
    class ToEntity {

        @Test
        @DisplayName("should map all fields from CreateProjectRequest")
        void shouldMapAllFields() {
            CreateProjectRequest request = new CreateProjectRequest(
                    "Payment Gateway", "Built payment system",
                    "Tech Lead", 5, List.of("Java", "Spring"),
                    "Microservices", Map.of("latency", "50ms"),
                    "Reduced processing time by 40%", "public"
            );

            ExperienceProject entity = ExperienceProjectMapper.toEntity(request);

            assertThat(entity.getTitle()).isEqualTo("Payment Gateway");
            assertThat(entity.getContext()).isEqualTo("Built payment system");
            assertThat(entity.getRole()).isEqualTo("Tech Lead");
            assertThat(entity.getTeamSize()).isEqualTo(5);
            assertThat(entity.getTechStack()).containsExactly("Java", "Spring");
            assertThat(entity.getArchitectureType()).isEqualTo("Microservices");
            assertThat(entity.getMetrics()).containsEntry("latency", "50ms");
            assertThat(entity.getOutcomes()).isEqualTo("Reduced processing time by 40%");
            assertThat(entity.getVisibility()).isEqualTo("public");
        }

        @Test
        @DisplayName("should default visibility to private when null")
        void shouldDefaultVisibilityToPrivate() {
            CreateProjectRequest request = new CreateProjectRequest(
                    "Project", null, null, null, null, null, null, null, null
            );

            ExperienceProject entity = ExperienceProjectMapper.toEntity(request);

            assertThat(entity.getVisibility()).isEqualTo("private");
        }
    }

    @Nested
    @DisplayName("toResponse")
    class ToResponse {

        @Test
        @DisplayName("should map all fields including story count")
        void shouldMapAllFieldsWithStoryCount() {
            ExperienceProject entity = createTestProject();

            ProjectResponse response = ExperienceProjectMapper.toResponse(entity, 3);

            assertThat(response.id()).isEqualTo(1L);
            assertThat(response.jobExperienceId()).isEqualTo(10L);
            assertThat(response.title()).isEqualTo("Payment Gateway");
            assertThat(response.context()).isEqualTo("Built payment system");
            assertThat(response.role()).isEqualTo("Tech Lead");
            assertThat(response.teamSize()).isEqualTo(5);
            assertThat(response.techStack()).containsExactly("Java", "Spring");
            assertThat(response.storyCount()).isEqualTo(3);
            assertThat(response.visibility()).isEqualTo("public");
        }

        @Test
        @DisplayName("should handle zero story count")
        void shouldHandleZeroStoryCount() {
            ExperienceProject entity = createTestProject();

            ProjectResponse response = ExperienceProjectMapper.toResponse(entity, 0);

            assertThat(response.storyCount()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("updateEntity")
    class UpdateEntity {

        @Test
        @DisplayName("should update all fields")
        void shouldUpdateAllFields() {
            ExperienceProject entity = createTestProject();
            UpdateProjectRequest request = new UpdateProjectRequest(
                    "New Title", "New context", "Senior Lead",
                    10, List.of("Python", "Django"), "Monolith",
                    Map.of("uptime", "99.99%"), "New outcomes", "private", 0L
            );

            ExperienceProjectMapper.updateEntity(entity, request);

            assertThat(entity.getTitle()).isEqualTo("New Title");
            assertThat(entity.getContext()).isEqualTo("New context");
            assertThat(entity.getRole()).isEqualTo("Senior Lead");
            assertThat(entity.getTeamSize()).isEqualTo(10);
            assertThat(entity.getTechStack()).containsExactly("Python", "Django");
            assertThat(entity.getVisibility()).isEqualTo("private");
        }

        @Test
        @DisplayName("should not update visibility when null")
        void shouldNotUpdateVisibilityWhenNull() {
            ExperienceProject entity = createTestProject();
            entity.setVisibility("public");
            UpdateProjectRequest request = new UpdateProjectRequest(
                    "Title", null, null, null, null, null, null, null, null, 0L
            );

            ExperienceProjectMapper.updateEntity(entity, request);

            assertThat(entity.getVisibility()).isEqualTo("public");
        }
    }

    private ExperienceProject createTestProject() {
        ExperienceProject project = new ExperienceProject();
        project.setId(1L);
        project.setTenantId(10L);
        project.setJobExperienceId(10L);
        project.setTitle("Payment Gateway");
        project.setContext("Built payment system");
        project.setRole("Tech Lead");
        project.setTeamSize(5);
        project.setTechStack(List.of("Java", "Spring"));
        project.setArchitectureType("Microservices");
        project.setMetrics(Map.of("latency", "50ms"));
        project.setOutcomes("Reduced processing time by 40%");
        project.setVisibility("public");
        project.setCreatedAt(Instant.now());
        project.setUpdatedAt(Instant.now());
        project.setVersion(0L);
        return project;
    }
}
