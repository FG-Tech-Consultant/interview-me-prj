package com.interviewme.controller;

import com.interviewme.dto.publicprofile.PublicProfileResponse;
import com.interviewme.service.PublicProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PublicProfileController")
class PublicProfileControllerTest {

    private MockMvc mockMvc;

    @Mock
    private PublicProfileService publicProfileService;

    @InjectMocks
    private PublicProfileController publicProfileController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(publicProfileController).build();
    }

    @Test
    @DisplayName("GET /api/public/profiles/{slug} - returns public profile")
    void getPublicProfile_Success() throws Exception {
        PublicProfileResponse response = new PublicProfileResponse(
                "john-doe", "John Doe", "Senior Engineer", "Summary",
                "Berlin", List.of("en"), Map.of("linkedin", "https://linkedin.com/in/john"),
                List.of(), List.of(), List.of(), null);
        when(publicProfileService.getPublicProfile("john-doe")).thenReturn(response);

        mockMvc.perform(get("/api/public/profiles/john-doe"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value("john-doe"))
                .andExpect(jsonPath("$.fullName").value("John Doe"))
                .andExpect(jsonPath("$.headline").value("Senior Engineer"));
    }

    @Test
    @DisplayName("GET /api/public/profiles/{slug} - throws when not found")
    void getPublicProfile_NotFound() {
        when(publicProfileService.getPublicProfile("unknown"))
                .thenThrow(new RuntimeException("Profile not found"));

        assertThrows(Exception.class, () ->
                mockMvc.perform(get("/api/public/profiles/unknown")));
    }
}
