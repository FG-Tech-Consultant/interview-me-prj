package com.interviewme.service;

import com.interviewme.billing.service.CoinWalletService;
import com.interviewme.exports.config.ExportProperties;
import com.interviewme.exports.service.FileStorageService;
import com.interviewme.exports.service.PdfGenerationService;
import com.interviewme.exports.event.ExportCompletedEvent;
import com.interviewme.exports.model.ExportHistory;
import com.interviewme.exports.model.ExportStatus;
import com.interviewme.exports.model.ExportType;
import com.interviewme.exports.repository.ExportHistoryRepository;
import com.interviewme.model.Education;
import com.interviewme.model.JobExperience;
import com.interviewme.model.Profile;
import com.interviewme.repository.EducationRepository;
import com.interviewme.repository.JobExperienceRepository;
import com.interviewme.repository.ProfileRepository;
import com.interviewme.model.UserSkill;
import com.interviewme.repository.UserSkillRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExportJobService {

    private final ExportHistoryRepository exportHistoryRepository;
    private final ProfileRepository profileRepository;
    private final JobExperienceRepository jobExperienceRepository;
    private final EducationRepository educationRepository;
    private final UserSkillRepository userSkillRepository;
    private final PdfGenerationService pdfGenerationService;
    private final FileStorageService fileStorageService;
    private final CoinWalletService coinWalletService;
    private final ApplicationEventPublisher eventPublisher;
    private final ExportProperties exportProperties;

    @Async("exportTaskExecutor")
    public void processExport(Long exportHistoryId) {
        log.info("Starting export job: exportId={}", exportHistoryId);
        ExportHistory export = exportHistoryRepository.findById(exportHistoryId)
                .orElseThrow(() -> new RuntimeException("Export not found: " + exportHistoryId));

        int maxRetries = exportProperties.getRetry().getMaxAttempts();

        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                updateStatus(export, ExportStatus.IN_PROGRESS, attempt);

                // Load profile data
                Profile profile = profileRepository.findById(export.getProfileId())
                        .orElseThrow(() -> new RuntimeException("Profile not found: " + export.getProfileId()));

                // Build type-specific template context
                Map<String, Object> context = buildTemplateContext(export, profile);

                // Generate PDF
                byte[] pdfBytes = pdfGenerationService.generatePdf(
                        export.getTemplate().getTemplateFile(), context);

                // Store file
                String filePrefix = ExportType.COVER_LETTER.name().equals(export.getType())
                        ? "cover-letter" : "resume";
                String fileName = filePrefix + "-" + export.getId() + ".pdf";
                String fileUrl = fileStorageService.store(export.getTenantId(), fileName, pdfBytes);

                // Update success
                export.setStatus(ExportStatus.COMPLETED.name());
                export.setFileUrl(fileUrl);
                export.setFileSizeBytes((long) pdfBytes.length);
                export.setCompletedAt(Instant.now());
                exportHistoryRepository.save(export);

                // Publish event
                eventPublisher.publishEvent(new ExportCompletedEvent(
                        export.getTenantId(), export.getId(), export.getType(),
                        fileUrl, export.getCoinsSpent(), Instant.now()
                ));

                log.info("Export completed: exportId={}, duration={}ms, size={} bytes",
                        exportHistoryId,
                        Duration.between(export.getCreatedAt(), Instant.now()).toMillis(),
                        pdfBytes.length);
                return;

            } catch (Exception e) {
                log.warn("Export attempt {} failed: exportId={}, error={}",
                        attempt + 1, exportHistoryId, e.getMessage());

                if (attempt == maxRetries) {
                    handleFailure(export,
                            "Export failed after " + (maxRetries + 1) + " attempts: " + e.getMessage());
                    return;
                }

                // Exponential backoff
                long delayMs = (long) Math.pow(exportProperties.getRetry().getMultiplier(), attempt)
                        * exportProperties.getRetry().getInitialDelayMs();
                try {
                    Thread.sleep(delayMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    handleFailure(export, "Export interrupted");
                    return;
                }
            }
        }
    }

    private Map<String, Object> buildTemplateContext(ExportHistory export, Profile profile) {
        Map<String, Object> context = new HashMap<>();
        context.put("profile", profile);
        context.put("generatedDate", LocalDate.now().toString());

        List<UserSkill> userSkills = userSkillRepository
                .findByProfileIdAndDeletedAtIsNullOrderBySkill_CategoryAscSkill_NameAsc(profile.getId());

        if (ExportType.COVER_LETTER.name().equals(export.getType())) {
            // Cover letter context
            context.put("targetCompany", export.getParameters().get("targetCompany"));
            context.put("targetRole", export.getParameters().get("targetRole"));
            context.put("jobDescription", export.getParameters().get("jobDescription"));
            context.put("market", export.getParameters().get("market"));

            // Top skills (up to 8, sorted by proficiency)
            List<Map<String, Object>> topSkills = userSkills.stream()
                    .filter(us -> us.getSkill() != null)
                    .sorted((a, b) -> Integer.compare(
                            b.getProficiencyDepth() != null ? b.getProficiencyDepth() : 0,
                            a.getProficiencyDepth() != null ? a.getProficiencyDepth() : 0))
                    .limit(8)
                    .map(this::toSkillMap)
                    .collect(Collectors.toList());
            context.put("topSkills", topSkills);

            // Most recent job
            List<JobExperience> jobs = jobExperienceRepository
                    .findByProfileIdAndDeletedAtIsNullOrderByStartDateDesc(profile.getId());
            if (!jobs.isEmpty()) {
                context.put("recentJob", jobs.get(0));
            }
        } else {
            // Resume context
            List<JobExperience> jobs = jobExperienceRepository
                    .findByProfileIdAndDeletedAtIsNullOrderByStartDateDesc(profile.getId());
            List<Education> educationList = educationRepository
                    .findByProfileIdAndDeletedAtIsNullOrderByEndDateDesc(profile.getId());

            Map<String, List<Map<String, Object>>> skillsByCategory = userSkills.stream()
                    .filter(us -> us.getSkill() != null)
                    .collect(Collectors.groupingBy(
                            us -> us.getSkill().getCategory() != null ? us.getSkill().getCategory() : "Other",
                            LinkedHashMap::new,
                            Collectors.mapping(this::toSkillMap, Collectors.toList())
                    ));

            context.put("jobExperiences", jobs);
            context.put("education", educationList);
            context.put("skills", skillsByCategory);
            context.put("targetRole", export.getParameters().get("targetRole"));
            context.put("location", export.getParameters().get("location"));
            context.put("seniority", export.getParameters().get("seniority"));
            context.put("language", export.getParameters().get("language"));
        }

        return context;
    }

    private void updateStatus(ExportHistory export, ExportStatus status, int retryCount) {
        export.setStatus(status.name());
        export.setRetryCount(retryCount);
        exportHistoryRepository.save(export);
    }

    private void handleFailure(ExportHistory export, String errorMessage) {
        export.setStatus(ExportStatus.FAILED.name());
        export.setErrorMessage(errorMessage);
        exportHistoryRepository.save(export);

        // Auto-refund coins
        if (export.getCoinTransactionId() != null) {
            try {
                coinWalletService.refund(export.getTenantId(), export.getCoinTransactionId());
                log.info("Auto-refunded {} coins for failed export: exportId={}",
                        export.getCoinsSpent(), export.getId());
            } catch (Exception e) {
                log.error("Failed to refund coins for export: exportId={}, error={}",
                        export.getId(), e.getMessage());
            }
        }
    }

    private Map<String, Object> toSkillMap(UserSkill userSkill) {
        Map<String, Object> map = new HashMap<>();
        map.put("skillName", userSkill.getSkill().getName());
        map.put("yearsOfExperience", userSkill.getYearsOfExperience());
        map.put("proficiencyDepth", userSkill.getProficiencyDepth());
        return map;
    }
}
