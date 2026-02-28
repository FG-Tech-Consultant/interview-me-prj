package com.interviewme.service;

import com.interviewme.common.exception.ProfileNotFoundException;
import com.interviewme.common.exception.ValidationException;
import com.interviewme.dto.education.CreateEducationRequest;
import com.interviewme.dto.education.EducationResponse;
import com.interviewme.dto.education.UpdateEducationRequest;
import com.interviewme.dto.profile.CreateProfileRequest;
import com.interviewme.dto.profile.ProfileResponse;
import com.interviewme.repository.EducationRepository;
import com.interviewme.repository.ProfileRepository;
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
class EducationServiceTest {

    @Autowired
    private EducationService educationService;

    @Autowired
    private ProfileService profileService;

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private EducationRepository educationRepository;

    private static final Long TENANT_ID = 1L;
    private static final Long USER_ID = 300L;

    private Long profileId;

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(TENANT_ID);

        CreateProfileRequest profileRequest = new CreateProfileRequest(
                "Test Student", "Student", null, null, null, null, null, null
        );
        ProfileResponse profile = profileService.createProfile(USER_ID, profileRequest);
        profileId = profile.id();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void createEducation_Success() {
        CreateEducationRequest request = new CreateEducationRequest(
                "Bachelor of Science",
                "MIT",
                LocalDate.of(2016, 9, 1),
                LocalDate.of(2020, 6, 15),
                "Computer Science",
                "3.9",
                "Graduated with honors",
                null
        );

        EducationResponse response = educationService.createEducation(profileId, request);

        assertThat(response).isNotNull();
        assertThat(response.id()).isNotNull();
        assertThat(response.degree()).isEqualTo("Bachelor of Science");
        assertThat(response.institution()).isEqualTo("MIT");
        assertThat(response.fieldOfStudy()).isEqualTo("Computer Science");
        assertThat(response.gpa()).isEqualTo("3.9");
        assertThat(response.tenantId()).isEqualTo(TENANT_ID);
    }

    @Test
    void createEducation_InvalidDates_ThrowsException() {
        CreateEducationRequest request = new CreateEducationRequest(
                "Bachelor of Science",
                "MIT",
                LocalDate.of(2020, 6, 15),
                LocalDate.of(2016, 9, 1),
                null,
                null,
                null,
                null
        );

        assertThatThrownBy(() -> educationService.createEducation(profileId, request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("End date cannot be before start date");
    }

    @Test
    void createEducation_ProfileNotFound_ThrowsException() {
        CreateEducationRequest request = new CreateEducationRequest(
                "Bachelor of Science",
                "MIT",
                null,
                LocalDate.of(2020, 6, 15),
                null,
                null,
                null,
                null
        );

        assertThatThrownBy(() -> educationService.createEducation(999L, request))
                .isInstanceOf(ProfileNotFoundException.class);
    }

    @Test
    void getEducationsByProfileId_Success() {
        CreateEducationRequest request1 = new CreateEducationRequest(
                "Bachelor", "University A",
                LocalDate.of(2012, 9, 1), LocalDate.of(2016, 6, 15),
                null, null, null, null
        );
        CreateEducationRequest request2 = new CreateEducationRequest(
                "Master", "University B",
                LocalDate.of(2016, 9, 1), LocalDate.of(2018, 6, 15),
                null, null, null, null
        );
        educationService.createEducation(profileId, request1);
        educationService.createEducation(profileId, request2);

        List<EducationResponse> educations = educationService.getEducationsByProfileId(profileId);

        assertThat(educations).hasSize(2);
    }

    @Test
    void updateEducation_Success() {
        CreateEducationRequest createRequest = new CreateEducationRequest(
                "Old Degree", "Old Institution",
                LocalDate.of(2016, 9, 1), LocalDate.of(2020, 6, 15),
                null, null, null, null
        );
        EducationResponse created = educationService.createEducation(profileId, createRequest);

        UpdateEducationRequest updateRequest = new UpdateEducationRequest(
                "New Degree", "New Institution",
                LocalDate.of(2016, 9, 1), LocalDate.of(2020, 6, 15),
                "Computer Science", "4.0", "Updated notes", null,
                created.version()
        );

        EducationResponse updated = educationService.updateEducation(profileId, created.id(), updateRequest);

        assertThat(updated.degree()).isEqualTo("New Degree");
        assertThat(updated.institution()).isEqualTo("New Institution");
        assertThat(updated.fieldOfStudy()).isEqualTo("Computer Science");
        assertThat(updated.gpa()).isEqualTo("4.0");
        assertThat(updated.version()).isEqualTo(created.version() + 1);
    }

    @Test
    void deleteEducation_Success() {
        CreateEducationRequest createRequest = new CreateEducationRequest(
                "Delete Degree", "Delete Institution",
                LocalDate.of(2016, 9, 1), LocalDate.of(2020, 6, 15),
                null, null, null, null
        );
        EducationResponse created = educationService.createEducation(profileId, createRequest);

        educationService.deleteEducation(profileId, created.id());

        List<EducationResponse> educations = educationService.getEducationsByProfileId(profileId);
        assertThat(educations).isEmpty();
    }

    @Test
    void deleteEducation_NotFound_ThrowsException() {
        assertThatThrownBy(() -> educationService.deleteEducation(profileId, 999L))
                .isInstanceOf(ValidationException.class);
    }
}
