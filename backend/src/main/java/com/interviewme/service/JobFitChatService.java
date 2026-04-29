package com.interviewme.service;

import com.interviewme.aichat.client.LlmChatMessage;
import com.interviewme.aichat.client.LlmResponse;
import com.interviewme.aichat.config.AiProperties;
import com.interviewme.aichat.model.*;
import com.interviewme.aichat.repository.ChatMessageRepository;
import com.interviewme.aichat.repository.ChatSessionRepository;
import com.interviewme.aichat.service.LlmRouterService;
import com.interviewme.dto.job.JobFitChatRequest;
import com.interviewme.dto.job.JobFitChatResponse;
import com.interviewme.model.JobPosting;
import com.interviewme.model.JobPostingStatus;
import com.interviewme.repository.JobPostingRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobFitChatService {

    private static final Pattern FIT_SCORE_PATTERN = Pattern.compile(
        "\\[FIT_SCORE:(\\d+)\\]");
    private static final Pattern MATCHED_SKILLS_PATTERN = Pattern.compile(
        "\\[MATCHED:([^]]*)]");
    private static final Pattern GAP_SKILLS_PATTERN = Pattern.compile(
        "\\[GAPS:([^]]*)]");
    private static final Pattern RECOMMENDATION_PATTERN = Pattern.compile(
        "\\[RECOMMENDATION:([^]]*)]");

    private final JobPostingRepository jobPostingRepository;
    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;
    private final LlmRouterService llmRouter;
    private final AiProperties aiProperties;

    @Transactional
    public JobFitChatResponse chat(String jobSlug, JobFitChatRequest request) {
        // 1. Find active job posting
        JobPosting job = jobPostingRepository.findBySlugAndStatusAndDeletedAtIsNull(
            jobSlug, JobPostingStatus.ACTIVE)
            .orElseThrow(() -> new EntityNotFoundException("Job posting not found: " + jobSlug));

        Long tenantId = job.getTenantId();

        // 2. Get or create session
        ChatSession session = getOrCreateSession(tenantId, job.getId(), request.sessionToken());

        // 3. Rate limit
        enforceRateLimit(session);

        // 4. Save user message
        saveMessage(session, ChatMessageRole.USER, request.message(), ChatMessageStatus.DELIVERED);

        // 5. Build conversation history
        List<LlmChatMessage> history = buildHistory(session);

        // 6. Build system prompt with job context
        String systemPrompt = buildJobFitSystemPrompt(job);

        // 7. Call LLM
        var result = llmRouter.completeWithRequest(tenantId, systemPrompt, history);
        LlmResponse llmResponse = result.response();

        // 8. Parse fit score from response
        String rawContent = llmResponse.content();
        JobFitChatResponse.FitScore fitScore = parseFitScore(rawContent);

        // 9. Clean response (remove score tags)
        String cleanMessage = cleanResponse(rawContent);

        // 10. Save assistant message
        ChatMessage assistantMsg = saveMessage(session, ChatMessageRole.ASSISTANT,
            cleanMessage, ChatMessageStatus.DELIVERED);
        assistantMsg.setTokensUsed(llmResponse.tokensUsed());
        assistantMsg.setLlmProvider(llmResponse.provider());
        assistantMsg.setLlmModel(llmResponse.model());
        assistantMsg.setLatencyMs((int) llmResponse.latencyMs());
        messageRepository.save(assistantMsg);

        // 11. Update session
        session.setLastMessageAt(Instant.now());
        session.setMessageCount(session.getMessageCount() + 2);
        sessionRepository.save(session);

        log.info("JobFit chat jobSlug={} tenantId={} fitScore={} tokens={}",
            jobSlug, tenantId,
            fitScore != null ? fitScore.overallScore() : "N/A",
            llmResponse.tokensUsed());

        return new JobFitChatResponse(cleanMessage, session.getSessionToken(), fitScore);
    }

    @Transactional(readOnly = true)
    public JobPosting getActiveJobBySlug(String slug) {
        return jobPostingRepository.findBySlugAndStatusAndDeletedAtIsNull(slug, JobPostingStatus.ACTIVE)
            .orElseThrow(() -> new EntityNotFoundException("Job posting not found: " + slug));
    }

    private String buildJobFitSystemPrompt(JobPosting job) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are a friendly AI career assistant helping a candidate evaluate their fit for a job position.\n\n");
        sb.append("JOB DETAILS:\n");
        sb.append("- Title: ").append(job.getTitle()).append("\n");
        sb.append("- Description: ").append(job.getDescription()).append("\n");
        if (job.getRequirements() != null) {
            sb.append("- Requirements: ").append(job.getRequirements()).append("\n");
        }
        if (job.getBenefits() != null) {
            sb.append("- Benefits: ").append(job.getBenefits()).append("\n");
        }
        if (job.getLocation() != null) {
            sb.append("- Location: ").append(job.getLocation()).append("\n");
        }
        if (job.getWorkModel() != null) {
            sb.append("- Work model: ").append(job.getWorkModel()).append("\n");
        }
        if (job.getSalaryRange() != null) {
            sb.append("- Salary range: ").append(job.getSalaryRange()).append("\n");
        }
        if (job.getExperienceLevel() != null) {
            sb.append("- Experience level: ").append(job.getExperienceLevel()).append("\n");
        }
        if (job.getRequiredSkills() != null && !job.getRequiredSkills().isEmpty()) {
            sb.append("- Required skills: ").append(String.join(", ", job.getRequiredSkills())).append("\n");
        }
        if (job.getNiceToHaveSkills() != null && !job.getNiceToHaveSkills().isEmpty()) {
            sb.append("- Nice to have: ").append(String.join(", ", job.getNiceToHaveSkills())).append("\n");
        }

        sb.append("\nINSTRUCTIONS:\n");
        sb.append("- Help the candidate understand if this role is a good fit for them.\n");
        sb.append("- Ask about their skills and experience conversationally.\n");
        sb.append("- After gathering enough info (usually 2-3 exchanges), calculate a fit score.\n");
        sb.append("- Explain gaps honestly but constructively — suggest how they could upskill.\n");
        sb.append("- If the fit is good, encourage them to apply.\n");
        sb.append("- Respond in the same language as the candidate's message.\n");
        sb.append("- Be concise and conversational.\n\n");

        sb.append("SCORING (include ONLY when you have enough info to score, after at least 2 exchanges):\n");
        sb.append("When scoring, append these tags at the END of your message (they will be hidden from the user):\n");
        sb.append("[FIT_SCORE:XX] — overall fit 0-100\n");
        sb.append("[MATCHED:skill1,skill2,...] — skills the candidate has that match the job\n");
        sb.append("[GAPS:skill1,skill2,...] — required skills the candidate is missing\n");
        sb.append("[RECOMMENDATION:short recommendation text]\n");
        sb.append("Do NOT include these tags in your first response.\n");

        return sb.toString();
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
        session.setProfileId(jobId); // reuse profileId field for jobId
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

    static JobFitChatResponse.FitScore parseFitScore(String response) {
        if (response == null) return null;

        Matcher scoreMatcher = FIT_SCORE_PATTERN.matcher(response);
        if (!scoreMatcher.find()) return null;

        int score = Integer.parseInt(scoreMatcher.group(1));
        score = Math.min(100, Math.max(0, score));

        List<String> matched = parseSkillList(MATCHED_SKILLS_PATTERN, response);
        List<String> gaps = parseSkillList(GAP_SKILLS_PATTERN, response);

        String recommendation = "";
        Matcher recMatcher = RECOMMENDATION_PATTERN.matcher(response);
        if (recMatcher.find()) {
            recommendation = recMatcher.group(1).trim();
        }

        return new JobFitChatResponse.FitScore(score, matched, gaps, recommendation);
    }

    private static List<String> parseSkillList(Pattern pattern, String text) {
        Matcher m = pattern.matcher(text);
        if (!m.find()) return List.of();
        String raw = m.group(1).trim();
        if (raw.isEmpty()) return List.of();
        return Arrays.stream(raw.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .toList();
    }

    static String cleanResponse(String response) {
        if (response == null) return null;
        return response
            .replaceAll("\\[FIT_SCORE:\\d+]", "")
            .replaceAll("\\[MATCHED:[^]]*]", "")
            .replaceAll("\\[GAPS:[^]]*]", "")
            .replaceAll("\\[RECOMMENDATION:[^]]*]", "")
            .trim();
    }
}
