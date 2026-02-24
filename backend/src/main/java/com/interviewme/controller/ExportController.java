package com.interviewme.controller;

import com.interviewme.common.util.TenantContext;
import com.interviewme.exports.dto.*;
import com.interviewme.service.ExportService;
import com.interviewme.service.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/exports")
@RequiredArgsConstructor
@Slf4j
public class ExportController {

    private final ExportService exportService;
    private final ProfileService profileService;

    @PostMapping("/resume")
    @Transactional
    public ResponseEntity<ExportHistoryResponse> createResumeExport(
            Authentication authentication,
            @Valid @RequestBody ExportResumeRequest request) {
        Long tenantId = TenantContext.getCurrentTenantId();
        Long userId = Long.parseLong(authentication.getName());
        log.info("POST /api/exports/resume - tenantId: {}, userId: {}", tenantId, userId);

        // Get profile ID from user
        var profile = profileService.getProfileByUserId(userId);

        ExportHistoryResponse response = exportService.createResumeExport(
                tenantId, profile.id(), request);

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @GetMapping("/{id}/status")
    @Transactional(readOnly = true)
    public ResponseEntity<ExportStatusResponse> getExportStatus(@PathVariable Long id) {
        Long tenantId = TenantContext.getCurrentTenantId();
        log.info("GET /api/exports/{}/status - tenantId: {}", id, tenantId);

        ExportStatusResponse response = exportService.getExportStatus(tenantId, id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/download")
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> downloadExport(@PathVariable Long id) {
        Long tenantId = TenantContext.getCurrentTenantId();
        log.info("GET /api/exports/{}/download - tenantId: {}", id, tenantId);

        byte[] pdfBytes = exportService.downloadExport(tenantId, id);

        String filename = "resume-" + LocalDate.now() + ".pdf";

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\"")
                .contentLength(pdfBytes.length)
                .body(pdfBytes);
    }

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<ExportHistoryPageResponse> getExportHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status) {
        Long tenantId = TenantContext.getCurrentTenantId();
        log.info("GET /api/exports - tenantId: {}, page: {}, size: {}", tenantId, page, size);

        ExportHistoryPageResponse response = exportService.getExportHistory(
                tenantId, page, size, type, status);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/templates")
    @Transactional(readOnly = true)
    public ResponseEntity<List<ExportTemplateResponse>> getTemplates() {
        log.info("GET /api/exports/templates");

        List<ExportTemplateResponse> templates = exportService.getActiveTemplates();
        return ResponseEntity.ok(templates);
    }
}
