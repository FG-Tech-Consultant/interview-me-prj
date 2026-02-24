package com.interviewme.profile.service;

import com.interviewme.common.exception.ProfileNotFoundException;
import com.interviewme.common.exception.ValidationException;
import com.interviewme.profile.dto.jobexperience.CreateJobExperienceRequest;
import com.interviewme.profile.dto.jobexperience.JobExperienceResponse;
import com.interviewme.profile.dto.jobexperience.UpdateJobExperienceRequest;
import com.interviewme.profile.dto.profile.CreateProfileRequest;
import com.interviewme.profile.dto.profile.ProfileResponse;
import com.interviewme.profile.repository.JobExperienceRepository;
import com.interviewme.profile.repository.ProfileRepository;
import com.interviewme.security.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class JobExperienceServiceTest {

    @Autowired
    private JobExperienceService jobExperienceService;

    @Autowired
    private ProfileService profileService;

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private JobExperienceRepository jobExperienceRepository;

    private static final Long TENANT_ID = 1L;
    private static final Long USER_ID = 200L;

    private Long profileId;

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(TENANT_ID);

        // Create a profile for testing
        CreateProfileRequest profileRequest = new CreateProfileRequest(
                "Test User", "Developer", null, null, null, null, null, null
        );
        ProfileResponse profile = profileService.createProfile(USER_ID, profileRequest);
        profileId = profile.id();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void createJobExperience_Success() {
        // Given
        CreateJobExperienceRequest request = new CreateJobExperienceRequest(
                "Acme Corp",
                "Software Engineer",
                LocalDate.of(2020, 1, 1),
                LocalDate.of(2023, 6, 30),
                false,
                "San Francisco",
                "Full-time",
                "Developed features",
                "Improved performance by 40%",
                null,
                null
        );

        // When
        JobExperienceResponse response = jobExperienceService.createJobExperience(profileId, request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.id()).isNotNull();
        assertThat(response.company()).isEqualTo("Acme Corp");
        assertThat(response.role()).isEqualTo("Software Engineer");
        assertThat(response.startDate()).isEqualTo(LocalDate.of(2020, 1, 1));
        assertThat(response.endDate()).isEqualTo(LocalDate.of(2023, 6, 30));
        assertThat(response.tenantId()).isEqualTo(TENANT_ID);
    }

    @Test
    void createJobExperience_InvalidDates_ThrowsException() {
        // Given - end date before start date
        CreateJobExperienceRequest request = new CreateJobExperienceRequest(
                "Acme Corp",
                "Software Engineer",
                LocalDate.of(2023, 6, 30),
                LocalDate.of(2020, 1, 1),
                false,
                null,
                null,
                null,
                null,
                null,
                null
        );

        // When/Then
        assertThatThrownBy(() -> jobExperienceService.createJobExperience(profileId, request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("End date cannot be before start date");
    }

    @Test
    void createJobExperience_ProfileNotFound_ThrowsException() {
        // Given
        CreateJobExperienceRequest request = new CreateJobExperienceRequest(
                "Acme Corp",
                "Software Engineer",
                LocalDate.of(2020, 1, 1),
                null,
                true,
                null,
                null,
                null,
                null,
                null,
                null
        );

        // When/Then
        assertThatThrownBy(() -> jobExperienceService.createJobExperience(999L, request))
                .isInstanceOf(ProfileNotFoundException.class);
    }

    @Test
    void getJobExperiencesByProfileId_Success() {
        // Given
        CreateJobExperienceRequest request1 = new CreateJobExperienceRequest(
                "Company A", "Role A",
                LocalDate.of(2018, 1, 1), LocalDate.of(2020, 1, 1),
                false, null, null, null, null, null, null
        );
        CreateJobExperienceRequest request2 = new CreateJobExperienceRequest(
                "Company B", "Role B",
                LocalDate.of(2020, 2, 1), null,
                true, null, null, null, null, null, null
        );
        jobExperienceService.createJobExperience(profileId, request1);
        jobExperienceService.createJobExperience(profileId, request2);

        // When
        List<JobExperienceResponse> experiences = jobExperienceService.getJobExperiencesByProfileId(profileId);

        // Then
        assertThat(experiences).hasSize(2);
        // Ordered by start_date DESC, so Company B first
        assertThat(experiences.get(0).company()).isEqualTo("Company B");
        assertThat(experiences.get(1).company()).isEqualTo("Company A");
    }

    @Test
    void updateJobExperience_Success() {
        // Given
        CreateJobExperienceRequest createRequest = new CreateJobExperienceRequest(
                "Old Company", "Old Role",
                LocalDate.of(2020, 1, 1), LocalDate.of(2022, 12, 31),
                false, null, null, null, null, null, null
        );
        JobExperienceResponse created = jobExperienceService.createJobExperience(profileId, createRequest);

        UpdateJobExperienceRequest updateRequest = new UpdateJobExperienceRequest(
                "New Company", "New Role",
                LocalDate.of(2020, 1, 1), LocalDate.of(2023, 6, 30),
                false, "New York", "Full-time", null, null, null, null,
                created.version()
        );

        // When
        JobExperienceResponse updated = jobExperienceService.updateJobExperience(profileId, created.id(), updateRequest);

        // Then
        assertThat(updated.company()).isEqualTo("New Company");
        assertThat(updated.role()).isEqualTo("New Role");
        assertThat(updated.location()).isEqualTo("New York");
        assertThat(updated.version()).isEqualTo(created.version() + 1);
    }

    @Test
    void deleteJobExperience_Success() {
        // Given
        CreateJobExperienceRequest createRequest = new CreateJobExperienceRequest(
                "Delete Corp", "Delete Role",
                LocalDate.of(2020, 1, 1), LocalDate.of(2022, 12, 31),
                false, null, null, null, null, null, null
        );
        JobExperienceResponse created = jobExperienceService.createJobExperience(profileId, createRequest);

        // When
        jobExperienceService.deleteJobExperience(profileId, created.id());

        // Then - should not appear in list
        List<JobExperienceResponse> experiences = jobExperienceService.getJobExperiencesByProfileId(profileId);
        assertThat(experiences).isEmpty();
    }

    @Test
    void deleteJobExperience_NotFound_ThrowsException() {
        // When/Then
        assertThatThrownBy(() -> jobExperienceService.deleteJobExperience(profileId, 999L))
                .isInstanceOf(ValidationException.class);
    }
}
