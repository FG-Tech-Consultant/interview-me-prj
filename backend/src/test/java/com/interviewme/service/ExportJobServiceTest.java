package com.interviewme.service;

import com.interviewme.billing.service.CoinWalletService;
import com.interviewme.exports.config.ExportProperties;
import com.interviewme.exports.service.FileStorageService;
import com.interviewme.exports.service.PdfGenerationService;
import com.interviewme.exports.model.ExportHistory;
import com.interviewme.exports.model.ExportStatus;
import com.interviewme.exports.model.ExportTemplate;
import com.interviewme.exports.model.ExportType;
import com.interviewme.exports.repository.ExportHistoryRepository;
import com.interviewme.model.*;
import com.interviewme.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExportJobService")
class ExportJobServiceTest {

    @Mock private ExportHistoryRepository exportHistoryRepository;
    @Mock private ProfileRepository profileRepository;
    @Mock private JobExperienceRepository jobExperienceRepository;
    @Mock private EducationRepository educationRepository;
    @Mock private UserSkillRepository userSkillRepository;
    @Mock private PdfGenerationService pdfGenerationService;
    @Mock private FileStorageService fileStorageService;
    @Mock private CoinWalletService coinWalletService;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private ExportProperties exportProperties;

    @InjectMocks
    private ExportJobService exportJobService;

    private static final Long TENANT_ID = 100L;
    private static final Long PROFILE_ID = 10L;

    private ExportHistory buildExport(Long id, String type) {
        ExportTemplate template = new ExportTemplate();
        template.setId(1L);
        template.setTemplateFile("template.html");

        ExportHistory export = new ExportHistory();
        export.setId(id);
        export.setTenantId(TENANT_ID);
        export.setProfileId(PROFILE_ID);
        export.setTemplate(template);
        export.setType(type);
        export.setStatus(ExportStatus.PENDING.name());
        export.setCoinsSpent(10);
        export.setCoinTransactionId(999L);
        export.setCreatedAt(Instant.now());

        Map<String, Object> params = new HashMap<>();
        params.put("targetRole", "Backend Dev");
        params.put("location", "NYC");
        params.put("seniority", "Senior");
        params.put("language", "en");
        export.setParameters(params);

        return export;
    }

    private Profile buildProfile() {
        Profile p = new Profile();
        p.setId(PROFILE_ID);
        p.setFullName("John Doe");
        p.setHeadline("Senior Developer");
        return p;
    }

    private ExportProperties.RetryConfig buildRetryConfig(int maxAttempts) {
        ExportProperties.RetryConfig config = new ExportProperties.RetryConfig();
        config.setMaxAttempts(maxAttempts);
        config.setInitialDelayMs(100);
        config.setMultiplier(2);
        return config;
    }

    @Nested
    @DisplayName("processExport - resume")
    class ProcessResumeExport {

        @Test
        @DisplayName("processes resume export successfully")
        void processesResumeSuccessfully() {
            ExportHistory export = buildExport(50L, ExportType.RESUME.name());
            when(exportHistoryRepository.findById(50L)).thenReturn(Optional.of(export));
            when(exportProperties.getRetry()).thenReturn(buildRetryConfig(0));
            when(profileRepository.findById(PROFILE_ID)).thenReturn(Optional.of(buildProfile()));
            when(userSkillRepository.findByProfileIdAndDeletedAtIsNullOrderBySkill_CategoryAscSkill_NameAsc(PROFILE_ID))
                    .thenReturn(Collections.emptyList());
            when(jobExperienceRepository.findByProfileIdAndDeletedAtIsNullOrderByStartDateDesc(PROFILE_ID))
                    .thenReturn(Collections.emptyList());
            when(educationRepository.findByProfileIdAndDeletedAtIsNullOrderByEndDateDesc(PROFILE_ID))
                    .thenReturn(Collections.emptyList());

            byte[] pdfBytes = new byte[]{1, 2, 3, 4};
            when(pdfGenerationService.generatePdf(eq("template.html"), anyMap())).thenReturn(pdfBytes);
            when(fileStorageService.store(eq(TENANT_ID), anyString(), eq(pdfBytes)))
                    .thenReturn("exports/resume-50.pdf");
            when(exportHistoryRepository.save(any(ExportHistory.class))).thenAnswer(inv -> inv.getArgument(0));

            exportJobService.processExport(50L);

            // Verify status updated to COMPLETED
            verify(exportHistoryRepository, atLeast(2)).save(argThat(h ->
                    h.getId().equals(50L)));
            assertThat(export.getStatus()).isEqualTo(ExportStatus.COMPLETED.name());
            assertThat(export.getFileUrl()).isEqualTo("exports/resume-50.pdf");
            verify(eventPublisher).publishEvent(any(Object.class));
        }
    }

