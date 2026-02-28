package com.interviewme.mapper;

import com.interviewme.dto.education.CreateEducationRequest;
import com.interviewme.dto.education.EducationResponse;
import com.interviewme.dto.education.UpdateEducationRequest;
import com.interviewme.model.Education;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

class EducationMapperTest {

    @Nested
    @DisplayName("toEntity")
    class ToEntity {

        @Test
        @DisplayName("should map all fields from CreateEducationRequest")
        void shouldMapAllFields() {
            CreateEducationRequest request = new CreateEducationRequest(
                    "Bachelor of Science", "MIT",
                    LocalDate.of(2016, 9, 1), LocalDate.of(2020, 6, 1),
                    "Computer Science", "3.9", "Dean's list", "public"
            );

            Education entity = EducationMapper.toEntity(request);

            assertThat(entity.getDegree()).isEqualTo("Bachelor of Science");
            assertThat(entity.getInstitution()).isEqualTo("MIT");
            assertThat(entity.getStartDate()).isEqualTo(LocalDate.of(2016, 9, 1));
            assertThat(entity.getEndDate()).isEqualTo(LocalDate.of(2020, 6, 1));
            assertThat(entity.getFieldOfStudy()).isEqualTo("Computer Science");
            assertThat(entity.getGpa()).isEqualTo("3.9");
            assertThat(entity.getNotes()).isEqualTo("Dean's list");
            assertThat(entity.getVisibility()).isEqualTo("public");
        }

        @Test
        @DisplayName("should default visibility to private when null")
        void shouldDefaultVisibilityToPrivate() {
            CreateEducationRequest request = new CreateEducationRequest(
                    "BS", "MIT", null, LocalDate.now(),
                    null, null, null, null
            );

            Education entity = EducationMapper.toEntity(request);

            assertThat(entity.getVisibility()).isEqualTo("private");
        }

        @Test
        @DisplayName("should handle null optional fields")
        void shouldHandleNullOptionalFields() {
            CreateEducationRequest request = new CreateEducationRequest(
                    "BS", "MIT", null, LocalDate.now(),
                    null, null, null, "public"
            );

            Education entity = EducationMapper.toEntity(request);

            assertThat(entity.getStartDate()).isNull();
            assertThat(entity.getFieldOfStudy()).isNull();
            assertThat(entity.getGpa()).isNull();
            assertThat(entity.getNotes()).isNull();
        }
    }

    @Nested
    @DisplayName("toResponse")
    class ToResponse {

        @Test
        @DisplayName("should map all fields from entity")
        void shouldMapAllFields() {
            Education entity = createTestEducation();

            EducationResponse response = EducationMapper.toResponse(entity);

            assertThat(response.id()).isEqualTo(1L);
            assertThat(response.tenantId()).isEqualTo(10L);
            assertThat(response.profileId()).isEqualTo(100L);
            assertThat(response.degree()).isEqualTo("Bachelor of Science");
            assertThat(response.institution()).isEqualTo("MIT");
            assertThat(response.fieldOfStudy()).isEqualTo("Computer Science");
            assertThat(response.gpa()).isEqualTo("3.9");
            assertThat(response.visibility()).isEqualTo("public");
        }
    }

    @Nested
    @DisplayName("updateEntity")
    class UpdateEntity {

        @Test
        @DisplayName("should update all fields")
        void shouldUpdateAllFields() {
            Education entity = createTestEducation();
            UpdateEducationRequest request = new UpdateEducationRequest(
                    "Master of Science", "Stanford",
                    LocalDate.of(2020, 9, 1), LocalDate.of(2022, 6, 1),
                    "AI/ML", "4.0", "Research assistant", "private", 0L
            );

            EducationMapper.updateEntity(entity, request);

            assertThat(entity.getDegree()).isEqualTo("Master of Science");
            assertThat(entity.getInstitution()).isEqualTo("Stanford");
            assertThat(entity.getFieldOfStudy()).isEqualTo("AI/ML");
            assertThat(entity.getGpa()).isEqualTo("4.0");
            assertThat(entity.getVisibility()).isEqualTo("private");
        }

        @Test
        @DisplayName("should not update visibility when null")
        void shouldNotUpdateVisibilityWhenNull() {
            Education entity = createTestEducation();
            entity.setVisibility("public");
            UpdateEducationRequest request = new UpdateEducationRequest(
                    "BS", "MIT", null, LocalDate.now(),
                    null, null, null, null, 0L
            );

            EducationMapper.updateEntity(entity, request);

            assertThat(entity.getVisibility()).isEqualTo("public");
        }
    }

    private Education createTestEducation() {
        Education edu = new Education();
        edu.setId(1L);
        edu.setTenantId(10L);
        edu.setProfileId(100L);
        edu.setDegree("Bachelor of Science");
        edu.setInstitution("MIT");
        edu.setStartDate(LocalDate.of(2016, 9, 1));
        edu.setEndDate(LocalDate.of(2020, 6, 1));
        edu.setFieldOfStudy("Computer Science");
        edu.setGpa("3.9");
        edu.setNotes("Dean's list");
        edu.setVisibility("public");
        edu.setCreatedAt(Instant.now());
        edu.setUpdatedAt(Instant.now());
        edu.setVersion(0L);
        return edu;
    }
}
