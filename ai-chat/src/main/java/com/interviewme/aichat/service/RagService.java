package com.interviewme.aichat.service;

import com.interviewme.aichat.config.AiProperties;
import com.interviewme.aichat.client.EmbeddingClient;
import com.interviewme.aichat.model.ContentEmbedding;
import com.interviewme.aichat.repository.ContentEmbeddingRepository;
import com.interviewme.aichat.service.HybridRetrievalService.RetrievedDocument;
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
    private final HybridRetrievalService hybridRetrievalService;
    private final ReRankingService reRankingService;

    public RagService(@Nullable EmbeddingClient embeddingClient,
                      ContentEmbeddingRepository embeddingRepository,
                      AiProperties aiProperties,
                      @Autowired(required = false) @Nullable LanguageModelQueryRouter queryRouter,
                      HybridRetrievalService hybridRetrievalService,
                      ReRankingService reRankingService) {
        this.embeddingClient = embeddingClient;
        this.embeddingRepository = embeddingRepository;
        this.aiProperties = aiProperties;
        this.queryRouter = queryRouter;
        this.hybridRetrievalService = hybridRetrievalService;
        this.reRankingService = reRankingService;
    }

    @Transactional(readOnly = true)
    public String retrieveContext(Long tenantId, String question) {
        // Try hybrid retrieval first (BM25 + pgvector + RRF + re-ranking)
        try {
            return retrieveHybrid(tenantId, question);
        } catch (Exception e) {
            log.warn("Hybrid retrieval failed, falling back to legacy: {}", e.getMessage());
        }

        // Fallback: query routing or simple vector search
        if (queryRouter != null) {
            return retrieveWithQueryRouting(tenantId, question);
        }
        return retrieveWithFallback(tenantId, question);
    }

    private String retrieveHybrid(Long tenantId, String question) {
        int topK = aiProperties.getEmbedding().getTopK();

        // 1. Hybrid retrieval: BM25 + pgvector + RRF
        List<RetrievedDocument> candidates = hybridRetrievalService.retrieve(tenantId, question, topK * 2);

        if (candidates.isEmpty()) {
            return "No specific information available.";
        }

        // 2. Re-rank top candidates using LLM cross-encoder
        List<RetrievedDocument> reranked = reRankingService.reRank(tenantId, question, candidates, topK);

        // 3. Build context string using parent text when available
        StringBuilder context = new StringBuilder();
        for (RetrievedDocument doc : reranked) {
            context.append("--- ").append(doc.getContentType()).append(" ---\n")
                   .append(doc.getContextText())
                   .append("\n\n");
        }

        log.info("Hybrid RAG tenantId={} question='{}' candidates={} reranked={}",
            tenantId,
            question.length() > 50 ? question.substring(0, 50) + "..." : question,
            candidates.size(), reranked.size());

        return context.toString();
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
