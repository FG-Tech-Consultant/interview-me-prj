package com.interviewme.integration;

import com.interviewme.common.util.TenantContext;
import com.interviewme.exports.model.ExportHistory;
import com.interviewme.exports.model.ExportStatus;
import com.interviewme.exports.model.ExportTemplate;
import com.interviewme.exports.repository.ExportHistoryRepository;
import com.interviewme.exports.repository.ExportTemplateRepository;
import com.interviewme.model.*;
import com.interviewme.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ExportLifecycleIntegrationTest extends AbstractIntegrationTest {

    @Autowired private TenantRepository tenantRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private ProfileRepository profileRepository;
    @Autowired private ExportHistoryRepository exportHistoryRepository;
    @Autowired private ExportTemplateRepository exportTemplateRepository;

    private Long tenantId;
    private Long profileId;
    private ExportTemplate resumeTemplate;

    @BeforeEach
    void setUp() {
        Tenant tenant = new Tenant();
        tenant.setName("export-tenant-" + System.nanoTime());
        tenant = tenantRepository.save(tenant);
        tenantId = tenant.getId();

        User user = new User();
        user.setTenantId(tenantId);
        user.setEmail("export-" + System.nanoTime() + "@test.com");
        user.setPasswordHash("hash");
        user = userRepository.save(user);

        Profile profile = new Profile();
        profile.setTenantId(tenantId);
        profile.setUserId(user.getId());
        profile.setFullName("Export User");
        profile.setHeadline("Dev");
        profile = profileRepository.save(profile);
        profileId = profile.getId();

        TenantContext.setTenantId(tenantId);

        // Get or create resume template (seeded by Liquibase)
        resumeTemplate = exportTemplateRepository.findByTypeAndIsActiveTrue("RESUME")
            .stream().findFirst()
            .orElseGet(() -> {
                ExportTemplate t = new ExportTemplate();
                t.setName("Standard Resume");
                t.setType("RESUME");
                t.setTemplateFile("standard-resume.html");
                t.setIsActive(true);
                return exportTemplateRepository.save(t);
            });
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @Transactional
    void createExportHistory_statusTransitions() {
        // Create export in PENDING state
        ExportHistory export = new ExportHistory();
        export.setTenantId(tenantId);
        export.setProfileId(profileId);
        export.setTemplate(resumeTemplate);
        export.setType("RESUME");
        export.setStatus(ExportStatus.PENDING.name());
        export.setParameters(Map.of("market", "US"));
        export.setCoinsSpent(10);
        export = exportHistoryRepository.save(export);
        Long exportId = export.getId();

        assertThat(export.getStatus()).isEqualTo("PENDING");

        // Transition to IN_PROGRESS
        export.setStatus(ExportStatus.IN_PROGRESS.name());
        export = exportHistoryRepository.save(export);
        assertThat(export.getStatus()).isEqualTo("IN_PROGRESS");

        // Transition to COMPLETED
        export.setStatus(ExportStatus.COMPLETED.name());
        export.setFileUrl("/exports/resume-" + exportId + ".pdf");
        export.setFileSizeBytes(50000L);
        export = exportHistoryRepository.save(export);
        assertThat(export.getStatus()).isEqualTo("COMPLETED");
        assertThat(export.getFileUrl()).contains("resume-");
    }

    @Test
    @Transactional
    void createExportHistory_failedStatus() {
        ExportHistory export = new ExportHistory();
        export.setTenantId(tenantId);
        export.setProfileId(profileId);
        export.setTemplate(resumeTemplate);
        export.setType("RESUME");
        export.setStatus(ExportStatus.PENDING.name());
        export.setParameters(Map.of());
        export.setCoinsSpent(10);
        export = exportHistoryRepository.save(export);

        // Simulate failure
        export.setStatus(ExportStatus.FAILED.name());
        export.setErrorMessage("PDF generation timed out");
        export.setRetryCount(3);
        export = exportHistoryRepository.save(export);

        assertThat(export.getStatus()).isEqualTo("FAILED");
        assertThat(export.getErrorMessage()).isEqualTo("PDF generation timed out");
        assertThat(export.getRetryCount()).isEqualTo(3);
    }

    @Test
    @Transactional
    void exportHistory_paginatedQuery() {
        // Create multiple exports
        for (int i = 0; i < 5; i++) {
            ExportHistory export = new ExportHistory();
            export.setTenantId(tenantId);
            export.setProfileId(profileId);
            export.setTemplate(resumeTemplate);
            export.setType("RESUME");
            export.setStatus(ExportStatus.COMPLETED.name());
            export.setParameters(Map.of("index", i));
            export.setCoinsSpent(10);
            exportHistoryRepository.save(export);
        }

        Page<ExportHistory> page = exportHistoryRepository
            .findByTenantIdOrderByCreatedAtDesc(tenantId, PageRequest.of(0, 3));
        assertThat(page.getContent()).hasSize(3);
        assertThat(page.getTotalElements()).isEqualTo(5);
        assertThat(page.getTotalPages()).isEqualTo(2);
    }
}
