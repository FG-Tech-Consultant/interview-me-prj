package com.interviewme.aichat.service;

import com.interviewme.aichat.config.AiProperties;
import com.interviewme.aichat.client.EmbeddingClient;
import com.interviewme.aichat.model.ContentEmbedding;
import com.interviewme.aichat.repository.ContentEmbeddingRepository;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.Metadata;
import dev.langchain4j.rag.query.Query;
import dev.langchain4j.rag.query.router.LanguageModelQueryRouter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Service
@Slf4j
public class RagService {

    private final EmbeddingClient embeddingClient;
    private final ContentEmbeddingRepository embeddingRepository;
    private final AiProperties aiProperties;
    private final LanguageModelQueryRouter queryRouter;

    public RagService(@Nullable EmbeddingClient embeddingClient,
                      ContentEmbeddingRepository embeddingRepository,
                      AiProperties aiProperties,
                      @Autowired(required = false) @Nullable LanguageModelQueryRouter queryRouter) {
        this.embeddingClient = embeddingClient;
        this.embeddingRepository = embeddingRepository;
        this.aiProperties = aiProperties;
        this.queryRouter = queryRouter;
    }

    @Transactional(readOnly = true)
    public String retrieveContext(Long tenantId, String question) {
        if (queryRouter != null) {
            return retrieveWithQueryRouting(tenantId, question);
        }
        return retrieveWithFallback(tenantId, question);
    }

    private String retrieveWithQueryRouting(Long tenantId, String question) {
        Metadata metadata = Metadata.from(UserMessage.from(question), tenantId, List.of());
        Query query = Query.from(question, metadata);

        Collection<ContentRetriever> routed;
        try {
            routed = queryRouter.route(query);
        } catch (Exception e) {
            log.warn("Query routing failed, falling back to default retrieval: {}", e.getMessage());
            return retrieveWithFallback(tenantId, question);
        }

        List<String> routedTypes = new ArrayList<>();
        for (ContentRetriever retriever : routed) {
            if (retriever instanceof ProfileContentRetriever pcr) {
                routedTypes.add(pcr.getContentType().name());
            }
        }
        log.info("Query routing tenantId={} question='{}' routedTo=[{}]",
                tenantId,
                question.length() > 50 ? question.substring(0, 50) + "..." : question,
                String.join(",", routedTypes));

        List<Content> allContent = new ArrayList<>();
        for (ContentRetriever retriever : routed) {
            allContent.addAll(retriever.retrieve(query));
        }

        if (allContent.isEmpty()) {
            return "No specific information available.";
        }

        StringBuilder context = new StringBuilder();
        for (Content content : allContent) {
            context.append(content.textSegment().text()).append("\n\n");
        }
        return context.toString();
    }

    private String retrieveWithFallback(Long tenantId, String question) {
        if (embeddingClient == null) {
            log.debug("Skipping RAG retrieval - no EmbeddingClient configured");
            return "No specific information available.";
        }
        float[] questionEmbedding;
        try {
            questionEmbedding = embeddingClient.embed("search_query: " + question);
        } catch (Exception e) {
            log.warn("Embedding generation failed for RAG, returning no context: {}", e.getMessage());
            return "No specific information available.";
        }
        String embeddingStr = EmbeddingService.floatArrayToString(questionEmbedding);

        List<ContentEmbedding> results = embeddingRepository.findTopKBySimilarity(
                tenantId,
                embeddingStr,
                aiProperties.getEmbedding().getTopK(),
                aiProperties.getEmbedding().getSimilarityThreshold()
        );

        String types = results.stream()
                .map(r -> r.getContentType().name())
                .reduce((a, b) -> a + "," + b)
                .orElse("none");
        log.info("RAG retrieval tenantId={} question='{}' resultsFound={} types=[{}]", tenantId,
                question.length() > 50 ? question.substring(0, 50) + "..." : question,
                results.size(), types);

        if (results.isEmpty()) {
            return "No specific information available.";
        }

        StringBuilder context = new StringBuilder();
        for (ContentEmbedding item : results) {
            context.append("--- ").append(item.getContentType()).append(" ---\n")
                    .append(item.getContentText())
                    .append("\n\n");
        }
        return context.toString();
    }
}
