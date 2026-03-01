package com.interviewme.linkedin;

import com.interviewme.common.util.TenantContext;
import com.interviewme.linkedin.dto.*;
import com.interviewme.linkedin.service.LinkedInZipParserService;
import com.interviewme.model.ImportStrategy;
import com.interviewme.model.LinkedInImport;
import com.interviewme.model.Profile;
import com.interviewme.model.User;
import com.interviewme.repository.LinkedInImportRepository;
import com.interviewme.repository.ProfileRepository;
import com.interviewme.util.SlugGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/v1/linkedin/import")
@RequiredArgsConstructor
@Slf4j
public class LinkedInImportController {

    private final LinkedInZipParserService zipParserService;
    private final LinkedInImportMappingService mappingService;
    private final LinkedInImportRepository importRepository;
    private final ProfileRepository profileRepository;

    private final ConcurrentHashMap<String, LinkedInImportData> previewCache = new ConcurrentHashMap<>();

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadAndPreview(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        Long tenantId = TenantContext.getCurrentTenantId();
        log.info("POST /api/v1/linkedin/import/upload - tenantId={}, filename={}", tenantId, file.getOriginalFilename());

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "File is empty"));
        }

        try {
            LinkedInImportData data = zipParserService.parseZip(file.getInputStream());

            String previewId = UUID.randomUUID().toString();
            previewCache.put(previewId, data);

            Map<String, Integer> counts = new LinkedHashMap<>();
            counts.put("jobs", data.positions() != null ? data.positions().size() : 0);
            counts.put("education", data.education() != null ? data.education().size() : 0);
            counts.put("skills", data.skills() != null ? data.skills().size() : 0);
            counts.put("languages", data.languages() != null ? data.languages().size() : 0);
            counts.put("projects", data.projects() != null ? data.projects().size() : 0);
            counts.put("certifications", data.certifications() != null ? data.certifications().size() : 0);

            ImportPreviewResponse response = new ImportPreviewResponse(
                    previewId, counts, data.warnings(), data.profile());

            return ResponseEntity.ok(response);
        } catch (IOException e) {
            log.error("Failed to read uploaded file", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to read uploaded file"));
        }
    }

    @PostMapping("/confirm")
    @Transactional
    public ResponseEntity<?> confirmImport(
            @RequestBody ConfirmImportRequest request,
            Authentication authentication) {
        Long tenantId = TenantContext.getCurrentTenantId();
        User user = (User) authentication.getPrincipal();
        log.info("POST /api/v1/linkedin/import/confirm - tenantId={}, previewId={}", tenantId, request.previewId());

        LinkedInImportData data = previewCache.get(request.previewId());
        if (data == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Preview not found or expired. Please upload the file again."));
        }

        Long profileId = profileRepository.findByUserIdAndDeletedAtIsNull(user.getId())
                .map(p -> p.getId())
                .orElse(null);

        if (profileId == null) {
            log.info("No profile found for user={}, auto-creating from LinkedIn data", user.getId());
            Profile profile = createProfileFromLinkedIn(tenantId, user, data);
            profileId = profile.getId();
        }

        ImportStrategy strategy = request.importStrategy() != null ? request.importStrategy() : ImportStrategy.MERGE;

        LinkedInImport importRecord = mappingService.executeImport(
                tenantId, profileId, data, strategy, "linkedin-export.zip");

        // Only remove from cache after successful import
        previewCache.remove(request.previewId());

        ImportResultResponse response = new ImportResultResponse(
                importRecord.getId(),
                importRecord.getStatus(),
                importRecord.getItemCounts(),
                importRecord.getErrors());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/history")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getImportHistory(Authentication authentication) {
        Long tenantId = TenantContext.getCurrentTenantId();
        User user = (User) authentication.getPrincipal();
        log.info("GET /api/v1/linkedin/import/history - tenantId={}", tenantId);

        Long profileId = profileRepository.findByUserIdAndDeletedAtIsNull(user.getId())
                .map(p -> p.getId())
                .orElse(null);

        if (profileId == null) {
            return ResponseEntity.ok(List.of());
        }

        List<LinkedInImport> imports = importRepository.findByTenantIdAndProfileIdOrderByCreatedAtDesc(tenantId, profileId);

        List<ImportHistoryResponse> history = imports.stream()
                .map(i -> new ImportHistoryResponse(
                        i.getId(),
                        i.getFilename(),
                        i.getImportStrategy(),
                        i.getStatus(),
                        i.getItemCounts(),
                        i.getImportedAt()))
                .toList();

        return ResponseEntity.ok(history);
    }

    private Profile createProfileFromLinkedIn(Long tenantId, User user, LinkedInImportData data) {
        LinkedInProfileData profileData = data.profile();

        String fullName = user.getEmail().split("@")[0]; // fallback
        if (profileData != null) {
            String linkedInName = ((profileData.firstName() != null ? profileData.firstName() : "") + " " +
                    (profileData.lastName() != null ? profileData.lastName() : "")).trim();
            if (!linkedInName.isBlank()) {
                fullName = linkedInName;
            }
        }

        String headline = profileData != null && profileData.headline() != null
                ? profileData.headline() : "Professional";

        String slug = SlugGenerator.generateUniqueSlug(fullName, profileRepository::existsBySlugGlobally);

        Profile profile = new Profile();
        profile.setTenantId(tenantId);
        profile.setUserId(user.getId());
        profile.setFullName(fullName);
        profile.setHeadline(headline);
        profile.setSummary(profileData != null ? profileData.summary() : null);
        profile.setLocation(profileData != null ? profileData.location() : null);
        profile.setSlug(slug);
        profile.setDefaultVisibility("public");

        Profile saved = profileRepository.save(profile);
        log.info("Auto-created profile id={} slug={} for user={}", saved.getId(), slug, user.getId());
        return saved;
    }
}
