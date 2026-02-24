package com.interviewme.profile.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewme.profile.dto.profile.CreateProfileRequest;
import com.interviewme.profile.dto.profile.UpdateProfileRequest;
import com.interviewme.profile.model.Profile;
import com.interviewme.profile.repository.ProfileRepository;
import com.interviewme.security.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProfileRepository profileRepository;

    private static final Long TENANT_ID = 1L;
    private static final String USER_ID_STR = "100";

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(TENANT_ID);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @WithMockUser(username = USER_ID_STR)
    void createProfile_Success() throws Exception {
        // Given
        CreateProfileRequest request = new CreateProfileRequest(
                "John Doe",
                "Senior Developer",
                "Bio here",
                "New York",
                List.of("English"),
                null,
                null,
                null
        );

        // When/Then
        mockMvc.perform(post("/api/profiles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.fullName").value("John Doe"))
                .andExpect(jsonPath("$.headline").value("Senior Developer"))
                .andExpect(jsonPath("$.userId").value(Long.parseLong(USER_ID_STR)));
    }

    @Test
    @WithMockUser(username = USER_ID_STR)
    void createProfile_ValidationError() throws Exception {
        // Given - missing required fullName
        CreateProfileRequest request = new CreateProfileRequest(
                null, null, null, null, null, null, null, null
        );

        // When/Then
        mockMvc.perform(post("/api/profiles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"));
    }

    @Test
    @WithMockUser(username = USER_ID_STR)
    void getCurrentUserProfile_Success() throws Exception {
        // Given
        Profile profile = new Profile();
        profile.setUserId(Long.parseLong(USER_ID_STR));
        profile.setTenantId(TENANT_ID);
        profile.setFullName("Jane Doe");
        profile.setHeadline("Developer");
        profileRepository.save(profile);

        // When/Then
        mockMvc.perform(get("/api/profiles/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Jane Doe"))
                .andExpect(jsonPath("$.userId").value(Long.parseLong(USER_ID_STR)));
    }

    @Test
    @WithMockUser(username = USER_ID_STR)
    void updateProfile_Success() throws Exception {
        // Given
        Profile profile = new Profile();
        profile.setUserId(Long.parseLong(USER_ID_STR));
        profile.setTenantId(TENANT_ID);
        profile.setFullName("Bob Smith");
        profile.setHeadline("Engineer");
        profile = profileRepository.save(profile);

        UpdateProfileRequest updateRequest = new UpdateProfileRequest(
                "Bob Smith Jr.",
                "Senior Engineer",
                null, null, null, null, null, null,
                profile.getVersion()
        );

        // When/Then
        mockMvc.perform(put("/api/profiles/" + profile.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Bob Smith Jr."))
                .andExpect(jsonPath("$.headline").value("Senior Engineer"));
    }

    @Test
    @WithMockUser(username = USER_ID_STR)
    void deleteProfile_Success() throws Exception {
        // Given
        Profile profile = new Profile();
        profile.setUserId(Long.parseLong(USER_ID_STR));
        profile.setTenantId(TENANT_ID);
        profile.setFullName("Delete Me");
        profile.setHeadline("To Delete");
        profile = profileRepository.save(profile);

        // When/Then
        mockMvc.perform(delete("/api/profiles/" + profile.getId()))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = USER_ID_STR)
    void checkProfileExists_ReturnsTrue() throws Exception {
        // Given
        Profile profile = new Profile();
        profile.setUserId(Long.parseLong(USER_ID_STR));
        profile.setTenantId(TENANT_ID);
        profile.setFullName("Exists Test");
        profile.setHeadline("Test");
        profileRepository.save(profile);

        // When/Then
        mockMvc.perform(get("/api/profiles/exists"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    @WithMockUser(username = "999")
    void checkProfileExists_ReturnsFalse() throws Exception {
        // When/Then
        mockMvc.perform(get("/api/profiles/exists"))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));
    }
}
