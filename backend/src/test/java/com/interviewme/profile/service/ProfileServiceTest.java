package com.interviewme.profile.service;

import com.interviewme.common.exception.DuplicateProfileException;
import com.interviewme.common.exception.ProfileNotFoundException;
import com.interviewme.profile.dto.profile.CreateProfileRequest;
import com.interviewme.profile.dto.profile.ProfileResponse;
import com.interviewme.profile.dto.profile.UpdateProfileRequest;
import com.interviewme.profile.model.Profile;
import com.interviewme.profile.repository.ProfileRepository;
import com.interviewme.security.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ProfileServiceTest {

    @Autowired
    private ProfileService profileService;

    @Autowired
    private ProfileRepository profileRepository;

    private static final Long TENANT_ID = 1L;
    private static final Long USER_ID = 100L;

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(TENANT_ID);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void createProfile_Success() {
        // Given
        CreateProfileRequest request = new CreateProfileRequest(
                "John Doe",
                "Senior Software Engineer",
                "Experienced software engineer",
                "San Francisco, CA",
                List.of("English", "Spanish"),
                null,
                null,
                null
        );

        // When
        ProfileResponse response = profileService.createProfile(USER_ID, request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.id()).isNotNull();
        assertThat(response.userId()).isEqualTo(USER_ID);
        assertThat(response.fullName()).isEqualTo("John Doe");
        assertThat(response.headline()).isEqualTo("Senior Software Engineer");
    }

    @Test
    void createProfile_DuplicateProfile_ThrowsException() {
        // Given
        CreateProfileRequest request = new CreateProfileRequest(
                "John Doe", "Headline", null, null, null, null, null, null
        );
        profileService.createProfile(USER_ID, request);

        // When/Then
        assertThatThrownBy(() -> profileService.createProfile(USER_ID, request))
                .isInstanceOf(DuplicateProfileException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void getProfileByUserId_Success() {
        // Given
        CreateProfileRequest createRequest = new CreateProfileRequest(
                "Jane Smith", "Developer", null, null, null, null, null, null
        );
        ProfileResponse created = profileService.createProfile(USER_ID, createRequest);

        // When
        ProfileResponse retrieved = profileService.getProfileByUserId(USER_ID);

        // Then
        assertThat(retrieved).isNotNull();
        assertThat(retrieved.id()).isEqualTo(created.id());
        assertThat(retrieved.fullName()).isEqualTo("Jane Smith");
    }

    @Test
    void getProfileByUserId_NotFound_ThrowsException() {
        // When/Then
        assertThatThrownBy(() -> profileService.getProfileByUserId(999L))
                .isInstanceOf(ProfileNotFoundException.class);
    }

    @Test
    void updateProfile_Success() {
        // Given
        CreateProfileRequest createRequest = new CreateProfileRequest(
                "Bob Johnson", "Engineer", null, null, null, null, null, null
        );
        ProfileResponse created = profileService.createProfile(USER_ID, createRequest);

        UpdateProfileRequest updateRequest = new UpdateProfileRequest(
                "Bob Johnson Jr.",
                "Tech Lead",
                null, null, null, null, null, null,
                created.version()
        );

        // When
        ProfileResponse updated = profileService.updateProfile(created.id(), updateRequest);

        // Then
        assertThat(updated.fullName()).isEqualTo("Bob Johnson Jr.");
        assertThat(updated.headline()).isEqualTo("Tech Lead");
        assertThat(updated.version()).isEqualTo(created.version() + 1);
    }

    @Test
    void deleteProfile_Success() {
        // Given
        CreateProfileRequest createRequest = new CreateProfileRequest(
                "Delete Me", "Headline", null, null, null, null, null, null
        );
        ProfileResponse created = profileService.createProfile(USER_ID, createRequest);

        // When
        profileService.deleteProfile(created.id());

        // Then
        assertThatThrownBy(() -> profileService.getProfileById(created.id()))
                .isInstanceOf(ProfileNotFoundException.class);
    }

    @Test
    void profileExists_ReturnsTrueWhenExists() {
        // Given
        CreateProfileRequest createRequest = new CreateProfileRequest(
                "Exists Test", "Headline", null, null, null, null, null, null
        );
        profileService.createProfile(USER_ID, createRequest);

        // When
        boolean exists = profileService.profileExists(USER_ID);

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    void profileExists_ReturnsFalseWhenNotExists() {
        // When
        boolean exists = profileService.profileExists(999L);

        // Then
        assertThat(exists).isFalse();
    }
}
