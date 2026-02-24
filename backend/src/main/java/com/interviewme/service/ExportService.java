package com.interviewme.service;

import com.interviewme.billing.config.BillingProperties;
import com.interviewme.billing.model.CoinTransaction;
import com.interviewme.billing.model.RefType;
import com.interviewme.billing.service.CoinWalletService;
import com.interviewme.exports.dto.*;
import com.interviewme.exports.service.FileStorageService;
import com.interviewme.exports.mapper.ExportMapper;
import com.interviewme.exports.model.ExportHistory;
import com.interviewme.exports.model.ExportStatus;
import com.interviewme.exports.model.ExportTemplate;
import com.interviewme.exports.model.ExportType;
import com.interviewme.exports.repository.ExportHistoryRepository;
import com.interviewme.exports.repository.ExportTemplateRepository;
import com.interviewme.model.Profile;
import com.interviewme.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExportService {

    private final ExportTemplateRepository templateRepository;
    private final ExportHistoryRepository exportHistoryRepository;
    private final ProfileRepository profileRepository;
    private final CoinWalletService coinWalletService;
    private final BillingProperties billingProperties;
    private final ExportJobService exportJobService;
    private final FileStorageService fileStorageService;

    @Transactional
    public ExportHistoryResponse createResumeExport(Long tenantId, Long profileId, ExportResumeRequest request) {
        log.info("Creating resume export: tenantId={}, profileId={}, templateId={}",
                tenantId, profileId, request.templateId());

        // 1. Validate template exists and is active
        ExportTemplate template = templateRepository.findById(request.templateId())
                .orElseThrow(() -> new RuntimeException("Export template not found: " + request.templateId()));

        if (!template.getIsActive()) {
            throw new RuntimeException("Export template is not active: " + template.getName());
        }

        // 2. Validate profile has minimum data
        Profile profile = profileRepository.findById(profileId)
                .orElseThrow(() -> new RuntimeException("Profile not found: " + profileId));

        if (profile.getFullName() == null || profile.getFullName().isBlank()
                || profile.getHeadline() == null || profile.getHeadline().isBlank()) {
            throw new IllegalArgumentException(
                    "Profile must have at least a name and headline to generate a resume");
        }

        // 3. Get cost and deduct coins
        int cost = billingProperties.getCosts().getOrDefault("RESUME_EXPORT", 10);
        CoinTransaction tx = coinWalletService.spend(
                tenantId, cost, RefType.EXPORT, null, "Resume export");

        // 4. Create ExportHistory with PENDING status
        Map<String, Object> params = new HashMap<>();
        params.put("targetRole", request.targetRole());
        params.put("location", request.location());
        params.put("seniority", request.seniority());
        params.put("language", request.language() != null ? request.language() : "en");

        ExportHistory export = new ExportHistory();
        export.setTenantId(tenantId);
        export.setProfileId(profileId);
        export.setTemplate(template);
        export.setType(ExportType.RESUME.name());
        export.setStatus(ExportStatus.PENDING.name());
        export.setParameters(params);
        export.setCoinsSpent(cost);
        export.setCoinTransactionId(tx.getId());

        ExportHistory saved = exportHistoryRepository.save(export);
        log.info("Export history created: exportId={}, status=PENDING", saved.getId());

        // 5. Submit async job
        exportJobService.processExport(saved.getId());

        return ExportMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public ExportStatusResponse getExportStatus(Long tenantId, Long exportId) {
        ExportHistory export = exportHistoryRepository.findByIdAndTenantId(exportId, tenantId)
                .orElseThrow(() -> new RuntimeException("Export not found: " + exportId));
        return ExportMapper.toStatusResponse(export);
    }

    @Transactional(readOnly = true)
    public byte[] downloadExport(Long tenantId, Long exportId) {
        ExportHistory export = exportHistoryRepository.findByIdAndTenantId(exportId, tenantId)
                .orElseThrow(() -> new RuntimeException("Export not found: " + exportId));

        if (!ExportStatus.COMPLETED.name().equals(export.getStatus())) {
            throw new RuntimeException("Export is not completed: " + export.getStatus());
        }

        if (export.getFileUrl() == null) {
            throw new RuntimeException("Export file not found");
        }

        return fileStorageService.retrieve(export.getFileUrl());
    }

    @Transactional(readOnly = true)
    public ExportHistoryPageResponse getExportHistory(Long tenantId, int page, int size,
                                                       String type, String status) {
        PageRequest pageRequest = PageRequest.of(page, Math.min(size, 100));

        Page<ExportHistory> historyPage;
        if (type != null && status != null) {
            historyPage = exportHistoryRepository.findByTenantIdAndTypeAndStatus(
                    tenantId, type, status, pageRequest);
        } else if (type != null) {
            historyPage = exportHistoryRepository.findByTenantIdAndTypeOrderByCreatedAtDesc(
                    tenantId, type, pageRequest);
        } else if (status != null) {
            historyPage = exportHistoryRepository.findByTenantIdAndStatusOrderByCreatedAtDesc(
                    tenantId, status, pageRequest);
        } else {
            historyPage = exportHistoryRepository.findByTenantIdOrderByCreatedAtDesc(
                    tenantId, pageRequest);
        }

        List<ExportHistoryResponse> content = historyPage.getContent().stream()
                .map(ExportMapper::toResponse)
                .toList();

        return new ExportHistoryPageResponse(
                content,
                historyPage.getNumber(),
                historyPage.getSize(),
                historyPage.getTotalElements(),
                historyPage.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public List<ExportTemplateResponse> getActiveTemplates() {
        return templateRepository.findByIsActiveTrue().stream()
                .map(ExportMapper::toResponse)
                .toList();
    }
}
