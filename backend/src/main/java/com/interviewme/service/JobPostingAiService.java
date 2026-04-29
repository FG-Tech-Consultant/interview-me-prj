package com.interviewme.service;

import com.interviewme.aichat.client.LlmChatMessage;
import com.interviewme.aichat.client.LlmResponse;
import com.interviewme.aichat.service.LlmRouterService;
import com.interviewme.dto.job.AiJobDescriptionRequest;
import com.interviewme.dto.job.AiJobDescriptionResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobPostingAiService {

    private static final Pattern DESCRIPTION_PATTERN = Pattern.compile(
        "\\[DESCRIPTION](.*?)\\[/DESCRIPTION]", Pattern.DOTALL);
    private static final Pattern REQUIREMENTS_PATTERN = Pattern.compile(
        "\\[REQUIREMENTS](.*?)\\[/REQUIREMENTS]", Pattern.DOTALL);
    private static final Pattern BENEFITS_PATTERN = Pattern.compile(
        "\\[BENEFITS](.*?)\\[/BENEFITS]", Pattern.DOTALL);
    private static final Pattern SKILLS_PATTERN = Pattern.compile(
        "\\[SUGGESTED_SKILLS:([^]]*)]");
    private static final Pattern NICE_PATTERN = Pattern.compile(
        "\\[NICE_TO_HAVE:([^]]*)]");

    private final LlmRouterService llmRouter;

    @Transactional(readOnly = true)
    public AiJobDescriptionResponse generateJobDescription(Long tenantId, AiJobDescriptionRequest request) {
        String systemPrompt = buildSystemPrompt();
        String userMessage = buildUserMessage(request);

        var result = llmRouter.completeWithRequest(tenantId, systemPrompt,
            List.of(new LlmChatMessage("user", userMessage)));
        LlmResponse response = result.response();
        String content = response.content();

        log.info("AI job description generated for '{}' tokens={}", request.title(), response.tokensUsed());

        return parseResponse(content);
    }

    private String buildSystemPrompt() {
        return """
            You are a professional HR copywriter that creates compelling job postings.
            Generate a complete job posting based on the provided details.

            Output format (use these EXACT tags):
            [DESCRIPTION]
            Full job description (3-5 paragraphs, engaging, inclusive)
            [/DESCRIPTION]
            [REQUIREMENTS]
            Bulleted list of requirements (technical and soft skills)
            [/REQUIREMENTS]
            [BENEFITS]
            Bulleted list of benefits (competitive, modern)
            [/BENEFITS]
            [SUGGESTED_SKILLS:skill1,skill2,skill3,...] — core technical skills for this role
            [NICE_TO_HAVE:skill1,skill2,...] — bonus/nice-to-have skills

            Write in English unless the user's context suggests otherwise.
            Be specific and avoid generic phrases. Make the posting stand out.
            """;
    }

    private String buildUserMessage(AiJobDescriptionRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("Create a job posting for: ").append(request.title()).append("\n");
        if (request.experienceLevel() != null) {
            sb.append("Experience level: ").append(request.experienceLevel()).append("\n");
        }
        if (request.workModel() != null) {
            sb.append("Work model: ").append(request.workModel()).append("\n");
        }
        if (request.requiredSkills() != null && !request.requiredSkills().isEmpty()) {
            sb.append("Key skills: ").append(String.join(", ", request.requiredSkills())).append("\n");
        }
        if (request.additionalContext() != null) {
            sb.append("Additional context: ").append(request.additionalContext()).append("\n");
        }
        return sb.toString();
    }

    private AiJobDescriptionResponse parseResponse(String content) {
        String description = extractSection(DESCRIPTION_PATTERN, content, content);
        String requirements = extractSection(REQUIREMENTS_PATTERN, content, "");
        String benefits = extractSection(BENEFITS_PATTERN, content, "");
        List<String> suggestedSkills = extractList(SKILLS_PATTERN, content);
        List<String> niceToHave = extractList(NICE_PATTERN, content);

        return new AiJobDescriptionResponse(description, requirements, benefits,
            suggestedSkills, niceToHave);
    }

    private static String extractSection(Pattern pattern, String text, String defaultValue) {
        Matcher m = pattern.matcher(text);
        if (m.find()) return m.group(1).trim();
        return defaultValue;
    }

    private static List<String> extractList(Pattern pattern, String text) {
        Matcher m = pattern.matcher(text);
        if (!m.find()) return List.of();
        String raw = m.group(1).trim();
        if (raw.isEmpty()) return List.of();
        return Arrays.stream(raw.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .toList();
    }
}
