package com.interviewme.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewme.common.dto.ai.LlmRequest;
import com.interviewme.common.dto.ai.LlmResponse;
import com.interviewme.linkedin.dto.LinkedInLlmResult;
import com.interviewme.linkedin.model.ProfileSection;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class LinkedInPromptService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public LlmRequest buildScoringPrompt(Map<ProfileSection, String> sections) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("""
                You are a LinkedIn profile optimization expert. Analyze the following LinkedIn profile content and provide:
                1. An overall profile score (0-100)
                2. Per-section scores (0-100) with quality explanations and one improvement suggestion each

                Scoring Rubric:
                - 0-39 (Poor): Missing content, generic descriptions, no keywords or metrics
                - 40-59 (Below Average): Basic content present but lacks specificity, metrics, or compelling narrative
                - 60-79 (Good): Solid content with some metrics and keywords, room for optimization
                - 80-100 (Excellent): Compelling, keyword-rich, metric-driven content that stands out to recruiters

                Section-specific criteria:
                - HEADLINE: Keywords, specificity, value proposition, target role clarity
                - ABOUT: Metrics, storytelling, clarity, length (200-300 words ideal), career narrative
                - EXPERIENCE: Achievements vs duties, quantification, career progression, action verbs
                - EDUCATION: Completeness, relevance to target roles, certifications
                - SKILLS: Breadth, relevance to target roles, organization
                - RECOMMENDATIONS: Quantity (3+ ideal), quality, relevance of recommenders
                - OTHER: Additional sections (volunteering, publications, certifications)

                Profile Content:
                """);

        for (ProfileSection section : ProfileSection.values()) {
            String content = sections.getOrDefault(section, "");
            prompt.append("\n--- ").append(section.name()).append(" ---\n");
            if (content.isBlank()) {
                prompt.append("[Section not found or empty]\n");
            } else {
                prompt.append(content).append("\n");
            }
        }

        prompt.append("""

                Respond with ONLY a JSON object in this exact format (no markdown, no code blocks):
                {
                  "overallScore": <0-100>,
                  "sections": [
                    {
                      "sectionName": "<HEADLINE|ABOUT|EXPERIENCE|EDUCATION|SKILLS|RECOMMENDATIONS|OTHER>",
                      "score": <0-100>,
                      "explanation": "<quality explanation>",
                      "suggestion": "<one concrete, actionable improvement suggestion>"
                    }
                  ]
                }

                Include all 7 sections. If a section was not found, give it a score of 0 and explain that the section is missing.
                """);

        return new LlmRequest(prompt.toString(), "ANALYZE_LINKEDIN_PROFILE");
    }

    public LlmRequest buildAdditionalSuggestionsPrompt(String sectionName, String rawContent,
                                                        List<String> existingSuggestions, int count) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are a LinkedIn profile optimization expert.\n\n");
        prompt.append("Section: ").append(sectionName).append("\n");
        prompt.append("Current content:\n").append(rawContent != null ? rawContent : "[No content]").append("\n\n");

        if (existingSuggestions != null && !existingSuggestions.isEmpty()) {
            prompt.append("Existing suggestions (do NOT repeat these):\n");
            for (String s : existingSuggestions) {
                prompt.append("- ").append(s).append("\n");
            }
            prompt.append("\n");
        }

        prompt.append("Generate ").append(count).append(" NEW, concrete, actionable improvement suggestions for this section.\n");
        prompt.append("Each suggestion should be specific and different from existing ones.\n\n");
        prompt.append("""
                Respond with ONLY a JSON object in this exact format (no markdown, no code blocks):
                {
                  "suggestions": [
                    "<suggestion 1>",
                    "<suggestion 2>",
                    "<suggestion 3>"
                  ]
                }
                """);

        return new LlmRequest(prompt.toString(), "GENERATE_LINKEDIN_SUGGESTIONS");
    }

    public LinkedInLlmResult parseScoringResponse(LlmResponse response) {
        try {
            String content = cleanJsonResponse(response.content());
            JsonNode root = objectMapper.readTree(content);

            int overallScore = clampScore(root.path("overallScore").asInt(50));
            List<LinkedInLlmResult.SectionResult> sectionResults = new ArrayList<>();

            JsonNode sectionsNode = root.path("sections");
            if (sectionsNode.isArray()) {
                for (JsonNode sectionNode : sectionsNode) {
                    sectionResults.add(new LinkedInLlmResult.SectionResult(
                            sectionNode.path("sectionName").asText("OTHER"),
                            clampScore(sectionNode.path("score").asInt(50)),
                            sectionNode.path("explanation").asText("No explanation available"),
                            sectionNode.path("suggestion").asText("No suggestion available")
                    ));
                }
            }

            // Ensure all sections are present
            for (ProfileSection section : ProfileSection.values()) {
                boolean found = sectionResults.stream()
                        .anyMatch(r -> r.sectionName().equals(section.name()));
                if (!found) {
                    sectionResults.add(new LinkedInLlmResult.SectionResult(
                            section.name(), 0, "Section not analyzed", "Add this section to your profile"
                    ));
                }
            }

            return new LinkedInLlmResult(overallScore, sectionResults);

        } catch (JsonProcessingException e) {
            log.error("Failed to parse LLM scoring response", e);
            return buildFallbackResult();
        }
    }

    public List<String> parseAdditionalSuggestions(LlmResponse response) {
        try {
            String content = cleanJsonResponse(response.content());
            JsonNode root = objectMapper.readTree(content);
            JsonNode suggestionsNode = root.path("suggestions");

            List<String> suggestions = new ArrayList<>();
            if (suggestionsNode.isArray()) {
                for (JsonNode node : suggestionsNode) {
                    suggestions.add(node.asText());
                }
            }
            return suggestions;

        } catch (JsonProcessingException e) {
            log.error("Failed to parse additional suggestions response", e);
            return List.of("Unable to generate suggestions at this time. Please try again.");
        }
    }

    private String cleanJsonResponse(String response) {
        String cleaned = response.trim();
        // Remove markdown code blocks if present
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7);
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3);
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }
        return cleaned.trim();
    }

    private int clampScore(int score) {
        return Math.max(0, Math.min(100, score));
    }

    private LinkedInLlmResult buildFallbackResult() {
        List<LinkedInLlmResult.SectionResult> sections = new ArrayList<>();
        for (ProfileSection section : ProfileSection.values()) {
            sections.add(new LinkedInLlmResult.SectionResult(
                    section.name(), 0,
                    "Unable to analyze this section due to a processing error",
                    "Please try uploading your PDF again"
            ));
        }
        return new LinkedInLlmResult(0, sections);
    }
}
