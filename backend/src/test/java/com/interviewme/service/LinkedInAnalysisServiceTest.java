package com.interviewme.service;

import com.interviewme.billing.model.CoinTransaction;
import com.interviewme.billing.model.RefType;
import com.interviewme.billing.service.CoinWalletService;
import com.interviewme.common.dto.ai.LlmClient;
import com.interviewme.common.dto.ai.LlmRequest;
import com.interviewme.common.dto.ai.LlmResponse;
import com.interviewme.common.exception.*;
import com.interviewme.linkedin.dto.*;
import com.interviewme.linkedin.model.*;
import com.interviewme.linkedin.repository.LinkedInAnalysisRepository;
import com.interviewme.linkedin.repository.LinkedInSectionScoreRepository;
import com.interviewme.linkedin.service.LinkedInPdfParserService;
import com.interviewme.model.Profile;
import com.interviewme.repository.ProfileRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LinkedInAnalysisService")
class LinkedInAnalysisServiceTest {

    @Mock private LinkedInAnalysisRepository analysisRepository;
    @Mock private LinkedInSectionScoreRepository sectionScoreRepository;
    @Mock private LinkedInPdfParserService pdfParserService;
    @Mock private LinkedInPromptService promptService;
    @Mock private LlmClient llmClient;
    @Mock private CoinWalletService coinWalletService;
    @Mock private ProfileRepository profileRepository;

    @InjectMocks
    private LinkedInAnalysisService linkedInAnalysisService;

    private static final Long TENANT_ID = 100L;
    private static final Long PROFILE_ID = 10L;

    @Nested
    @DisplayName("startAnalysis")
    class StartAnalysis {

        @Test
        @DisplayName("throws InvalidPdfException for null PDF")
        void throwsForNullPdf() {
            assertThatThrownBy(() -> linkedInAnalysisService.startAnalysis(TENANT_ID, PROFILE_ID, null))
                    .isInstanceOf(InvalidPdfException.class);
        }

        @Test
        @DisplayName("throws InvalidPdfException for empty PDF")
        void throwsForEmptyPdf() {
            MultipartFile pdf = mock(MultipartFile.class);
            when(pdf.isEmpty()).thenReturn(true);

            assertThatThrownBy(() -> linkedInAnalysisService.startAnalysis(TENANT_ID, PROFILE_ID, pdf))
                    .isInstanceOf(InvalidPdfException.class)
                    .hasMessageContaining("required");
        }

        @Test
        @DisplayName("throws InvalidPdfException for non-PDF file")
        void throwsForNonPdfFile() {
            MultipartFile pdf = mock(MultipartFile.class);
            when(pdf.isEmpty()).thenReturn(false);
            when(pdf.getOriginalFilename()).thenReturn("document.docx");

            assertThatThrownBy(() -> linkedInAnalysisService.startAnalysis(TENANT_ID, PROFILE_ID, pdf))
                    .isInstanceOf(InvalidPdfException.class)
                    .hasMessageContaining("PDF");
        }

        @Test
        @DisplayName("throws InvalidPdfException for wrong content type")
        void throwsForWrongContentType() {
            MultipartFile pdf = mock(MultipartFile.class);
            when(pdf.isEmpty()).thenReturn(false);
            when(pdf.getOriginalFilename()).thenReturn("file.pdf");
            when(pdf.getContentType()).thenReturn("application/msword");

            assertThatThrownBy(() -> linkedInAnalysisService.startAnalysis(TENANT_ID, PROFILE_ID, pdf))
                    .isInstanceOf(InvalidPdfException.class);
        }

