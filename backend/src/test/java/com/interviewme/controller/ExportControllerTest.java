package com.interviewme.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewme.common.util.TenantContext;
import com.interviewme.dto.profile.ProfileResponse;
import com.interviewme.exports.dto.*;
import com.interviewme.model.User;
import com.interviewme.service.ExportService;
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
@DisplayName("ExportController")
class ExportControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private ExportService exportService;

    @Mock
    private ProfileService profileService;

    @InjectMocks
    private ExportController exportController;

    private User testUser;
    private Authentication authentication;
    private ProfileResponse profileResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(exportController).build();
        TenantContext.setTenantId(100L);

        testUser = new User();
        testUser.setId(1L);
        testUser.setTenantId(100L);
        testUser.setEmail("test@example.com");
        authentication = new UsernamePasswordAuthenticationToken(testUser, null, testUser.getAuthorities());

        profileResponse = new ProfileResponse(
                10L, 100L, 1L, "John Doe", "Engineer", "Summary",
                "Berlin", List.of("en"), Map.of(), Map.of(), "public", "john-doe", 0,
                0L, Instant.now(), Instant.now(), 1L, List.of(), List.of());
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private ExportHistoryResponse sampleExportResponse() {
        return new ExportHistoryResponse(
                1L, new ExportTemplateResponse(1L, "Standard Resume", "RESUME", "desc"),
                "RESUME", "PENDING", Map.of(), 10, null, 0,
                Instant.now(), null);
    }

    @Nested
    @DisplayName("POST /api/exports/resume")
    class CreateResumeExport {

        @Test
        @DisplayName("returns 202 on success")
        void success() throws Exception {
            when(profileService.getProfileByUserId(1L)).thenReturn(profileResponse);
            when(exportService.createResumeExport(eq(100L), eq(10L), any(ExportResumeRequest.class)))
                    .thenReturn(sampleExportResponse());

            mockMvc.perform(post("/api/exports/resume")
                            .principal(authentication)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new ExportResumeRequest(1L, "Software Engineer", "Berlin", "Senior", "en"))))
                    .andExpect(status().isAccepted())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.status").value("PENDING"));
        }

        @Test
        @DisplayName("returns 400 when templateId is null")
        void missingTemplateId() throws Exception {
            String json = "{\"targetRole\":\"Engineer\",\"seniority\":\"Senior\"}";

            mockMvc.perform(post("/api/exports/resume")
                            .principal(authentication)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("returns 400 when targetRole is blank")
        void blankTargetRole() throws Exception {
            mockMvc.perform(post("/api/exports/resume")
                            .principal(authentication)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new ExportResumeRequest(1L, "", "Berlin", "Senior", "en"))))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/exports/cover-letter")
    class CreateCoverLetterExport {

        @Test
        @DisplayName("returns 202 on success")
        void success() throws Exception {
            when(profileService.getProfileByUserId(1L)).thenReturn(profileResponse);
            when(exportService.createCoverLetterExport(eq(100L), eq(10L), any(ExportCoverLetterRequest.class)))
                    .thenReturn(sampleExportResponse());

            mockMvc.perform(post("/api/exports/cover-letter")
                            .principal(authentication)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new ExportCoverLetterRequest(1L, "Acme Corp", "Engineer", "Build things", "US"))))
                    .andExpect(status().isAccepted())
                    .andExpect(jsonPath("$.id").value(1));
        }

        @Test
        @DisplayName("returns 400 when targetCompany is blank")
        void blankTargetCompany() throws Exception {
            mockMvc.perform(post("/api/exports/cover-letter")
                            .principal(authentication)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new ExportCoverLetterRequest(1L, "", "Engineer", null, "US"))))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/exports/background-deck")
    class CreateBackgroundDeckExport {

        @Test
        @DisplayName("returns 202 on success")
        void success() throws Exception {
            when(profileService.getProfileByUserId(1L)).thenReturn(profileResponse);
            when(exportService.createBackgroundDeckExport(eq(100L), eq(10L), any(ExportBackgroundDeckRequest.class)))
                    .thenReturn(sampleExportResponse());

            mockMvc.perform(post("/api/exports/background-deck")
                            .principal(authentication)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new ExportBackgroundDeckRequest(1L))))
                    .andExpect(status().isAccepted())
                    .andExpect(jsonPath("$.id").value(1));
        }
    }

    @Nested
    @DisplayName("GET /api/exports/{id}/status")
    class GetExportStatus {

        @Test
        @DisplayName("returns 200 with status")
        void success() throws Exception {
            ExportStatusResponse statusResponse = new ExportStatusResponse(
                    1L, "COMPLETED", null, 0, Instant.now());
            when(exportService.getExportStatus(100L, 1L)).thenReturn(statusResponse);

            mockMvc.perform(get("/api/exports/1/status"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("COMPLETED"));
        }
    }

    @Nested
    @DisplayName("GET /api/exports/{id}/download")
    class DownloadExport {

        @Test
        @DisplayName("returns PDF bytes with correct headers")
        void success() throws Exception {
            byte[] pdfBytes = new byte[]{1, 2, 3, 4, 5};
            when(exportService.downloadExport(100L, 1L)).thenReturn(pdfBytes);
            when(exportService.getExportType(100L, 1L)).thenReturn("RESUME");

            mockMvc.perform(get("/api/exports/1/download"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                    .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("resume-")));
        }

        @Test
        @DisplayName("returns correct filename for cover letter")
        void coverLetterFilename() throws Exception {
            byte[] pdfBytes = new byte[]{1, 2, 3};
            when(exportService.downloadExport(100L, 2L)).thenReturn(pdfBytes);
            when(exportService.getExportType(100L, 2L)).thenReturn("COVER_LETTER");

            mockMvc.perform(get("/api/exports/2/download"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("cover-letter-")));
        }

        @Test
        @DisplayName("returns correct filename for background deck")
        void backgroundDeckFilename() throws Exception {
            byte[] pdfBytes = new byte[]{1, 2, 3};
            when(exportService.downloadExport(100L, 3L)).thenReturn(pdfBytes);
            when(exportService.getExportType(100L, 3L)).thenReturn("BACKGROUND_DECK");

            mockMvc.perform(get("/api/exports/3/download"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("background-deck-")));
        }
    }

    @Nested
    @DisplayName("GET /api/exports")
    class GetExportHistory {

        @Test
        @DisplayName("returns paginated history")
        void success() throws Exception {
            ExportHistoryPageResponse pageResponse = new ExportHistoryPageResponse(
                    List.of(sampleExportResponse()), 0, 20, 1, 1);
            when(exportService.getExportHistory(100L, 0, 20, null, null)).thenReturn(pageResponse);

            mockMvc.perform(get("/api/exports"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.totalElements").value(1));
        }

        @Test
        @DisplayName("passes type and status filters")
        void withFilters() throws Exception {
            ExportHistoryPageResponse pageResponse = new ExportHistoryPageResponse(
                    List.of(), 0, 10, 0, 0);
            when(exportService.getExportHistory(100L, 0, 10, "RESUME", "COMPLETED")).thenReturn(pageResponse);

            mockMvc.perform(get("/api/exports")
                            .param("page", "0")
                            .param("size", "10")
                            .param("type", "RESUME")
                            .param("status", "COMPLETED"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray());
        }
    }

    @Nested
    @DisplayName("GET /api/exports/templates")
    class GetTemplates {

        @Test
        @DisplayName("returns active templates")
        void success() throws Exception {
            List<ExportTemplateResponse> templates = List.of(
                    new ExportTemplateResponse(1L, "Standard Resume", "RESUME", "desc"),
                    new ExportTemplateResponse(2L, "Cover Letter", "COVER_LETTER", "desc"));
            when(exportService.getActiveTemplates()).thenReturn(templates);

            mockMvc.perform(get("/api/exports/templates"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].name").value("Standard Resume"));
        }
    }
}