    @Nested
    @DisplayName("processExport - cover letter")
    class ProcessCoverLetterExport {

        @Test
        @DisplayName("builds cover letter context with top skills and recent job")
        void buildsCoverLetterContext() {
            ExportHistory export = buildExport(51L, ExportType.COVER_LETTER.name());
            export.getParameters().put("targetCompany", "Acme Corp");
            export.getParameters().put("jobDescription", "Build APIs");
            export.getParameters().put("market", "US");

            when(exportHistoryRepository.findById(51L)).thenReturn(Optional.of(export));
            when(exportProperties.getRetry()).thenReturn(buildRetryConfig(0));
            when(profileRepository.findById(PROFILE_ID)).thenReturn(Optional.of(buildProfile()));

            Skill catalogSkill = new Skill();
            catalogSkill.setName("Java");
            catalogSkill.setCategory("Language");
            UserSkill us = new UserSkill();
            us.setSkill(catalogSkill);
            us.setProficiencyDepth(4);
            us.setYearsOfExperience(5);
            when(userSkillRepository.findByProfileIdAndDeletedAtIsNullOrderBySkill_CategoryAscSkill_NameAsc(PROFILE_ID))
                    .thenReturn(List.of(us));

            JobExperience job = new JobExperience();
            job.setCompany("Previous Corp");
            when(jobExperienceRepository.findByProfileIdAndDeletedAtIsNullOrderByStartDateDesc(PROFILE_ID))
                    .thenReturn(List.of(job));

            byte[] pdfBytes = new byte[]{5, 6};
            when(pdfGenerationService.generatePdf(eq("template.html"), anyMap())).thenReturn(pdfBytes);
            when(fileStorageService.store(eq(TENANT_ID), anyString(), eq(pdfBytes)))
                    .thenReturn("exports/cover-letter-51.pdf");
            when(exportHistoryRepository.save(any(ExportHistory.class))).thenAnswer(inv -> inv.getArgument(0));

            exportJobService.processExport(51L);

            // Verify PDF generated with cover letter context
            verify(pdfGenerationService).generatePdf(eq("template.html"), argThat(ctx ->
                    ctx.containsKey("targetCompany") &&
                    ctx.containsKey("topSkills") &&
                    ctx.containsKey("recentJob")
            ));
        }
    }

    @Nested
    @DisplayName("processExport - failure")
    class ProcessExportFailure {

        @Test
        @DisplayName("handles failure and auto-refunds coins")
        void handlesFailureWithRefund() {
            ExportHistory export = buildExport(50L, ExportType.RESUME.name());
            when(exportHistoryRepository.findById(50L)).thenReturn(Optional.of(export));
            when(exportProperties.getRetry()).thenReturn(buildRetryConfig(0));
            when(profileRepository.findById(PROFILE_ID))
                    .thenThrow(new RuntimeException("DB connection error"));
            when(exportHistoryRepository.save(any(ExportHistory.class))).thenAnswer(inv -> inv.getArgument(0));

            exportJobService.processExport(50L);

            assertThat(export.getStatus()).isEqualTo(ExportStatus.FAILED.name());
            assertThat(export.getErrorMessage()).contains("DB connection error");
            verify(coinWalletService).refund(TENANT_ID, 999L);
        }

        @Test
        @DisplayName("does not refund when no transaction ID")
        void noRefundWithoutTransactionId() {
            ExportHistory export = buildExport(50L, ExportType.RESUME.name());
            export.setCoinTransactionId(null);
            when(exportHistoryRepository.findById(50L)).thenReturn(Optional.of(export));
            when(exportProperties.getRetry()).thenReturn(buildRetryConfig(0));
            when(profileRepository.findById(PROFILE_ID))
                    .thenThrow(new RuntimeException("fail"));
            when(exportHistoryRepository.save(any(ExportHistory.class))).thenAnswer(inv -> inv.getArgument(0));

            exportJobService.processExport(50L);

            verify(coinWalletService, never()).refund(anyLong(), anyLong());
        }
    }
}
