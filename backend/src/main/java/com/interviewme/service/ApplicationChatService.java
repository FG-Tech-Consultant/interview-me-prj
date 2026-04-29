package com.interviewme.service;

import com.interviewme.aichat.client.LlmChatMessage;
import com.interviewme.aichat.client.LlmResponse;
import com.interviewme.aichat.config.AiProperties;
import com.interviewme.aichat.model.*;
import com.interviewme.aichat.repository.ChatMessageRepository;
import com.interviewme.aichat.repository.ChatSessionRepository;
import com.interviewme.aichat.service.LlmRouterService;
import com.interviewme.dto.job.ApplicationChatRequest;
import com.interviewme.dto.job.ApplicationChatResponse;
import com.interviewme.dto.job.LinkedInUploadResponse;
import com.interviewme.linkedin.model.ProfileSection;
import com.interviewme.linkedin.service.LinkedInPdfParserService;
import com.interviewme.model.*;
import com.interviewme.repository.JobApplicationRepository;
import com.interviewme.repository.JobPostingRepository;
import com.interviewme.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApplicationChatService {

    private static final Pattern NAME_PATTERN = Pattern.compile("\\[NAME:([^]]*)]");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("\\[EMAIL:([^]]*)]");
    private static final Pattern SKILLS_PATTERN = Pattern.compile("\\[SKILLS:([^]]*)]");
    private static final Pattern SUMMARY_PATTERN = Pattern.compile("\\[SUMMARY:([^]]*)]");
    private static final Pattern PHASE_PATTERN = Pattern.compile("\\[PHASE:([^]]*)]");
    private static final Pattern COMPLETE_PATTERN = Pattern.compile("\\[APPLICATION_COMPLETE]");
    private static final Pattern FIT_SCORE_PATTERN = Pattern.compile("\\[FIT_SCORE:(\\d+)]");

    private final JobPostingRepository jobPostingRepository;
    private final JobApplicationRepository applicationRepository;
    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;
    private final LlmRouterService llmRouter;
    private final AiProperties aiProperties;
    private final LinkedInPdfParserService linkedInPdfParser;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public ApplicationChatResponse chat(String jobSlug, ApplicationChatRequest request) {
        JobPosting job = findActiveJob(jobSlug);
        Long tenantId = job.getTenantId();

        ChatSession session = getOrCreateSession(tenantId, job.getId(), request.sessionToken());

        enforceRateLimit(session);

        saveMessage(session, ChatMessageRole.USER, request.message(), ChatMessageStatus.DELIVERED);

        List<LlmChatMessage> history = buildHistory(session);
        String systemPrompt = buildApplicationSystemPrompt(job, session);

        var result = llmRouter.completeWithRequest(tenantId, systemPrompt, history);
        LlmResponse llmResponse = result.response();
        String rawContent = llmResponse.content();

        // Parse structured data from response
        String phase = extractTag(PHASE_PATTERN, rawContent, "greeting");
        boolean complete = COMPLETE_PATTERN.matcher(rawContent).find();
        String name = extractTag(NAME_PATTERN, rawContent, null);
        String email = extractTag(EMAIL_PATTERN, rawContent, null);
        String skills = extractTag(SKILLS_PATTERN, rawContent, null);
        String summary = extractTag(SUMMARY_PATTERN, rawContent, null);
        Integer fitScore = extractFitScore(rawContent);

        // Update or create application when we have enough data
        JobApplication application = applicationRepository.findByChatSessionToken(session.getSessionToken())
            .orElse(null);

        if (name != null && email != null) {
            if (application == null) {
                application = new JobApplication();
                application.setTenantId(tenantId);
                application.setJobPostingId(job.getId());
                application.setChatSessionToken(session.getSessionToken());
                application.setStatus(ApplicationStatus.ONBOARDING);
                application.setSource(ApplicationSource.CHATBOT);
            }
            application.setCandidateName(name);
            application.setCandidateEmail(email);
            if (skills != null) {
                application.setMatchedSkills(Arrays.asList(skills.split(",")));
            }
            if (summary != null) {
                application.setCandidateSummary(summary);
            }
            if (fitScore != null) {
                application.setFitScore(fitScore);
            }

            if (complete) {
                application.setStatus(ApplicationStatus.SUBMITTED);
                createUserAccountIfNeeded(application, tenantId);
            }

            applicationRepository.save(application);
        }

        String cleanMessage = cleanResponse(rawContent);

        ChatMessage assistantMsg = saveMessage(session, ChatMessageRole.ASSISTANT,
            cleanMessage, ChatMessageStatus.DELIVERED);
        assistantMsg.setTokensUsed(llmResponse.tokensUsed());
        assistantMsg.setLlmProvider(llmResponse.provider());
        assistantMsg.setLlmModel(llmResponse.model());
        assistantMsg.setLatencyMs((int) llmResponse.latencyMs());
        messageRepository.save(assistantMsg);

        session.setLastMessageAt(Instant.now());
        session.setMessageCount(session.getMessageCount() + 2);
        sessionRepository.save(session);

        log.info("Application chat jobSlug={} tenantId={} phase={} complete={}",
            jobSlug, tenantId, phase, complete);

        ApplicationChatResponse.ApplicationSummary appSummary = null;
        if (complete && application != null) {
            appSummary = new ApplicationChatResponse.ApplicationSummary(
                application.getCandidateName(),
                application.getCandidateEmail(),
                application.getFitScore(),
                application.getMatchedSkills(),
                application.getGapSkills(),
                summary
            );
        }

        return new ApplicationChatResponse(cleanMessage, session.getSessionToken(),
            phase, complete, appSummary);
    }

    @Transactional
    public LinkedInUploadResponse uploadLinkedInPdf(String jobSlug, MultipartFile file, UUID sessionToken) {
        JobPosting job = findActiveJob(jobSlug);
        Long tenantId = job.getTenantId();

        // Save PDF to temp file
        Path tempFile;
        try {
            tempFile = Files.createTempFile("linkedin-", ".pdf");
            file.transferTo(tempFile.toFile());
        } catch (IOException e) {
            log.error("Failed to save LinkedIn PDF", e);
            return new LinkedInUploadResponse(false, sessionToken, null, null, null, null,
                "Failed to process PDF file");
        }

        try {
            // Parse PDF
            Map<ProfileSection, String> sections = linkedInPdfParser.parse(tempFile);

            String aboutText = sections.getOrDefault(ProfileSection.ABOUT, "");
            String experienceText = sections.getOrDefault(ProfileSection.EXPERIENCE, "");
            String skillsText = sections.getOrDefault(ProfileSection.SKILLS, "");

            // Use LLM to extract structured data from PDF content
            String extractionPrompt = buildLinkedInExtractionPrompt(aboutText, experienceText, skillsText, job);
            var result = llmRouter.completeWithRequest(tenantId, extractionPrompt,
                List.of(new LlmChatMessage("user", "Extract the candidate data from the LinkedIn profile above.")));
            String extractedContent = result.response().content();

            String name = extractTag(NAME_PATTERN, extractedContent, "Unknown");
            String headline = extractTag(Pattern.compile("\\[HEADLINE:([^]]*)]"), extractedContent, "");
            String skills = extractTag(SKILLS_PATTERN, extractedContent, "");
            String summary = extractTag(SUMMARY_PATTERN, extractedContent, aboutText);

            List<String> extractedSkills = skills.isEmpty() ? List.of() :
                Arrays.stream(skills.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();

            // Create or update session + application
            ChatSession session = getOrCreateSession(tenantId, job.getId(), sessionToken);

            JobApplication application = applicationRepository.findByChatSessionToken(session.getSessionToken())
                .orElse(new JobApplication());

            application.setTenantId(tenantId);
            application.setJobPostingId(job.getId());
            application.setChatSessionToken(session.getSessionToken());
            application.setCandidateName(name);
            application.setCandidateEmail("");
            application.setStatus(ApplicationStatus.ONBOARDING);
            application.setSource(ApplicationSource.LINKEDIN_IMPORT);
            application.setMatchedSkills(extractedSkills);
            application.setCandidateSummary(summary);
            application.setLinkedinPdfPath(tempFile.toString());
            applicationRepository.save(application);

            // Save a system message noting the LinkedIn import
            saveMessage(session, ChatMessageRole.ASSISTANT,
                "LinkedIn profile imported successfully! I found your name (" + name +
                ") and " + extractedSkills.size() + " skills. " +
                "Now I just need your email to complete the application. What's your email address?",
                ChatMessageStatus.DELIVERED);

            session.setLastMessageAt(Instant.now());
            session.setMessageCount(session.getMessageCount() + 1);
            sessionRepository.save(session);

            return new LinkedInUploadResponse(true, session.getSessionToken(), name, headline,
                extractedSkills, summary,
                "LinkedIn profile imported. Please provide your email to complete the application.");

        } finally {
            try {
                Files.deleteIfExists(tempFile);
            } catch (IOException ignored) {
                // best effort cleanup
            }
        }
    }

    private JobPosting findActiveJob(String slug) {
        return jobPostingRepository.findBySlugAndStatusAndDeletedAtIsNull(slug, JobPostingStatus.ACTIVE)
            .orElseThrow(() -> new EntityNotFoundException("Job posting not found: " + slug));
    }

    private String buildApplicationSystemPrompt(JobPosting job, ChatSession session) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are a friendly AI career assistant guiding a candidate through applying for a job.\n\n");
        sb.append("JOB: ").append(job.getTitle()).append("\n");
        sb.append("Description: ").append(job.getDescription()).append("\n");
        if (job.getRequiredSkills() != null && !job.getRequiredSkills().isEmpty()) {
            sb.append("Required skills: ").append(String.join(", ", job.getRequiredSkills())).append("\n");
        }
        if (job.getExperienceLevel() != null) {
            sb.append("Level: ").append(job.getExperienceLevel()).append("\n");
        }

        // Check if we already have an application with LinkedIn data
        Optional<JobApplication> existingApp = applicationRepository.findByChatSessionToken(session.getSessionToken());
        if (existingApp.isPresent()) {
            JobApplication app = existingApp.get();
            sb.append("\nCANDIDATE DATA ALREADY COLLECTED:\n");
            if (app.getCandidateName() != null) sb.append("- Name: ").append(app.getCandidateName()).append("\n");
            if (app.getCandidateEmail() != null && !app.getCandidateEmail().isEmpty())
                sb.append("- Email: ").append(app.getCandidateEmail()).append("\n");
            if (app.getMatchedSkills() != null)
                sb.append("- Skills: ").append(String.join(", ", app.getMatchedSkills())).append("\n");
            if (app.getCandidateSummary() != null)
                sb.append("- Summary: ").append(app.getCandidateSummary()).append("\n");
            if (app.getSource() == ApplicationSource.LINKEDIN_IMPORT)
                sb.append("- Source: LinkedIn PDF import\n");
        }

        sb.append("\nINSTRUCTIONS:\n");
        sb.append("Guide the candidate step-by-step to apply. Collect:\n");
        sb.append("1. Full name (if not already known)\n");
        sb.append("2. Email address (if not already known)\n");
        sb.append("3. Brief about their experience/skills relevant to this job\n");
        sb.append("4. Calculate a fit score when you have enough info\n\n");
        sb.append("Be conversational, warm, and concise. Ask ONE question at a time.\n");
        sb.append("Respond in the same language as the candidate.\n\n");
        sb.append("TAGS (append at END of message, hidden from user):\n");
        sb.append("[PHASE:greeting|collecting_name|collecting_email|collecting_experience|scoring|complete]\n");
        sb.append("[NAME:full name] — when you learn the candidate's name\n");
        sb.append("[EMAIL:email@example.com] — when you learn the email\n");
        sb.append("[SKILLS:skill1,skill2,...] — matched skills from conversation\n");
        sb.append("[SUMMARY:brief summary] — candidate summary\n");
        sb.append("[FIT_SCORE:XX] — fit score 0-100 when ready\n");
        sb.append("[APPLICATION_COMPLETE] — when all data collected and candidate confirms\n");

        return sb.toString();
    }

    private String buildLinkedInExtractionPrompt(String about, String experience, String skills, JobPosting job) {
        return "Extract structured data from this LinkedIn profile.\n\n" +
            "ABOUT:\n" + about + "\n\n" +
            "EXPERIENCE:\n" + experience + "\n\n" +
            "SKILLS:\n" + skills + "\n\n" +
            "JOB THEY'RE APPLYING TO: " + job.getTitle() + "\n" +
            "Required skills: " + (job.getRequiredSkills() != null ? String.join(", ", job.getRequiredSkills()) : "N/A") + "\n\n" +
            "Output EXACTLY these tags:\n" +
            "[NAME:full name from profile]\n" +
            "[HEADLINE:their headline]\n" +
            "[SKILLS:skill1,skill2,...] — skills that match the job requirements\n" +
            "[SUMMARY:2-3 sentence summary of candidate's fit for this role]\n";
    }

    private void createUserAccountIfNeeded(JobApplication application, Long tenantId) {
        if (application.getCandidateEmail() == null || application.getCandidateEmail().isEmpty()) return;
        if (userRepository.existsByEmail(application.getCandidateEmail())) {
            User existing = userRepository.findByEmail(application.getCandidateEmail()).orElse(null);
            if (existing != null) {
                application.setUserId(existing.getId());
            }
            return;
        }

        User user = new User();
        user.setEmail(application.getCandidateEmail());
        user.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
        user.setTenantId(tenantId);
        user.setRole("CANDIDATE");
        user = userRepository.save(user);
        application.setUserId(user.getId());

        log.info("Created candidate account for {} userId={}", application.getCandidateEmail(), user.getId());
    }

    private ChatSession getOrCreateSession(Long tenantId, Long jobId, UUID sessionToken) {
        if (sessionToken != null) {
            Optional<ChatSession> existing = sessionRepository.findBySessionTokenAndStatus(
                sessionToken, ChatSessionStatus.ACTIVE);
            if (existing.isPresent()) {
                ChatSession session = existing.get();
                if (session.getLastMessageAt() != null &&
                    session.getLastMessageAt().isBefore(
                        Instant.now().minus(aiProperties.getChat().getSessionExpiryHours(), ChronoUnit.HOURS))) {
                    session.setStatus(ChatSessionStatus.EXPIRED);
                    sessionRepository.save(session);
                } else {
                    return session;
                }
            }
        }

        ChatSession session = new ChatSession();
        session.setTenantId(tenantId);
        session.setProfileId(jobId);
        session.setSessionToken(UUID.randomUUID());
        session.setStatus(ChatSessionStatus.ACTIVE);
        return sessionRepository.save(session);
    }

    private void enforceRateLimit(ChatSession session) {
        Instant oneMinuteAgo = Instant.now().minus(1, ChronoUnit.MINUTES);
        long recentCount = messageRepository.countBySessionIdAndRoleAndCreatedAtAfter(
            session.getId(), ChatMessageRole.USER, oneMinuteAgo);
        if (recentCount >= aiProperties.getChat().getRateLimitPerMinute()) {
            throw new com.interviewme.aichat.exception.ChatRateLimitException(
                "Rate limit exceeded. Max " + aiProperties.getChat().getRateLimitPerMinute() + " messages per minute.");
        }
    }

    private ChatMessage saveMessage(ChatSession session, ChatMessageRole role,
                                     String content, ChatMessageStatus status) {
        ChatMessage msg = new ChatMessage();
        msg.setTenantId(session.getTenantId());
        msg.setSessionId(session.getId());
        msg.setRole(role);
        msg.setContent(content);
        msg.setStatus(status);
        return messageRepository.save(msg);
    }

    private List<LlmChatMessage> buildHistory(ChatSession session) {
        List<ChatMessage> recent = messageRepository.findTop10BySessionIdOrderByCreatedAtDesc(session.getId());
        List<LlmChatMessage> history = new ArrayList<>();
        for (int i = recent.size() - 1; i >= 0; i--) {
            ChatMessage msg = recent.get(i);
            if (msg.getStatus() == ChatMessageStatus.FAILED) continue;
            String role = msg.getRole() == ChatMessageRole.USER ? "user" : "assistant";
            history.add(new LlmChatMessage(role, msg.getContent()));
        }
        return history;
    }

    private static String extractTag(Pattern pattern, String text, String defaultValue) {
        if (text == null) return defaultValue;
        Matcher m = pattern.matcher(text);
        if (m.find()) return m.group(1).trim();
        return defaultValue;
    }

    private static Integer extractFitScore(String text) {
        if (text == null) return null;
        Matcher m = FIT_SCORE_PATTERN.matcher(text);
        if (!m.find()) return null;
        int score = Integer.parseInt(m.group(1));
        return Math.min(100, Math.max(0, score));
    }

    static String cleanResponse(String response) {
        if (response == null) return null;
        return response
            .replaceAll("\\[PHASE:[^]]*]", "")
            .replaceAll("\\[NAME:[^]]*]", "")
            .replaceAll("\\[EMAIL:[^]]*]", "")
            .replaceAll("\\[SKILLS:[^]]*]", "")
            .replaceAll("\\[SUMMARY:[^]]*]", "")
            .replaceAll("\\[FIT_SCORE:\\d+]", "")
            .replaceAll("\\[HEADLINE:[^]]*]", "")
            .replaceAll("\\[APPLICATION_COMPLETE]", "")
            .trim();
    }
}
