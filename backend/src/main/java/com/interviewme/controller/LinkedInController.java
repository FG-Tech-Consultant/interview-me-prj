package com.interviewme.controller;

import com.interviewme.common.util.TenantContext;
import com.interviewme.linkedin.dto.*;
import com.interviewme.linkedin.model.AnalysisSourceType;
import com.interviewme.service.LinkedInAnalysisService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/linkedin")
@RequiredArgsConstructor
@Slf4j
public class LinkedInController {

    private final LinkedInAnalysisService analysisService;

    @PostMapping(value = "/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Transactional
    public ResponseEntity<StartAnalysisResponse> analyze(
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam("profileId") Long profileId,
            @RequestParam(value = "sourceType", defaultValue = "PDF") String sourceType,
            Authentication authentication) {
        Long tenantId = TenantContext.getCurrentTenantId();
        log.info("POST /api/v1/linkedin/analyze - profileId={}, tenantId={}, sourceType={}", profileId, tenantId, sourceType);

        AnalysisSourceType source;
        try {
            source = AnalysisSourceType.valueOf(sourceType.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }

        StartAnalysisResponse response = analysisService.startAnalysis(tenantId, profileId, file, source);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @GetMapping("/analyses/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<LinkedInAnalysisResponse> getAnalysis(@PathVariable Long id) {
        Long tenantId = TenantContext.getCurrentTenantId();
        log.info("GET /api/v1/linkedin/analyses/{} - tenantId={}", id, tenantId);

        LinkedInAnalysisResponse response = analysisService.getAnalysis(id, tenantId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/analyses")
    @Transactional(readOnly = true)
    public ResponseEntity<Page<LinkedInAnalysisSummary>> getAnalysisHistory(
            @RequestParam Long profileId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long tenantId = TenantContext.getCurrentTenantId();
        log.info("GET /api/v1/linkedin/analyses - profileId={}, tenantId={}", profileId, tenantId);

        Page<LinkedInAnalysisSummary> history = analysisService.getAnalysisHistory(
                profileId, tenantId, PageRequest.of(page, Math.min(size, 50)));
        return ResponseEntity.ok(history);
    }

    @PostMapping("/analyses/{id}/sections/{sectionName}/suggestions")
    @Transactional
    public ResponseEntity<SectionScoreResponse> generateSuggestions(
            @PathVariable Long id,
            @PathVariable String sectionName,
            @Valid @RequestBody GenerateSuggestionsRequest request) {
        Long tenantId = TenantContext.getCurrentTenantId();
        log.info("POST /api/v1/linkedin/analyses/{}/sections/{}/suggestions - tenantId={}",
                id, sectionName, tenantId);

        SectionScoreResponse response = analysisService.generateAdditionalSuggestions(
                id, sectionName, request.count(), tenantId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/analyses/{id}/sections/{sectionName}/apply")
    @Transactional
    public ResponseEntity<Void> applySuggestion(
            @PathVariable Long id,
            @PathVariable String sectionName,
            @Valid @RequestBody ApplySuggestionRequest request) {
        Long tenantId = TenantContext.getCurrentTenantId();
        log.info("POST /api/v1/linkedin/analyses/{}/sections/{}/apply - tenantId={}, index={}",
                id, sectionName, tenantId, request.suggestionIndex());

        analysisService.applySuggestion(id, sectionName, request.suggestionIndex(), tenantId);
        return ResponseEntity.ok().build();
    }
}
