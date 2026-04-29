package com.interviewme.graph;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewme.aichat.client.LlmChatMessage;
import com.interviewme.aichat.service.LlmRouterService;
import com.interviewme.dto.skill.SkillDto;
import com.interviewme.model.Skill;
import com.interviewme.repository.SkillRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AI-powered skill extraction from free text (CVs, job descriptions).
 * Extracted skills are created in PostgreSQL and synced to Neo4j.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SkillExtractionService {

    private static final String SYSTEM_PROMPT = """
            You are a skill extraction assistant. Extract technical and professional skills from the given text.

            Rules:
            - Extract only concrete, named skills (technologies, tools, methodologies, soft skills)
            - Normalize names: "java spring" → "Spring Boot", "js" → "JavaScript"
            - Do not extract generic terms like "experience", "knowledge", "ability"
            - Group under one of these categories:
              Programming Languages, Frameworks & Libraries, Cloud & DevOps, Data & AI, Soft Skills, Other
            - Return ONLY a valid JSON array, no other text:
            [{"name":"Java","category":"Programming Languages"},{"name":"Docker","category":"Cloud & DevOps"}]
            """;

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Pattern JSON_ARRAY = Pattern.compile("\\[.*?]", Pattern.DOTALL);

    private final LlmRouterService llmRouterService;
    private final SkillRepository skillRepository;
    private final GraphSkillSyncService graphSkillSyncService;

    public record ExtractedSkillEntry(String name, String category) {}

    /**
     * Extracts skills from text, persists new ones in PG, syncs all to Neo4j.
     *
     * @param tenantId  tenant context for the LLM call
     * @param text      raw text (CV, job description, etc.)
     * @return list of SkillDto for all extracted skills (new + existing)
     */
    @Transactional
    public List<SkillDto> extractAndSync(Long tenantId, String text) {
        log.info("Extracting skills from text ({} chars) for tenant {}", text.length(), tenantId);

        List<ExtractedSkillEntry> extracted = callLlm(tenantId, text);
        List<SkillDto> results = new ArrayList<>();

        for (ExtractedSkillEntry entry : extracted) {
            String name = entry.name().trim();
            if (name.isBlank()) continue;

            String category = entry.category() != null ? entry.category().trim() : "Other";

            Skill skill = skillRepository.findByNameIgnoreCase(name).orElse(null);
            if (skill == null) {
                skill = new Skill();
                skill.setName(name);
                skill.setCategory(category);
                skill.setDescription("Extracted by AI from user content");
                skill.setIsActive(true);
                skill = skillRepository.save(skill);
                log.debug("Created new skill: {} [{}]", name, category);
            }

            graphSkillSyncService.syncSkill(skill);

            results.add(new SkillDto(skill.getId(), skill.getName(), skill.getCategory(),
                    skill.getDescription(), skill.getTags(), skill.getIsActive()));
        }

        log.info("Extraction complete: {} skills found, {} new, synced to graph.", extracted.size(), results.size());
        return results;
    }

    private List<ExtractedSkillEntry> callLlm(Long tenantId, String text) {
        String truncated = text.length() > 4000 ? text.substring(0, 4000) : text;

        try {
            var response = llmRouterService.complete(tenantId, SYSTEM_PROMPT,
                    List.of(new LlmChatMessage("user", truncated)));

            String content = response.content();
            log.debug("LLM extraction raw response: {}", content);

            Matcher matcher = JSON_ARRAY.matcher(content);
            if (!matcher.find()) {
                log.warn("LLM did not return a JSON array for skill extraction");
                return List.of();
            }

            List<Map<String, String>> raw = MAPPER.readValue(matcher.group(),
                    new TypeReference<>() {});

            return raw.stream()
                    .filter(m -> m.get("name") != null && !m.get("name").isBlank())
                    .map(m -> new ExtractedSkillEntry(m.get("name"), m.getOrDefault("category", "Other")))
                    .toList();

        } catch (Exception e) {
            log.error("Skill extraction LLM call failed: {}", e.getMessage(), e);
            return List.of();
        }
    }
}
