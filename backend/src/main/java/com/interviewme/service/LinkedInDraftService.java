package com.interviewme.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewme.billing.model.FeatureType;
import com.interviewme.billing.model.RefType;
import com.interviewme.billing.service.CoinWalletService;
import com.interviewme.billing.service.FreeTierService;
import com.interviewme.common.dto.ai.LlmClient;
import com.interviewme.common.dto.ai.LlmRequest;
import com.interviewme.common.dto.ai.LlmResponse;
import com.interviewme.common.exception.ProfileNotFoundException;
import com.interviewme.common.exception.ValidationException;
import com.interviewme.common.util.TenantContext;
import com.interviewme.dto.linkedin.CreateDraftRequest;
import com.interviewme.dto.linkedin.DraftPageResponse;
import com.interviewme.dto.linkedin.DraftResponse;
import com.interviewme.mapper.LinkedInDraftMapper;
import com.interviewme.model.DraftCategory;
import com.interviewme.model.DraftStatus;
import com.interviewme.model.LinkedInDraft;
import com.interviewme.repository.LinkedInDraftRepository;
import com.interviewme.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class LinkedInDraftService {

    private final LinkedInDraftRepository draftRepository;
    private final ProfileRepository profileRepository;
    private final LlmClient llmClient;
    private final FreeTierService freeTierService;
    private final CoinWalletService coinWalletService;
    private final ObjectMapper objectMapper;

    @Value("${billing.costs.LINKEDIN_DRAFT:3}")
    private int draftCost;

    private static final int MAX_RETRIES = 3;
    private static final long[] RETRY_DELAYS_MS = {2000, 4000, 8000};

    @Transactional
    public DraftResponse createDraft(Long profileId, CreateDraftRequest request) {
        Long tenantId = TenantContext.getCurrentTenantId();
        log.info("Creating LinkedIn draft for profileId={}, tenantId={}", profileId, tenantId);

        profileRepository.findByIdAndTenantId(profileId, tenantId)
            .orElseThrow(() -> new ProfileNotFoundException(profileId));

        // Handle billing: free tier first, then coins
        boolean usedFreeTier = freeTierService.tryConsumeFreeTier(tenantId, FeatureType.LINKEDIN_DRAFT);
        if (!usedFreeTier) {
            log.info("Free tier exhausted for LinkedIn drafts, spending {} coins for tenantId={}", draftCost, tenantId);
            coinWalletService.spend(tenantId, draftCost, RefType.LINKEDIN_DRAFT,
                "linkedin-draft-" + System.currentTimeMillis(),
                "LinkedIn inbox draft generation");
        }

        // Build prompt and call LLM
        String tone = request.tone() != null ? request.tone() : "professional";
        LlmRequest llmRequest = buildDraftPrompt(request.originalMessage(), tone);
        LlmResponse llmResponse = callLlmWithRetry(llmRequest);

        // Parse LLM response
        DraftCategory category = DraftCategory.OTHER;
        String suggestedReply = llmResponse.content();

        try {
            String cleaned = cleanJsonResponse(llmResponse.content());
            JsonNode root = objectMapper.readTree(cleaned);

            String categoryStr = root.path("category").asText("OTHER");
            try {
                category = DraftCategory.valueOf(categoryStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                category = DraftCategory.OTHER;
            }

            suggestedReply = root.path("suggestedReply").asText(llmResponse.content());
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse LLM draft response as JSON, using raw content", e);
        }

        // Save draft
        LinkedInDraft draft = new LinkedInDraft();
        draft.setTenantId(tenantId);
        draft.setProfileId(profileId);
        draft.setOriginalMessage(request.originalMessage());
        draft.setCategory(category);
        draft.setSuggestedReply(suggestedReply);
        draft.setTone(tone);
        draft.setStatus(DraftStatus.DRAFT);

        LinkedInDraft saved = draftRepository.save(draft);
        log.info("LinkedIn draft created with id={}, category={}", saved.getId(), category);

        return LinkedInDraftMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public DraftPageResponse listDrafts(Long profileId, int page, int size) {
        log.debug("Fetching drafts for profileId={}, page={}, size={}", profileId, page, size);

        Page<LinkedInDraft> drafts = draftRepository
            .findByProfileIdAndDeletedAtIsNullOrderByCreatedAtDesc(profileId, PageRequest.of(page, Math.min(size, 50)));

        return LinkedInDraftMapper.toPageResponse(drafts);
    }

    @Transactional(readOnly = true)
    public DraftResponse getDraft(Long draftId) {
        log.debug("Fetching draft id={}", draftId);

        LinkedInDraft draft = draftRepository.findByIdAndDeletedAtIsNull(draftId)
            .orElseThrow(() -> new ValidationException("id", "Draft not found: " + draftId));

        return LinkedInDraftMapper.toResponse(draft);
    }

    @Transactional
    public void deleteDraft(Long draftId) {
        log.info("Soft deleting draft id={}", draftId);

        LinkedInDraft draft = draftRepository.findByIdAndDeletedAtIsNull(draftId)
            .orElseThrow(() -> new ValidationException("id", "Draft not found: " + draftId));

        draft.setDeletedAt(Instant.now());
        draftRepository.save(draft);
        log.info("Draft soft deleted: {}", draftId);
    }

    @Transactional
    public DraftResponse updateStatus(Long draftId, DraftStatus newStatus) {
        log.info("Updating draft id={} status to {}", draftId, newStatus);

        LinkedInDraft draft = draftRepository.findByIdAndDeletedAtIsNull(draftId)
            .orElseThrow(() -> new ValidationException("id", "Draft not found: " + draftId));

        draft.setStatus(newStatus);
        LinkedInDraft updated = draftRepository.save(draft);
        log.info("Draft status updated: id={}, status={}", draftId, newStatus);

        return LinkedInDraftMapper.toResponse(updated);
    }

    private LlmRequest buildDraftPrompt(String originalMessage, String tone) {
        String prompt = """
            You are a LinkedIn messaging expert. Analyze the following LinkedIn message and:
            1. Categorize it as one of: RECRUITER, AGENCY, FOUNDER, SPAM, OTHER
            2. Generate a professional reply draft

            Tone preference: %s

            LinkedIn Message:
            ---
            %s
            ---

            Respond with ONLY a JSON object in this exact format (no markdown, no code blocks):
            {
              "category": "<RECRUITER|AGENCY|FOUNDER|SPAM|OTHER>",
              "suggestedReply": "<your draft reply>"
            }

            Guidelines for the reply:
            - Keep it concise (2-4 sentences)
            - Be polite and professional
            - If SPAM, write a brief polite decline
            - If RECRUITER/AGENCY, express interest and ask for more details about the role
            - If FOUNDER, express interest and ask about the opportunity
            - Match the requested tone (%s)
            """.formatted(tone, originalMessage, tone);

        return new LlmRequest(prompt, "LINKEDIN_DRAFT");
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

    private String cleanJsonResponse(String response) {
        String cleaned = response.trim();
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
}
