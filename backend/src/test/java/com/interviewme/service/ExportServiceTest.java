package com.interviewme.service;

import com.interviewme.billing.config.BillingProperties;
import com.interviewme.billing.model.CoinTransaction;
import com.interviewme.billing.model.RefType;
import com.interviewme.billing.service.CoinWalletService;
import com.interviewme.exports.dto.*;
import com.interviewme.exports.service.FileStorageService;
import com.interviewme.exports.model.ExportHistory;
import com.interviewme.exports.model.ExportStatus;
import com.interviewme.exports.model.ExportTemplate;
import com.interviewme.exports.model.ExportType;
import com.interviewme.exports.repository.ExportHistoryRepository;
import com.interviewme.exports.repository.ExportTemplateRepository;
import com.interviewme.model.Profile;
import com.interviewme.repository.ProfileRepository;
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

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExportService")
class ExportServiceTest {

    @Mock private ExportTemplateRepository templateRepository;
    @Mock private ExportHistoryRepository exportHistoryRepository;
    @Mock private ProfileRepository profileRepository;
    @Mock private CoinWalletService coinWalletService;
    @Mock private BillingProperties billingProperties;
    @Mock private ExportJobService exportJobService;
    @Mock private FileStorageService fileStorageService;

    @InjectMocks
    private ExportService exportService;

    private static final Long TENANT_ID = 100L;
    private static final Long PROFILE_ID = 10L;

    private ExportTemplate buildTemplate(Long id, boolean active) {
        ExportTemplate t = new ExportTemplate();
        t.setId(id);
        t.setName("Modern Resume");
        t.setIsActive(active);
        t.setTemplateFile("modern-resume.html");
        return t;
    }

    private Profile buildProfile() {
        Profile p = new Profile();
        p.setId(PROFILE_ID);
        p.setFullName("John Doe");
        p.setHeadline("Senior Dev");
        return p;
    }

    private CoinTransaction buildCoinTx() {
        CoinTransaction tx = new CoinTransaction();
        tx.setId(999L);
        return tx;
    }

    @Nested
    @DisplayName("createResumeExport")
    class CreateResumeExport {

        @Test
        @DisplayName("creates resume export successfully")
        void createsResumeExportSuccessfully() {
            ExportTemplate template = buildTemplate(1L, true);
            when(templateRepository.findById(1L)).thenReturn(Optional.of(template));

            Profile profile = buildProfile();
            when(profileRepository.findById(PROFILE_ID)).thenReturn(Optional.of(profile));

            Map<String, Integer> costs = Map.of("RESUME_EXPORT", 10);
            when(billingProperties.getCosts()).thenReturn(costs);
            when(coinWalletService.spend(eq(TENANT_ID), eq(10), eq(RefType.EXPORT), isNull(), anyString()))
                    .thenReturn(buildCoinTx());

            ExportHistory saved = new ExportHistory();
            saved.setId(50L);
            saved.setTenantId(TENANT_ID);
            saved.setProfileId(PROFILE_ID);
            saved.setTemplate(template);
            saved.setType(ExportType.RESUME.name());
            saved.setStatus(ExportStatus.PENDING.name());
            saved.setCoinsSpent(10);
            saved.setCreatedAt(Instant.now());
            when(exportHistoryRepository.save(any(ExportHistory.class))).thenReturn(saved);

            ExportResumeRequest request = new ExportResumeRequest(1L, "Backend Dev", "NYC", "Senior", "en");

            ExportHistoryResponse result = exportService.createResumeExport(TENANT_ID, PROFILE_ID, request);

            assertThat(result).isNotNull();
            verify(exportJobService).processExport(50L);
            verify(coinWalletService).spend(eq(TENANT_ID), eq(10), eq(RefType.EXPORT), isNull(), anyString());
        }

