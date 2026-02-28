package com.interviewme.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewme.common.util.TenantContext;
import com.interviewme.linkedin.dto.*;
import com.interviewme.model.User;
import com.interviewme.service.LinkedInAnalysisService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LinkedInController")
class LinkedInControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private LinkedInAnalysisService analysisService;

    @InjectMocks
    private LinkedInController linkedInController;

    private Authentication authentication;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(linkedInController).build();
        TenantContext.setTenantId(100L);

        User testUser = new User();
        testUser.setId(1L);
        testUser.setTenantId(100L);
        testUser.setEmail("test@example.com");
        authentication = new UsernamePasswordAuthenticationToken(testUser, null, testUser.getAuthorities());
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Nested
    @DisplayName("POST /api/v1/linkedin/analyze")
    class Analyze {

        @Test
        @DisplayName("returns 202 on success")
        void success() throws Exception {
            StartAnalysisResponse response = new StartAnalysisResponse(1L, "PENDING", "Analysis started");
            when(analysisService.startAnalysis(eq(100L), eq(10L), any())).thenReturn(response);

            MockMultipartFile file = new MockMultipartFile(
                    "file", "profile.pdf", "application/pdf", "fake pdf content".getBytes());

            mockMvc.perform(multipart("/api/v1/linkedin/analyze")
                            .file(file)
                            .param("profileId", "10")
                            .principal(authentication))
                    .andExpect(status().isAccepted())
                    .andExpect(jsonPath("$.analysisId").value(1))
                    .andExpect(jsonPath("$.status").value("PENDING"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/linkedin/analyses/{id}")
    class GetAnalysis {

        @Test
        @DisplayName("returns analysis")
        void success() throws Exception {
            LinkedInAnalysisResponse response = new LinkedInAnalysisResponse(
                    1L, 10L, "COMPLETED", 85, null, "profile.pdf",
                    Instant.now(), List.of(), Instant.now());
            when(analysisService.getAnalysis(1L, 100L)).thenReturn(response);

            mockMvc.perform(get("/api/v1/linkedin/analyses/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.overallScore").value(85));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/linkedin/analyses")
    class GetAnalysisHistory {

        @Test
        @DisplayName("returns paginated history")
        void success() throws Exception {
            Page<LinkedInAnalysisSummary> page = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
            when(analysisService.getAnalysisHistory(eq(10L), eq(100L), any(PageRequest.class))).thenReturn(page);

            mockMvc.perform(get("/api/v1/linkedin/analyses")
                            .param("profileId", "10")
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/linkedin/analyses/{id}/sections/{sectionName}/suggestions")
    class GenerateSuggestions {

        @Test
        @DisplayName("returns section score response")
        void success() throws Exception {
            SectionScoreResponse response = new SectionScoreResponse(
                    1L, "headline", 70, "Good but could be better", List.of("Improve keywords"), false);
            when(analysisService.generateAdditionalSuggestions(1L, "headline", 3, 100L)).thenReturn(response);

            mockMvc.perform(post("/api/v1/linkedin/analyses/1/sections/headline/suggestions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new GenerateSuggestionsRequest(3))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.sectionName").value("headline"));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/linkedin/analyses/{id}/sections/{sectionName}/apply")
    class ApplySuggestion {

        @Test
        @DisplayName("returns 200 on success")
        void success() throws Exception {
            doNothing().when(analysisService).applySuggestion(1L, "headline", 0, 100L);

            mockMvc.perform(post("/api/v1/linkedin/analyses/1/sections/headline/apply")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new ApplySuggestionRequest(0))))
                    .andExpect(status().isOk());

            verify(analysisService).applySuggestion(1L, "headline", 0, 100L);
        }
    }
}
