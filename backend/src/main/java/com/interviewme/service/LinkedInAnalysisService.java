package com.interviewme.service;

import com.interviewme.common.dto.ai.LlmClient;
import com.interviewme.common.dto.ai.LlmRequest;
import com.interviewme.common.dto.ai.LlmResponse;
import com.interviewme.billing.model.RefType;
import com.interviewme.billing.service.CoinWalletService;
import com.interviewme.billing.model.CoinTransaction;
import com.interviewme.common.exception.*;
import com.interviewme.common.util.TenantContext;
import com.interviewme.linkedin.dto.*;
import com.interviewme.linkedin.model.*;
import com.interviewme.linkedin.mapper.LinkedInAnalysisMapper;
import com.interviewme.linkedin.repository.LinkedInAnalysisRepository;
import com.interviewme.linkedin.service.LinkedInPdfParserService;
import com.interviewme.linkedin.service.LinkedInZipParserService;
import com.interviewme.linkedin.repository.LinkedInSectionScoreRepository;
import com.interviewme.model.Profile;
import com.interviewme.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class LinkedInAnalysisService {

    private final LinkedInAnalysisRepository analysisRepository;
    private final LinkedInSectionScoreRepository sectionScoreRepository;
    private final LinkedInPdfParserService pdfParserService;
    private final LinkedInZipParserService zipParserService;
    private final LinkedInPromptService promptService;
    private final ProfileDataCollectorService profileDataCollectorService;
    private final ZipDataToSectionsService zipDataToSectionsService;
    private final LlmClient llmClient;
    private final CoinWalletService coinWalletService;
    private final ProfileRepository profileRepository;

    @Value("${billing.costs.LINKEDIN_SUGGESTION:2}")
    private int suggestionCostPerItem;

    private static final int MAX_RETRIES = 3;
    private static final long[] RETRY_DELAYS_MS = {2000, 4000, 8000};

    @Transactional
    public StartAnalysisResponse startAnalysis(Long tenantId, Long profileId, MultipartFile file) {
        return startAnalysis(tenantId, profileId, file, AnalysisSourceType.PDF);
    }

    @Transactional
    public StartAnalysisResponse startAnalysis(Long tenantId, Long profileId, MultipartFile file,
                                                AnalysisSourceType sourceType) {
        log.info("Starting analysis for tenantId={}, profileId={}, sourceType={}", tenantId, profileId, sourceType);

        // Verify profile exists and belongs to tenant
        profileRepository.findByIdAndTenantId(profileId, tenantId)
                .orElseThrow(() -> new ProfileNotFoundException(profileId));

        if (sourceType == AnalysisSourceType.PROFILE) {
            return startProfileAnalysis(tenantId, profileId);
        } else if (sourceType == AnalysisSourceType.ZIP) {
            return startZipAnalysis(tenantId, profileId, file);
        } else {
            return startPdfAnalysis(tenantId, profileId, file);
        }
    }

    private StartAnalysisResponse startPdfAnalysis(Long tenantId, Long profileId, MultipartFile pdf) {
        validatePdf(pdf);

        // Save PDF to temp file
        Path tempPath;
        try {
            tempPath = Files.createTempFile("linkedin-", ".pdf");
            pdf.transferTo(tempPath.toFile());
        } catch (IOException e) {
            throw new RuntimeException("Failed to save uploaded PDF", e);
        }

        // Create analysis record
        LinkedInAnalysis analysis = new LinkedInAnalysis();
        analysis.setTenantId(tenantId);
        analysis.setProfileId(profileId);
        analysis.setStatus(AnalysisStatus.PENDING.name());
        analysis.setSourceType(AnalysisSourceType.PDF.name());
        analysis.setPdfFilename(pdf.getOriginalFilename());
        analysis = analysisRepository.save(analysis);

        log.info("PDF analysis created with id={}, triggering async processing", analysis.getId());

        // Trigger async processing
        processAnalysis(analysis.getId(), tenantId, tempPath);

        return new StartAnalysisResponse(
                analysis.getId(),
                AnalysisStatus.PENDING.name(),
                "Analysis started. Poll for status updates."
        );
    }

    private StartAnalysisResponse startZipAnalysis(Long tenantId, Long profileId, MultipartFile zip) {
        validateZip(zip);

        // Save ZIP to temp file
        Path tempPath;
        try {
            tempPath = Files.createTempFile("analysis-", ".zip");
            zip.transferTo(tempPath.toFile());
        } catch (IOException e) {
            throw new RuntimeException("Failed to save uploaded ZIP", e);
        }

        // Create analysis record
        LinkedInAnalysis analysis = new LinkedInAnalysis();
        analysis.setTenantId(tenantId);
        analysis.setProfileId(profileId);
        analysis.setStatus(AnalysisStatus.PENDING.name());
        analysis.setSourceType(AnalysisSourceType.ZIP.name());
        analysis.setPdfFilename(zip.getOriginalFilename());
        analysis = analysisRepository.save(analysis);

        log.info("ZIP analysis created with id={}, triggering async processing", analysis.getId());

        // Trigger async processing
        processZipAnalysis(analysis.getId(), tenantId, tempPath);

        return new StartAnalysisResponse(
                analysis.getId(),
                AnalysisStatus.PENDING.name(),
                "Analysis started. Poll for status updates."
        );
    }

    private StartAnalysisResponse startProfileAnalysis(Long tenantId, Long profileId) {
        // Create analysis record
        LinkedInAnalysis analysis = new LinkedInAnalysis();
        analysis.setTenantId(tenantId);
        analysis.setProfileId(profileId);
        analysis.setStatus(AnalysisStatus.PENDING.name());
        analysis.setSourceType(AnalysisSourceType.PROFILE.name());
        analysis.setPdfFilename(null);
        analysis = analysisRepository.save(analysis);

        log.info("Profile analysis created with id={}, triggering async processing", analysis.getId());

        // Trigger async processing
        processProfileAnalysis(analysis.getId(), tenantId, profileId);

        return new StartAnalysisResponse(
                analysis.getId(),
                AnalysisStatus.PENDING.name(),
                "Analysis started. Poll for status updates."
        );
    }

    @Async
    public void processAnalysis(Long analysisId, Long tenantId, Path pdfPath) {
        log.info("Processing PDF analysis id={}", analysisId);

        try {
            // Update status to IN_PROGRESS
            updateStatus(analysisId, AnalysisStatus.IN_PROGRESS, null, null);

            // Parse PDF
            Map<ProfileSection, String> sections;
            try {
                sections = pdfParserService.parse(pdfPath);
            } finally {
                // Always delete temp file
                deleteTempFile(pdfPath);
            }

            // Build prompt and call LLM with retries
            LlmRequest request = promptService.buildScoringPrompt(sections);
            LlmResponse llmResponse = callLlmWithRetry(request);

            // Parse LLM response
            LinkedInLlmResult result = promptService.parseScoringResponse(llmResponse);

            // Save results
            saveAnalysisResults(analysisId, tenantId, result, sections);

            log.info("Analysis id={} completed with overallScore={}", analysisId, result.overallScore());

        } catch (Exception e) {
            log.error("Analysis id={} failed", analysisId, e);
            updateStatus(analysisId, AnalysisStatus.FAILED, null, e.getMessage());
        }
    }

    @Async
    public void processZipAnalysis(Long analysisId, Long tenantId, Path zipPath) {
        log.info("Processing ZIP analysis id={}", analysisId);

        try {
            // Update status to IN_PROGRESS
            updateStatus(analysisId, AnalysisStatus.IN_PROGRESS, null, null);

            // Parse ZIP
            LinkedInImportData importData;
            try (InputStream is = Files.newInputStream(zipPath)) {
                importData = zipParserService.parseZip(is);
            } finally {
                deleteTempFile(zipPath);
            }

            // Convert to profile sections
            Map<ProfileSection, String> sections = zipDataToSectionsService.convertToSections(importData);

            // Build prompt and call LLM with retries
            LlmRequest request = promptService.buildScoringPrompt(sections);
            LlmResponse llmResponse = callLlmWithRetry(request);

            // Parse LLM response
            LinkedInLlmResult result = promptService.parseScoringResponse(llmResponse);

            // Save results
            saveAnalysisResults(analysisId, tenantId, result, sections);

            log.info("ZIP analysis id={} completed with overallScore={}", analysisId, result.overallScore());

        } catch (Exception e) {
            log.error("ZIP analysis id={} failed", analysisId, e);
            updateStatus(analysisId, AnalysisStatus.FAILED, null, e.getMessage());
        }
    }

    @Async
    public void processProfileAnalysis(Long analysisId, Long tenantId, Long profileId) {
        log.info("Processing profile analysis id={}", analysisId);

        try {
            // Update status to IN_PROGRESS
            updateStatus(analysisId, AnalysisStatus.IN_PROGRESS, null, null);

            // Collect profile data
            Map<ProfileSection, String> sections = profileDataCollectorService.collectProfileData(profileId, tenantId);

            // Check if profile has enough data
            boolean hasContent = sections.entrySet().stream()
                    .filter(e -> e.getKey() != ProfileSection.OTHER)
                    .anyMatch(e -> !e.getValue().isBlank());

            if (!hasContent) {
                updateStatus(analysisId, AnalysisStatus.FAILED, null,
                        "Your profile doesn't have enough data to analyze. Please add experience, education, or skills first.");
                return;
            }

            // Build prompt and call LLM with retries
            LlmRequest request = promptService.buildScoringPrompt(sections);
            LlmResponse llmResponse = callLlmWithRetry(request);

            // Parse LLM response
            LinkedInLlmResult result = promptService.parseScoringResponse(llmResponse);

            // Save results
            saveAnalysisResults(analysisId, tenantId, result, sections);

            log.info("Profile analysis id={} completed with overallScore={}", analysisId, result.overallScore());

        } catch (Exception e) {
            log.error("Profile analysis id={} failed", analysisId, e);
            updateStatus(analysisId, AnalysisStatus.FAILED, null, e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public LinkedInAnalysisResponse getAnalysis(Long analysisId, Long tenantId) {
        LinkedInAnalysis analysis = analysisRepository.findByIdAndTenantId(analysisId, tenantId)
                .orElseThrow(() -> new AnalysisNotFoundException(analysisId));

        List<LinkedInSectionScore> sections = Collections.emptyList();
        if (AnalysisStatus.COMPLETED.name().equals(analysis.getStatus())) {
            sections = sectionScoreRepository.findByAnalysisIdOrderBySectionName(analysisId);
        }

        return LinkedInAnalysisMapper.toResponse(analysis, sections);
    }

    @Transactional(readOnly = true)
    public Page<LinkedInAnalysisSummary> getAnalysisHistory(Long profileId, Long tenantId, Pageable pageable) {
        return analysisRepository
                .findByProfileIdAndTenantIdOrderByCreatedAtDesc(profileId, tenantId, pageable)
                .map(LinkedInAnalysisMapper::toSummary);
    }

    @Transactional
    public SectionScoreResponse generateAdditionalSuggestions(Long analysisId, String sectionName,
                                                              int count, Long tenantId) {
        LinkedInAnalysis analysis = analysisRepository.findByIdAndTenantId(analysisId, tenantId)
                .orElseThrow(() -> new AnalysisNotFoundException(analysisId));

        if (!AnalysisStatus.COMPLETED.name().equals(analysis.getStatus())) {
            throw new AnalysisNotCompletedException(analysisId);
        }

        LinkedInSectionScore sectionScore = sectionScoreRepository
                .findByAnalysisIdAndSectionName(analysisId, sectionName)
                .orElseThrow(() -> new AnalysisNotFoundException(
                        "Section " + sectionName + " not found for analysis " + analysisId));

        // Calculate cost and spend coins
        int totalCost = count * suggestionCostPerItem;
        CoinTransaction spendTx = coinWalletService.spend(
                tenantId, totalCost, RefType.LINKEDIN_SUGGESTION,
                String.valueOf(sectionScore.getId()),
                "LinkedIn suggestions for " + sectionName + " section"
        );

        try {
            // Generate suggestions via LLM
            LlmRequest request = promptService.buildAdditionalSuggestionsPrompt(
                    sectionName, sectionScore.getRawContent(),
                    sectionScore.getSuggestions(), count);
            LlmResponse llmResponse = callLlmWithRetry(request);
            List<String> newSuggestions = promptService.parseAdditionalSuggestions(llmResponse);

            // Append new suggestions
            List<String> allSuggestions = new ArrayList<>(
                    sectionScore.getSuggestions() != null ? sectionScore.getSuggestions() : new ArrayList<>());
            allSuggestions.addAll(newSuggestions);
            sectionScore.setSuggestions(allSuggestions);
            sectionScoreRepository.save(sectionScore);

            log.info("Generated {} additional suggestions for analysis={}, section={}",
                    newSuggestions.size(), analysisId, sectionName);

            return LinkedInAnalysisMapper.toSectionResponse(sectionScore);

        } catch (Exception e) {
            // Refund coins on failure
            log.error("Failed to generate suggestions, refunding coins", e);
            coinWalletService.refund(tenantId, spendTx.getId());
            throw new RuntimeException("Failed to generate additional suggestions: " + e.getMessage(), e);
        }
    }

    @Transactional
    public void applySuggestion(Long analysisId, String sectionName, int suggestionIndex, Long tenantId) {
        LinkedInAnalysis analysis = analysisRepository.findByIdAndTenantId(analysisId, tenantId)
                .orElseThrow(() -> new AnalysisNotFoundException(analysisId));

        if (!AnalysisStatus.COMPLETED.name().equals(analysis.getStatus())) {
            throw new AnalysisNotCompletedException(analysisId);
        }

        ProfileSection section;
        try {
            section = ProfileSection.valueOf(sectionName);
        } catch (IllegalArgumentException e) {
            throw new SectionNotApplicableException(sectionName);
        }

        if (!section.canApplyToProfile()) {
            throw new SectionNotApplicableException(sectionName);
        }

        LinkedInSectionScore sectionScore = sectionScoreRepository
                .findByAnalysisIdAndSectionName(analysisId, sectionName)
                .orElseThrow(() -> new AnalysisNotFoundException(
                        "Section " + sectionName + " not found for analysis " + analysisId));

        List<String> suggestions = sectionScore.getSuggestions();
        if (suggestions == null || suggestionIndex >= suggestions.size()) {
            throw new ValidationException("suggestionIndex", "Invalid suggestion index: " + suggestionIndex);
        }

        String suggestionText = suggestions.get(suggestionIndex);

        // Update profile
        Profile profile = profileRepository.findByIdAndTenantId(analysis.getProfileId(), tenantId)
                .orElseThrow(() -> new ProfileNotFoundException(analysis.getProfileId()));

        switch (section) {
            case HEADLINE -> profile.setHeadline(suggestionText);
            case ABOUT -> profile.setSummary(suggestionText);
            default -> throw new SectionNotApplicableException(sectionName);
        }

        profileRepository.save(profile);
        log.info("Applied suggestion to profile: analysisId={}, section={}, profileId={}",
                analysisId, sectionName, profile.getId());
    }

    // --- Private helpers ---

    private void validatePdf(MultipartFile pdf) {
        if (pdf == null || pdf.isEmpty()) {
            throw new InvalidPdfException("PDF file is required and must not be empty");
        }

        String filename = pdf.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".pdf")) {
            throw new InvalidPdfException("Only PDF files are accepted");
        }

        String contentType = pdf.getContentType();
        if (contentType != null && !contentType.equals("application/pdf")) {
            throw new InvalidPdfException("Only PDF files are accepted");
        }
    }

    private void validateZip(MultipartFile zip) {
        if (zip == null || zip.isEmpty()) {
            throw new ValidationException("file", "ZIP file is required and must not be empty");
        }

        String filename = zip.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".zip")) {
            throw new ValidationException("file", "Only ZIP files are accepted");
        }
    }

    private LlmResponse callLlmWithRetry(LlmRequest request) {
        Exception lastException = null;

        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            try {
                return llmClient.complete(request);
            } catch (Exception e) {
                lastException = e;
                log.warn("LLM call attempt {}/{} failed: {}", attempt + 1, MAX_RETRIES, e.getMessage());

                if (attempt < MAX_RETRIES - 1) {
                    try {
                        Thread.sleep(RETRY_DELAYS_MS[attempt]);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Interrupted during LLM retry", ie);
                    }
                }
            }
        }

        throw new RuntimeException("LLM call failed after " + MAX_RETRIES + " attempts", lastException);
    }

    @Transactional
    public void updateStatus(Long analysisId, AnalysisStatus status, Integer overallScore, String errorMessage) {
        LinkedInAnalysis analysis = analysisRepository.findById(analysisId)
                .orElseThrow(() -> new AnalysisNotFoundException(analysisId));

        analysis.setStatus(status.name());
        if (overallScore != null) {
            analysis.setOverallScore(overallScore);
        }
        if (errorMessage != null) {
            analysis.setErrorMessage(errorMessage);
        }
        if (status == AnalysisStatus.COMPLETED) {
            analysis.setAnalyzedAt(Instant.now());
        }

        analysisRepository.save(analysis);
    }

    @Transactional
    public void saveAnalysisResults(Long analysisId, Long tenantId,
                                     LinkedInLlmResult result, Map<ProfileSection, String> rawSections) {
        // Save section scores
        for (LinkedInLlmResult.SectionResult sectionResult : result.sections()) {
            LinkedInSectionScore score = new LinkedInSectionScore();
            score.setTenantId(tenantId);
            score.setAnalysisId(analysisId);
            score.setSectionName(sectionResult.sectionName());
            score.setSectionScore(sectionResult.score());
            score.setQualityExplanation(sectionResult.explanation());
            score.setSuggestions(List.of(sectionResult.suggestion()));

            // Try to get raw content for this section
            try {
                ProfileSection section = ProfileSection.valueOf(sectionResult.sectionName());
                score.setRawContent(rawSections.getOrDefault(section, ""));
            } catch (IllegalArgumentException e) {
                score.setRawContent("");
            }

            sectionScoreRepository.save(score);
        }

        // Update analysis with overall score and COMPLETED status
        updateStatus(analysisId, AnalysisStatus.COMPLETED, result.overallScore(), null);
    }

    private void deleteTempFile(Path path) {
        try {
            Files.deleteIfExists(path);
            log.debug("Deleted temp file: {}", path);
        } catch (IOException e) {
            log.warn("Failed to delete temp file: {}", path, e);
        }
    }
}