        @Test
        @DisplayName("throws when template not found")
        void throwsWhenTemplateNotFound() {
            when(templateRepository.findById(99L)).thenReturn(Optional.empty());

            ExportResumeRequest request = new ExportResumeRequest(99L, "Dev", null, null, null);

            assertThatThrownBy(() -> exportService.createResumeExport(TENANT_ID, PROFILE_ID, request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("template not found");
        }

        @Test
        @DisplayName("throws when template is inactive")
        void throwsWhenTemplateInactive() {
            ExportTemplate template = buildTemplate(1L, false);
            when(templateRepository.findById(1L)).thenReturn(Optional.of(template));

            ExportResumeRequest request = new ExportResumeRequest(1L, "Dev", null, null, null);

            assertThatThrownBy(() -> exportService.createResumeExport(TENANT_ID, PROFILE_ID, request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("not active");
        }

        @Test
        @DisplayName("throws when profile not found")
        void throwsWhenProfileNotFound() {
            ExportTemplate template = buildTemplate(1L, true);
            when(templateRepository.findById(1L)).thenReturn(Optional.of(template));
            when(profileRepository.findById(PROFILE_ID)).thenReturn(Optional.empty());

            ExportResumeRequest request = new ExportResumeRequest(1L, "Dev", null, null, null);

            assertThatThrownBy(() -> exportService.createResumeExport(TENANT_ID, PROFILE_ID, request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Profile not found");
        }

        @Test
        @DisplayName("throws when profile has no name")
        void throwsWhenProfileIncomplete() {
            ExportTemplate template = buildTemplate(1L, true);
            when(templateRepository.findById(1L)).thenReturn(Optional.of(template));

            Profile profile = new Profile();
            profile.setId(PROFILE_ID);
            profile.setFullName("");
            profile.setHeadline("Dev");
            when(profileRepository.findById(PROFILE_ID)).thenReturn(Optional.of(profile));

            ExportResumeRequest request = new ExportResumeRequest(1L, "Dev", null, null, null);

            assertThatThrownBy(() -> exportService.createResumeExport(TENANT_ID, PROFILE_ID, request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("name and headline");
        }
    }

    @Nested
    @DisplayName("createCoverLetterExport")
    class CreateCoverLetterExport {

        @Test
        @DisplayName("creates cover letter export with correct parameters")
        void createsCoverLetterExport() {
            ExportTemplate template = buildTemplate(1L, true);
            when(templateRepository.findById(1L)).thenReturn(Optional.of(template));
            when(profileRepository.findById(PROFILE_ID)).thenReturn(Optional.of(buildProfile()));
            when(billingProperties.getCosts()).thenReturn(Map.of("COVER_LETTER_EXPORT", 10));
            when(coinWalletService.spend(eq(TENANT_ID), eq(10), eq(RefType.EXPORT), isNull(), anyString()))
                    .thenReturn(buildCoinTx());

            ExportHistory saved = new ExportHistory();
            saved.setId(51L);
            saved.setTenantId(TENANT_ID);
            saved.setTemplate(template);
            saved.setType(ExportType.COVER_LETTER.name());
            saved.setStatus(ExportStatus.PENDING.name());
            saved.setCreatedAt(Instant.now());
            when(exportHistoryRepository.save(any(ExportHistory.class))).thenReturn(saved);

            ExportCoverLetterRequest request = new ExportCoverLetterRequest(
                    1L, "Acme Corp", "Backend Dev", "Build APIs", "US");

            ExportHistoryResponse result = exportService.createCoverLetterExport(TENANT_ID, PROFILE_ID, request);

            assertThat(result).isNotNull();
            verify(exportHistoryRepository).save(argThat(h ->
                    h.getType().equals(ExportType.COVER_LETTER.name())));
        }
    }

    @Nested
    @DisplayName("createBackgroundDeckExport")
    class CreateBackgroundDeckExport {

        @Test
        @DisplayName("creates background deck export with correct cost")
        void createsBackgroundDeckExport() {
            ExportTemplate template = buildTemplate(1L, true);
            when(templateRepository.findById(1L)).thenReturn(Optional.of(template));
            when(profileRepository.findById(PROFILE_ID)).thenReturn(Optional.of(buildProfile()));
            when(billingProperties.getCosts()).thenReturn(Map.of("BACKGROUND_DECK_EXPORT", 15));
            when(coinWalletService.spend(eq(TENANT_ID), eq(15), eq(RefType.EXPORT), isNull(), anyString()))
                    .thenReturn(buildCoinTx());

            ExportHistory saved = new ExportHistory();
            saved.setId(52L);
            saved.setTenantId(TENANT_ID);
            saved.setTemplate(template);
            saved.setType(ExportType.BACKGROUND_DECK.name());
            saved.setStatus(ExportStatus.PENDING.name());
            saved.setCreatedAt(Instant.now());
            when(exportHistoryRepository.save(any(ExportHistory.class))).thenReturn(saved);

            ExportBackgroundDeckRequest request = new ExportBackgroundDeckRequest(1L);

            ExportHistoryResponse result = exportService.createBackgroundDeckExport(TENANT_ID, PROFILE_ID, request);

            assertThat(result).isNotNull();
            verify(coinWalletService).spend(eq(TENANT_ID), eq(15), eq(RefType.EXPORT), isNull(), anyString());
        }
    }

    @Nested
    @DisplayName("getExportStatus")
    class GetExportStatus {

        @Test
        @DisplayName("returns status for existing export")
        void returnsStatus() {
            ExportHistory export = new ExportHistory();
            export.setId(50L);
            export.setTenantId(TENANT_ID);
            export.setStatus(ExportStatus.IN_PROGRESS.name());
            export.setCreatedAt(Instant.now());
            when(exportHistoryRepository.findByIdAndTenantId(50L, TENANT_ID)).thenReturn(Optional.of(export));

            ExportStatusResponse result = exportService.getExportStatus(TENANT_ID, 50L);

            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("throws when export not found")
        void throwsWhenNotFound() {
            when(exportHistoryRepository.findByIdAndTenantId(99L, TENANT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> exportService.getExportStatus(TENANT_ID, 99L))
                    .isInstanceOf(RuntimeException.class);
        }
    }

    @Nested
    @DisplayName("downloadExport")
    class DownloadExport {

        @Test
        @DisplayName("returns file bytes for completed export")
        void returnsFileBytes() {
            ExportHistory export = new ExportHistory();
            export.setId(50L);
            export.setTenantId(TENANT_ID);
            export.setStatus(ExportStatus.COMPLETED.name());
            export.setFileUrl("exports/resume-50.pdf");
            when(exportHistoryRepository.findByIdAndTenantId(50L, TENANT_ID)).thenReturn(Optional.of(export));
            when(fileStorageService.retrieve("exports/resume-50.pdf")).thenReturn(new byte[]{1, 2, 3});

            byte[] result = exportService.downloadExport(TENANT_ID, 50L);

            assertThat(result).hasSize(3);
        }

        @Test
        @DisplayName("throws when export not completed")
        void throwsWhenNotCompleted() {
            ExportHistory export = new ExportHistory();
            export.setId(50L);
            export.setTenantId(TENANT_ID);
            export.setStatus(ExportStatus.IN_PROGRESS.name());
            when(exportHistoryRepository.findByIdAndTenantId(50L, TENANT_ID)).thenReturn(Optional.of(export));

            assertThatThrownBy(() -> exportService.downloadExport(TENANT_ID, 50L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("not completed");
        }

        @Test
        @DisplayName("throws when file URL is null")
        void throwsWhenFileUrlNull() {
            ExportHistory export = new ExportHistory();
            export.setId(50L);
            export.setTenantId(TENANT_ID);
            export.setStatus(ExportStatus.COMPLETED.name());
            export.setFileUrl(null);
            when(exportHistoryRepository.findByIdAndTenantId(50L, TENANT_ID)).thenReturn(Optional.of(export));

            assertThatThrownBy(() -> exportService.downloadExport(TENANT_ID, 50L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("file not found");
        }
    }

    @Nested
    @DisplayName("getExportHistory")
    class GetExportHistory {

        @Test
        @DisplayName("returns paginated history with type and status filters")
        void returnsPaginatedHistory() {
            ExportHistory h = new ExportHistory();
            h.setId(50L);
            h.setTenantId(TENANT_ID);
            h.setType(ExportType.RESUME.name());
            h.setStatus(ExportStatus.COMPLETED.name());
            h.setCreatedAt(Instant.now());

            Page<ExportHistory> page = new PageImpl<>(List.of(h), PageRequest.of(0, 10), 1);
            when(exportHistoryRepository.findByTenantIdAndTypeAndStatus(eq(TENANT_ID), eq("RESUME"), eq("COMPLETED"), any()))
                    .thenReturn(page);

            ExportHistoryPageResponse result = exportService.getExportHistory(TENANT_ID, 0, 10, "RESUME", "COMPLETED");

            assertThat(result.content()).hasSize(1);
            assertThat(result.totalElements()).isEqualTo(1);
        }

        @Test
        @DisplayName("returns history without filters")
        void returnsWithoutFilters() {
            Page<ExportHistory> page = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
            when(exportHistoryRepository.findByTenantIdOrderByCreatedAtDesc(eq(TENANT_ID), any()))
                    .thenReturn(page);

            ExportHistoryPageResponse result = exportService.getExportHistory(TENANT_ID, 0, 10, null, null);

            assertThat(result.content()).isEmpty();
        }

        @Test
        @DisplayName("caps page size at 100")
        void capsPageSize() {
            Page<ExportHistory> page = new PageImpl<>(List.of(), PageRequest.of(0, 100), 0);
            when(exportHistoryRepository.findByTenantIdOrderByCreatedAtDesc(eq(TENANT_ID), any()))
                    .thenReturn(page);

            exportService.getExportHistory(TENANT_ID, 0, 500, null, null);

            verify(exportHistoryRepository).findByTenantIdOrderByCreatedAtDesc(
                    eq(TENANT_ID), argThat(pr -> pr.getPageSize() == 100));
        }
    }

    @Nested
    @DisplayName("getActiveTemplates")
    class GetActiveTemplates {

        @Test
        @DisplayName("returns active templates")
        void returnsActiveTemplates() {
            ExportTemplate t = buildTemplate(1L, true);
            when(templateRepository.findByIsActiveTrue()).thenReturn(List.of(t));

            List<ExportTemplateResponse> result = exportService.getActiveTemplates();

            assertThat(result).hasSize(1);
        }
    }
}
