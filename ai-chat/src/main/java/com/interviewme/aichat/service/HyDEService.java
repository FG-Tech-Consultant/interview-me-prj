package com.interviewme.aichat.service;

import com.interviewme.aichat.client.EmbeddingClient;
import com.interviewme.aichat.client.LlmChatMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Hypothetical Document Embeddings (HyDE): generates a hypothetical answer
 * document for the query, then embeds that document instead of the raw query.
 * This bridges the vocabulary gap between questions and relevant documents.
 */
@Service
@Slf4j
public class HyDEService {

    private static final String SYSTEM_PROMPT = """
        You are a helpful assistant. Given a question, write a short paragraph (3-5 sentences) \
        that directly answers the question as if you were writing a section of a relevant document. \
        Do not say "I don't know" — write a plausible, factual-sounding answer. \
        Output ONLY the paragraph, no preamble.""";

    private final LlmRouterService llmRouter;
    private final EmbeddingClient embeddingClient;

    public HyDEService(LlmRouterService llmRouter, @Nullable EmbeddingClient embeddingClient) {
        this.llmRouter = llmRouter;
        this.embeddingClient = embeddingClient;
    }

    /**
     * Generates a hypothetical document for the query and returns its embedding string.
     * Returns null if HyDE fails (caller should fall back to regular query embedding).
     */
    public String generateHypotheticalEmbedding(Long tenantId, String query) {
        if (embeddingClient == null) {
            return null;
        }

        try {
            // 1. Generate hypothetical document
            var result = llmRouter.completeWithRequest(tenantId,
                SYSTEM_PROMPT,
                List.of(new LlmChatMessage("user", query)));

            String hypotheticalDoc = result.response().content();

            if (hypotheticalDoc == null || hypotheticalDoc.isBlank()) {
                return null;
            }

            // 2. Embed the hypothetical document
            float[] embedding = embeddingClient.embed("search_document: " + hypotheticalDoc);
            String embeddingStr = EmbeddingService.floatArrayToString(embedding);

            log.info("HyDE generated for query='{}' hypoDocLen={}",
                truncate(query), hypotheticalDoc.length());
            return embeddingStr;

        } catch (Exception e) {
            log.warn("HyDE generation failed, will use standard query embedding: {}", e.getMessage());
            return null;
        }
    }

    private String truncate(String s) {
        return s.length() > 50 ? s.substring(0, 50) + "..." : s;
    }
}
