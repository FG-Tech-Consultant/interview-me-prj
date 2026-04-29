package com.interviewme.aichat.service;

import com.interviewme.aichat.client.LlmChatMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Contextual compression: uses the LLM to extract only the relevant portions
 * of retrieved documents, removing noise and irrelevant information.
 * Applied after retrieval + re-ranking to reduce context window usage.
 */
@Service
@Slf4j
public class ContextualCompressionService {

    private static final int MAX_CONTEXT_LENGTH = 3000;
    private static final String SYSTEM_PROMPT = """
        You are a context compression assistant. Given a user query and a set of retrieved \
        documents, extract ONLY the parts that are directly relevant to answering the query. \
        Remove irrelevant details, redundancies, and noise. Preserve important facts, numbers, \
        and specific details. Output the compressed context as a coherent summary. \
        If nothing is relevant, output "No relevant information found." \
        Keep the output concise — maximum 500 words.""";

    private final LlmRouterService llmRouter;

    public ContextualCompressionService(LlmRouterService llmRouter) {
        this.llmRouter = llmRouter;
    }

    /**
     * Compresses retrieved context by extracting only query-relevant portions.
     * Falls back to truncated original context if compression fails.
     */
    public String compress(Long tenantId, String query, String retrievedContext) {
        if (retrievedContext == null || retrievedContext.isBlank()) {
            return retrievedContext;
        }

        // Skip compression for short contexts
        if (retrievedContext.length() < 500) {
            return retrievedContext;
        }

        try {
            String userPrompt = "Query: " + query + "\n\nDocuments:\n" + retrievedContext;

            var result = llmRouter.completeWithRequest(tenantId,
                SYSTEM_PROMPT,
                List.of(new LlmChatMessage("user", userPrompt)));

            String compressed = result.response().content();

            if (compressed != null && !compressed.isBlank()) {
                double ratio = compressed.length() * 100.0 / retrievedContext.length();
                log.info("Context compressed: original={}chars compressed={}chars ratio={}%",
                    retrievedContext.length(), compressed.length(),
                    String.format("%.1f", ratio));
                return compressed;
            }
        } catch (Exception e) {
            log.warn("Contextual compression failed, using original: {}", e.getMessage());
        }

        // Fallback: truncate
        if (retrievedContext.length() > MAX_CONTEXT_LENGTH) {
            return retrievedContext.substring(0, MAX_CONTEXT_LENGTH) + "\n[truncated]";
        }
        return retrievedContext;
    }
}
