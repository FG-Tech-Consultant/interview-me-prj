package com.interviewme.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewme.dto.packages.*;
import com.interviewme.dto.profile.ProfileResponse;
import com.interviewme.model.User;
import com.interviewme.service.PackageService;
import com.interviewme.service.ProfileService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PackageController")
class PackageControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private PackageService packageService;

    @Mock
    private ProfileService profileService;

    @InjectMocks
    private PackageController packageController;

    private User testUser;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(packageController)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
        testUser = new User();
        testUser.setId(1L);
        testUser.setTenantId(100L);
        testUser.setEmail("1");
        authentication = new UsernamePasswordAuthenticationToken(testUser, null, testUser.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        ProfileResponse profileResponse = new ProfileResponse(
                10L, 100L, 1L, "John Doe", "Engineer", null,
                null, List.of(), Map.of(), Map.of(), "public", "john-doe", 0,
                0L, Instant.now(), Instant.now(), 1L, List.of(), List.of());
        lenient().when(profileService.getProfileByUserId(1L)).thenReturn(profileResponse);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private PackageResponse samplePackageResponse() {
        return new PackageResponse(
                1L, 10L, "My Package", "Description", "my-pkg",
                "token123", Instant.now().plusSeconds(86400), true, 0,
                Instant.now(), Instant.now(), 1);
    }

    @Nested
    @DisplayName("GET /api/packages")
    class ListPackages {

        @Test
        @DisplayName("returns list of packages")
        void success() throws Exception {
            when(packageService.getPackagesByProfile(10L)).thenReturn(List.of(samplePackageResponse()));

            mockMvc.perform(get("/api/packages").principal(authentication))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].name").value("My Package"));
        }
    }

    @Nested
    @DisplayName("GET /api/packages/{id}")
    class GetPackage {

        @Test
        @DisplayName("returns package detail")
        void success() throws Exception {
            PackageDetailResponse detail = new PackageDetailResponse(
                    1L, 10L, "My Package", "Description", "my-pkg",
                    "token123", Instant.now().plusSeconds(86400), true, 0,
                    List.of(), List.of(), List.of(),
                    Instant.now(), Instant.now(), 1);
            when(packageService.getPackageDetail(1L)).thenReturn(detail);

            mockMvc.perform(get("/api/packages/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("My Package"))
                    .andExpect(jsonPath("$.slug").value("my-pkg"));
        }
    }

    @Nested
    @DisplayName("POST /api/packages")
    class CreatePackage {

        @Test
        @DisplayName("returns 201 on success")
        void success() throws Exception {
            when(packageService.createPackage(eq(10L), any(CreatePackageRequest.class)))
                    .thenReturn(samplePackageResponse());

            mockMvc.perform(post("/api/packages")
                            .principal(authentication)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new CreatePackageRequest("My Package", "Description", "my-pkg"))))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.name").value("My Package"));
        }

        @Test
        @DisplayName("returns 400 when name is blank")
        void blankName() throws Exception {
            mockMvc.perform(post("/api/packages")
                            .principal(authentication)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new CreatePackageRequest("", "Description", "my-pkg"))))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("PUT /api/packages/{id}")
    class UpdatePackage {

        @Test
        @DisplayName("returns 200 on success")
        void success() throws Exception {
            when(packageService.updatePackage(eq(1L), any(UpdatePackageRequest.class)))
                    .thenReturn(samplePackageResponse());

            mockMvc.perform(put("/api/packages/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new UpdatePackageRequest("Updated Name", "New desc", true, 1L))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("My Package"));
        }
    }

    @Nested
    @DisplayName("DELETE /api/packages/{id}")
    class DeletePackage {

        @Test
        @DisplayName("returns 204 on success")
        void success() throws Exception {
            doNothing().when(packageService).deletePackage(1L);

            mockMvc.perform(delete("/api/packages/1"))
                    .andExpect(status().isNoContent());

            verify(packageService).deletePackage(1L);
        }
    }

    @Nested
    @DisplayName("Package item operations")
    class PackageItems {

        @Test
        @DisplayName("POST /api/packages/{id}/skills - adds skill")
        void addSkill() throws Exception {
            doNothing().when(packageService).addSkill(eq(1L), any(AddPackageItemRequest.class));

            mockMvc.perform(post("/api/packages/1/skills")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new AddPackageItemRequest(5L, 1))))
                    .andExpect(status().isCreated());

            verify(packageService).addSkill(eq(1L), any(AddPackageItemRequest.class));
        }

        @Test
        @DisplayName("DELETE /api/packages/{id}/skills/{skillId} - removes skill")
        void removeSkill() throws Exception {
            doNothing().when(packageService).removeSkill(1L, 5L);

            mockMvc.perform(delete("/api/packages/1/skills/5"))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("POST /api/packages/{id}/projects - adds project")
        void addProject() throws Exception {
            doNothing().when(packageService).addProject(eq(1L), any(AddPackageItemRequest.class));

            mockMvc.perform(post("/api/packages/1/projects")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new AddPackageItemRequest(3L, 1))))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("POST /api/packages/{id}/stories - adds story")
        void addStory() throws Exception {
            doNothing().when(packageService).addStory(eq(1L), any(AddPackageItemRequest.class));

            mockMvc.perform(post("/api/packages/1/stories")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new AddPackageItemRequest(7L, 2))))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("POST /api/packages/{id}/skills - returns 400 when itemId is null")
        void addSkill_MissingItemId() throws Exception {
            mockMvc.perform(post("/api/packages/1/skills")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"displayOrder\":1}"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/packages/{id}/regenerate-token")
    class RegenerateToken {

        @Test
        @DisplayName("returns updated package")
        void success() throws Exception {
            when(packageService.regenerateToken(1L)).thenReturn(samplePackageResponse());

            mockMvc.perform(post("/api/packages/1/regenerate-token"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").value("token123"));
        }
    }
}