        @Test
        @DisplayName("throws ProfileNotFoundException for missing profile")
        void throwsForMissingProfile() {
            MultipartFile pdf = mock(MultipartFile.class);
            when(pdf.isEmpty()).thenReturn(false);
            when(pdf.getOriginalFilename()).thenReturn("linkedin.pdf");
            when(pdf.getContentType()).thenReturn("application/pdf");
            when(profileRepository.findByIdAndTenantId(PROFILE_ID, TENANT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> linkedInAnalysisService.startAnalysis(TENANT_ID, PROFILE_ID, pdf))
                    .isInstanceOf(ProfileNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("getAnalysis")
    class GetAnalysis {

        @Test
        @DisplayName("returns analysis with section scores when completed")
        void returnsAnalysisWithScores() {
            LinkedInAnalysis analysis = new LinkedInAnalysis();
            analysis.setId(1L);
            analysis.setTenantId(TENANT_ID);
            analysis.setStatus(AnalysisStatus.COMPLETED.name());
            analysis.setOverallScore(85);
            when(analysisRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(analysis));

            LinkedInSectionScore score = new LinkedInSectionScore();
            score.setSectionName("HEADLINE");
            score.setSectionScore(90);
            when(sectionScoreRepository.findByAnalysisIdOrderBySectionName(1L)).thenReturn(List.of(score));

            LinkedInAnalysisResponse result = linkedInAnalysisService.getAnalysis(1L, TENANT_ID);

            assertThat(result).isNotNull();
            verify(sectionScoreRepository).findByAnalysisIdOrderBySectionName(1L);
        }

        @Test
        @DisplayName("returns analysis without scores when not completed")
        void returnsWithoutScoresWhenPending() {
            LinkedInAnalysis analysis = new LinkedInAnalysis();
            analysis.setId(1L);
            analysis.setTenantId(TENANT_ID);
            analysis.setStatus(AnalysisStatus.PENDING.name());
            when(analysisRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(analysis));

            linkedInAnalysisService.getAnalysis(1L, TENANT_ID);

            verify(sectionScoreRepository, never()).findByAnalysisIdOrderBySectionName(anyLong());
        }

        @Test
        @DisplayName("throws AnalysisNotFoundException when not found")
        void throwsWhenNotFound() {
            when(analysisRepository.findByIdAndTenantId(99L, TENANT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> linkedInAnalysisService.getAnalysis(99L, TENANT_ID))
                    .isInstanceOf(AnalysisNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("generateAdditionalSuggestions")
    class GenerateAdditionalSuggestions {

        @Test
        @DisplayName("throws AnalysisNotFoundException when analysis not found")
        void throwsWhenAnalysisNotFound() {
            when(analysisRepository.findByIdAndTenantId(99L, TENANT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> linkedInAnalysisService.generateAdditionalSuggestions(99L, "HEADLINE", 3, TENANT_ID))
                    .isInstanceOf(AnalysisNotFoundException.class);
        }

        @Test
        @DisplayName("throws AnalysisNotCompletedException when analysis not completed")
        void throwsWhenNotCompleted() {
            LinkedInAnalysis analysis = new LinkedInAnalysis();
            analysis.setId(1L);
            analysis.setStatus(AnalysisStatus.IN_PROGRESS.name());
            when(analysisRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(analysis));

            assertThatThrownBy(() -> linkedInAnalysisService.generateAdditionalSuggestions(1L, "HEADLINE", 3, TENANT_ID))
                    .isInstanceOf(AnalysisNotCompletedException.class);
        }

        @Test
        @DisplayName("refunds coins on LLM failure")
        void refundsCoinsOnFailure() {
            LinkedInAnalysis analysis = new LinkedInAnalysis();
            analysis.setId(1L);
            analysis.setStatus(AnalysisStatus.COMPLETED.name());
            when(analysisRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(analysis));

            LinkedInSectionScore score = new LinkedInSectionScore();
            score.setId(10L);
            score.setSuggestions(List.of("existing suggestion"));
            score.setRawContent("some content");
            when(sectionScoreRepository.findByAnalysisIdAndSectionName(1L, "HEADLINE"))
                    .thenReturn(Optional.of(score));

            CoinTransaction coinTx = new CoinTransaction();
            coinTx.setId(999L);
            when(coinWalletService.spend(eq(TENANT_ID), anyInt(), eq(RefType.LINKEDIN_SUGGESTION), anyString(), anyString()))
                    .thenReturn(coinTx);

            when(promptService.buildAdditionalSuggestionsPrompt(anyString(), anyString(), anyList(), anyInt()))
                    .thenThrow(new RuntimeException("LLM error"));

            assertThatThrownBy(() -> linkedInAnalysisService.generateAdditionalSuggestions(1L, "HEADLINE", 3, TENANT_ID))
                    .isInstanceOf(RuntimeException.class);

            verify(coinWalletService).refund(TENANT_ID, 999L);
        }
    }

    @Nested
    @DisplayName("applySuggestion")
    class ApplySuggestion {

        @Test
        @DisplayName("throws AnalysisNotCompletedException when not completed")
        void throwsWhenNotCompleted() {
            LinkedInAnalysis analysis = new LinkedInAnalysis();
            analysis.setId(1L);
            analysis.setStatus(AnalysisStatus.PENDING.name());
            when(analysisRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(analysis));

            assertThatThrownBy(() -> linkedInAnalysisService.applySuggestion(1L, "HEADLINE", 0, TENANT_ID))
                    .isInstanceOf(AnalysisNotCompletedException.class);
        }

        @Test
        @DisplayName("throws SectionNotApplicableException for non-applicable section")
        void throwsForNonApplicableSection() {
            LinkedInAnalysis analysis = new LinkedInAnalysis();
            analysis.setId(1L);
            analysis.setStatus(AnalysisStatus.COMPLETED.name());
            when(analysisRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(analysis));

            assertThatThrownBy(() -> linkedInAnalysisService.applySuggestion(1L, "INVALID_SECTION", 0, TENANT_ID))
                    .isInstanceOf(SectionNotApplicableException.class);
        }

        @Test
        @DisplayName("throws ValidationException for out-of-range suggestion index")
        void throwsForInvalidIndex() {
            LinkedInAnalysis analysis = new LinkedInAnalysis();
            analysis.setId(1L);
            analysis.setProfileId(PROFILE_ID);
            analysis.setStatus(AnalysisStatus.COMPLETED.name());
            when(analysisRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(analysis));

            LinkedInSectionScore score = new LinkedInSectionScore();
            score.setSuggestions(List.of("Only one suggestion"));
            when(sectionScoreRepository.findByAnalysisIdAndSectionName(1L, "HEADLINE"))
                    .thenReturn(Optional.of(score));

            assertThatThrownBy(() -> linkedInAnalysisService.applySuggestion(1L, "HEADLINE", 5, TENANT_ID))
                    .isInstanceOf(ValidationException.class);
        }

        @Test
        @DisplayName("applies HEADLINE suggestion to profile")
        void appliesHeadlineSuggestion() {
            LinkedInAnalysis analysis = new LinkedInAnalysis();
            analysis.setId(1L);
            analysis.setProfileId(PROFILE_ID);
            analysis.setStatus(AnalysisStatus.COMPLETED.name());
            when(analysisRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(analysis));

            LinkedInSectionScore score = new LinkedInSectionScore();
            score.setSuggestions(List.of("New Headline Text"));
            when(sectionScoreRepository.findByAnalysisIdAndSectionName(1L, "HEADLINE"))
                    .thenReturn(Optional.of(score));

            Profile profile = new Profile();
            profile.setId(PROFILE_ID);
            profile.setHeadline("Old Headline");
            when(profileRepository.findByIdAndTenantId(PROFILE_ID, TENANT_ID)).thenReturn(Optional.of(profile));
            when(profileRepository.save(any(Profile.class))).thenReturn(profile);

            linkedInAnalysisService.applySuggestion(1L, "HEADLINE", 0, TENANT_ID);

            verify(profileRepository).save(argThat(p -> p.getHeadline().equals("New Headline Text")));
        }

        @Test
        @DisplayName("applies ABOUT suggestion to profile summary")
        void appliesAboutSuggestion() {
            LinkedInAnalysis analysis = new LinkedInAnalysis();
            analysis.setId(1L);
            analysis.setProfileId(PROFILE_ID);
            analysis.setStatus(AnalysisStatus.COMPLETED.name());
            when(analysisRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(analysis));

            LinkedInSectionScore score = new LinkedInSectionScore();
            score.setSuggestions(List.of("Updated summary text"));
            when(sectionScoreRepository.findByAnalysisIdAndSectionName(1L, "ABOUT"))
                    .thenReturn(Optional.of(score));

            Profile profile = new Profile();
            profile.setId(PROFILE_ID);
            profile.setSummary("Old summary");
            when(profileRepository.findByIdAndTenantId(PROFILE_ID, TENANT_ID)).thenReturn(Optional.of(profile));
            when(profileRepository.save(any(Profile.class))).thenReturn(profile);

            linkedInAnalysisService.applySuggestion(1L, "ABOUT", 0, TENANT_ID);

            verify(profileRepository).save(argThat(p -> p.getSummary().equals("Updated summary text")));
        }
    }

    @Nested
    @DisplayName("updateStatus")
    class UpdateStatus {

        @Test
        @DisplayName("updates status and overall score")
        void updatesStatusAndScore() {
            LinkedInAnalysis analysis = new LinkedInAnalysis();
            analysis.setId(1L);
            analysis.setStatus(AnalysisStatus.IN_PROGRESS.name());
            when(analysisRepository.findById(1L)).thenReturn(Optional.of(analysis));
            when(analysisRepository.save(any(LinkedInAnalysis.class))).thenReturn(analysis);

            linkedInAnalysisService.updateStatus(1L, AnalysisStatus.COMPLETED, 85, null);

            assertThat(analysis.getStatus()).isEqualTo(AnalysisStatus.COMPLETED.name());
            assertThat(analysis.getOverallScore()).isEqualTo(85);
            assertThat(analysis.getAnalyzedAt()).isNotNull();
        }

        @Test
        @DisplayName("updates status with error message on failure")
        void updatesWithErrorMessage() {
            LinkedInAnalysis analysis = new LinkedInAnalysis();
            analysis.setId(1L);
            when(analysisRepository.findById(1L)).thenReturn(Optional.of(analysis));
            when(analysisRepository.save(any(LinkedInAnalysis.class))).thenReturn(analysis);

            linkedInAnalysisService.updateStatus(1L, AnalysisStatus.FAILED, null, "Parse error");

            assertThat(analysis.getStatus()).isEqualTo(AnalysisStatus.FAILED.name());
            assertThat(analysis.getErrorMessage()).isEqualTo("Parse error");
        }
    }
}
