package com.interviewme.profile.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewme.dto.profile.CreateProfileRequest;
import com.interviewme.dto.profile.UpdateProfileRequest;
import com.interviewme.model.Profile;
import com.interviewme.model.User;
import com.interviewme.repository.ProfileRepository;
import com.interviewme.security.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

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
    private static final Long USER_ID = 100L;

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(TENANT_ID);
        setAuthenticatedUser(USER_ID, "test@example.com");
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    private void setAuthenticatedUser(Long userId, String email) {
        User mockUser = new User();
        mockUser.setId(userId);
        mockUser.setEmail(email);
        mockUser.setTenantId(TENANT_ID);
        mockUser.setPasswordHash("encoded");

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        mockUser, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @Test
    void createProfile_Success() throws Exception {
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

        mockMvc.perform(post("/api/profiles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.fullName").value("John Doe"))
                .andExpect(jsonPath("$.headline").value("Senior Developer"))
                .andExpect(jsonPath("$.userId").value(USER_ID));
    }

    @Test
    void createProfile_ValidationError() throws Exception {
        CreateProfileRequest request = new CreateProfileRequest(
                null, null, null, null, null, null, null, null
        );

        mockMvc.perform(post("/api/profiles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"));
    }

    @Test
    void getCurrentUserProfile_Success() throws Exception {
        Profile profile = new Profile();
        profile.setUserId(USER_ID);
        profile.setTenantId(TENANT_ID);
        profile.setFullName("Jane Doe");
        profile.setHeadline("Developer");
        profileRepository.save(profile);

        mockMvc.perform(get("/api/profiles/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Jane Doe"))
                .andExpect(jsonPath("$.userId").value(USER_ID));
    }

    @Test
    void updateProfile_Success() throws Exception {
        Profile profile = new Profile();
        profile.setUserId(USER_ID);
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

        mockMvc.perform(put("/api/profiles/" + profile.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Bob Smith Jr."))
                .andExpect(jsonPath("$.headline").value("Senior Engineer"));
    }

    @Test
    void deleteProfile_Success() throws Exception {
        Profile profile = new Profile();
        profile.setUserId(USER_ID);
        profile.setTenantId(TENANT_ID);
        profile.setFullName("Delete Me");
        profile.setHeadline("To Delete");
        profile = profileRepository.save(profile);

        mockMvc.perform(delete("/api/profiles/" + profile.getId()))
                .andExpect(status().isNoContent());
    }

    @Test
    void checkProfileExists_ReturnsTrue() throws Exception {
        Profile profile = new Profile();
        profile.setUserId(USER_ID);
        profile.setTenantId(TENANT_ID);
        profile.setFullName("Exists Test");
        profile.setHeadline("Test");
        profileRepository.save(profile);

        mockMvc.perform(get("/api/profiles/exists"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    void checkProfileExists_ReturnsFalse() throws Exception {
        setAuthenticatedUser(999L, "other@example.com");

        mockMvc.perform(get("/api/profiles/exists"))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));
    }
}
