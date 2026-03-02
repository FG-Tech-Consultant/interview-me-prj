package com.interviewme.controller;

import com.interviewme.common.util.TenantContext;
import com.interviewme.dto.admin.AccountResponse;
import com.interviewme.dto.admin.GlobalStatsResponse;
import com.interviewme.dto.visitor.*;
import com.interviewme.event.ContentChangedEventListener;
import com.interviewme.model.*;
import com.interviewme.repository.*;
import com.interviewme.service.EmailNotificationService;
import com.interviewme.service.VisitorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminController {

    private final VisitorService visitorService;
    private final EmailNotificationService emailNotificationService;
    private final ProfileRepository profileRepository;
    private final JobExperienceRepository jobExperienceRepository;
    private final EducationRepository educationRepository;
    private final UserSkillRepository userSkillRepository;
    private final UserRepository userRepository;
    private final VisitorRepository visitorRepository;
    private final VisitorSessionRepository visitorSessionRepository;
    private final ContentChangedEventListener contentChangedEventListener;

    @GetMapping("/global-stats")
    @Transactional(readOnly = true)
    public ResponseEntity<GlobalStatsResponse> getGlobalStats(
            @AuthenticationPrincipal User user) {
        if (!"ADMIN".equals(user.getRole())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        Long savedTenantId = TenantContext.getTenantId();
        TenantContext.clear();
        try {
            long totalAccounts = userRepository.count();
            long totalProfileViews = profileRepository.sumAllViewCounts();
            long totalVisitors = visitorRepository.countAllVisitors();
            long totalInterviews = visitorSessionRepository.countAllInterviews();

            return ResponseEntity.ok(new GlobalStatsResponse(
                    totalAccounts, totalProfileViews, totalVisitors, totalInterviews));
        } finally {
            TenantContext.setTenantId(savedTenantId);
        }
    }

    @GetMapping("/accounts")
    @Transactional(readOnly = true)
    public ResponseEntity<List<AccountResponse>> getAccounts(
            @AuthenticationPrincipal User user) {
        if (!"ADMIN".equals(user.getRole())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        Long savedTenantId = TenantContext.getTenantId();
        TenantContext.clear();
        try {
            List<User> users = userRepository.findAll();
            List<AccountResponse> accounts = users.stream().map(u -> {
                Profile profile = profileRepository.findByUserIdAndDeletedAtIsNull(u.getId()).orElse(null);
                String fullName = profile != null ? profile.getFullName() : u.getEmail();
                String slug = profile != null ? profile.getSlug() : null;
                String publicProfileUrl = slug != null ? "/p/" + slug : null;
                return new AccountResponse(
                        u.getId(),
                        u.getEmail(),
                        fullName,
                        u.getRole(),
                        slug,
                        publicProfileUrl,
                        u.getCreatedAt()
                );
            }).toList();

            return ResponseEntity.ok(accounts);
        } finally {
            TenantContext.setTenantId(savedTenantId);
        }
    }

    @GetMapping("/visitors")
    @Transactional(readOnly = true)
    public ResponseEntity<Page<VisitorResponse>> getAllVisitors(
            @AuthenticationPrincipal User user,
            Pageable pageable) {
        if (!"ADMIN".equals(user.getRole())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        Long savedTenantId = TenantContext.getTenantId();
        TenantContext.clear();
        try {
            Page<VisitorResponse> visitors = visitorService.getAllVisitors(null, pageable);
            return ResponseEntity.ok(visitors);
        } finally {
            TenantContext.setTenantId(savedTenantId);
        }
    }

    @GetMapping("/visitors/{visitorId}/sessions")
    @Transactional(readOnly = true)
    public ResponseEntity<List<VisitorSessionResponse>> getVisitorSessions(
            @AuthenticationPrincipal User user,
            @PathVariable Long visitorId) {
        if (!"ADMIN".equals(user.getRole())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        Long savedTenantId = TenantContext.getTenantId();
        TenantContext.clear();
        try {
            List<VisitorSessionResponse> sessions = visitorService.getVisitorSessions(visitorId);
            return ResponseEntity.ok(sessions);
        } finally {
            TenantContext.setTenantId(savedTenantId);
        }
    }

    @GetMapping("/sessions/{sessionId}/messages")
    @Transactional(readOnly = true)
    public ResponseEntity<List<VisitorChatLogResponse>> getSessionMessages(
            @AuthenticationPrincipal User user,
            @PathVariable Long sessionId) {
        if (!"ADMIN".equals(user.getRole())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        Long savedTenantId = TenantContext.getTenantId();
        TenantContext.clear();
        try {
            List<VisitorChatLogResponse> messages = visitorService.getSessionMessages(sessionId);
            return ResponseEntity.ok(messages);
        } finally {
            TenantContext.setTenantId(savedTenantId);
        }
    }

    @PostMapping("/embeddings/regenerate")
    @Transactional
    public ResponseEntity<?> regenerateEmbeddings(@AuthenticationPrincipal User user) {
        if (!"ADMIN".equals(user.getRole())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Long savedTenantId = TenantContext.getTenantId();
        TenantContext.clear();
        try {
            List<Profile> profiles = profileRepository.findAll();
            int totalJobs = 0, totalEdu = 0, totalSkills = 0, totalProfiles = 0;

            for (Profile profile : profiles) {
                if (profile.getDeletedAt() != null) continue;

                try { contentChangedEventListener.onProfileChanged(profile); totalProfiles++; }
                catch (Exception e) { log.warn("Profile embedding failed for {}: {}", profile.getId(), e.getMessage()); }

                List<JobExperience> jobs = jobExperienceRepository.findByProfileIdAndDeletedAtIsNullOrderByStartDateDesc(profile.getId());
                for (JobExperience job : jobs) {
                    try { contentChangedEventListener.onJobChanged(job); totalJobs++; }
                    catch (Exception e) { log.warn("Job embedding failed: {}", e.getMessage()); }
                }

                List<Education> edus = educationRepository.findByProfileIdAndDeletedAtIsNullOrderByEndDateDesc(profile.getId());
                for (Education edu : edus) {
                    try { contentChangedEventListener.onEducationChanged(edu); totalEdu++; }
                    catch (Exception e) { log.warn("Edu embedding failed: {}", e.getMessage()); }
                }

                List<UserSkill> skills = userSkillRepository.findByProfileIdAndDeletedAtIsNullOrderBySkill_CategoryAscSkill_NameAsc(profile.getId());
                for (UserSkill skill : skills) {
                    try { contentChangedEventListener.onSkillChanged(skill); totalSkills++; }
                    catch (Exception e) { log.warn("Skill embedding failed: {}", e.getMessage()); }
                }
            }

            log.info("Regenerated embeddings: profiles={}, jobs={}, education={}, skills={}", totalProfiles, totalJobs, totalEdu, totalSkills);
            return ResponseEntity.ok(Map.of(
                    "profiles", totalProfiles,
                    "jobs", totalJobs,
                    "education", totalEdu,
                    "skills", totalSkills
            ));
        } finally {
            TenantContext.setTenantId(savedTenantId);
        }
    }

    @PostMapping("/test-email")
    public ResponseEntity<?> sendTestEmail(
            @AuthenticationPrincipal User user,
            @RequestBody Map<String, String> request) {
        if (!"ADMIN".equals(user.getRole())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        String to = request.get("to");
        if (to == null || to.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email address is required"));
        }
        try {
            emailNotificationService.sendTestEmail(to);
            return ResponseEntity.ok(Map.of("message", "Test email sent to " + to));
        } catch (Exception e) {
            log.error("Failed to send test email to={}: {}", to, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to send email: " + e.getMessage()));
        }
    }
}
