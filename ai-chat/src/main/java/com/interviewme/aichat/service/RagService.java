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
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class RagService {

    private final EmbeddingClient embeddingClient;
    private final ContentEmbeddingRepository embeddingRepository;
    private final AiProperties aiProperties;
    private final LanguageModelQueryRouter queryRouter;
    private final HybridRetrievalService hybridRetrievalService;
    private final ReRankingService reRankingService;
    private final MultiQueryExpander multiQueryExpander;
    private final HyDEService hydeService;
    private final GraphRagService graphRagService;
    private final ContextualCompressionService compressionService;
    private final RagasEvaluationService ragasService;

    public RagService(@Nullable EmbeddingClient embeddingClient,
                      ContentEmbeddingRepository embeddingRepository,
                      AiProperties aiProperties,
                      @Autowired(required = false) @Nullable LanguageModelQueryRouter queryRouter,
                      HybridRetrievalService hybridRetrievalService,
                      ReRankingService reRankingService,
                      MultiQueryExpander multiQueryExpander,
                      HyDEService hydeService,
                      GraphRagService graphRagService,
                      ContextualCompressionService compressionService,
                      RagasEvaluationService ragasService) {
        this.embeddingClient = embeddingClient;
        this.embeddingRepository = embeddingRepository;
        this.aiProperties = aiProperties;
        this.queryRouter = queryRouter;
        this.hybridRetrievalService = hybridRetrievalService;
        this.reRankingService = reRankingService;
        this.multiQueryExpander = multiQueryExpander;
        this.hydeService = hydeService;
        this.graphRagService = graphRagService;
        this.compressionService = compressionService;
        this.ragasService = ragasService;
    }

    @Transactional(readOnly = true)
    public String retrieveContext(Long tenantId, String question) {
        // Primary: advanced RAG pipeline (multi-query + HyDE + hybrid + graph + compression)
        try {
            return retrieveAdvanced(tenantId, question);
        } catch (Exception e) {
            log.warn("Advanced RAG failed, falling back to hybrid: {}", e.getMessage());
        }

        // Fallback 1: hybrid retrieval
        try {
            return retrieveHybrid(tenantId, question);
        } catch (Exception e) {
            log.warn("Hybrid retrieval failed, falling back to legacy: {}", e.getMessage());
        }

        // Fallback 2: query routing or simple vector
        if (queryRouter != null) {
            return retrieveWithQueryRouting(tenantId, question);
        }
        return retrieveWithFallback(tenantId, question);
    }

    private String retrieveAdvanced(Long tenantId, String question) {
        int topK = aiProperties.getEmbedding().getTopK();

        // 1. Multi-query expansion: generate alternative perspectives
        List<String> queries = multiQueryExpander.expand(tenantId, question);

        // 2. HyDE: generate hypothetical document embedding
        String hydeEmbedding = hydeService.generateHypotheticalEmbedding(tenantId, question);

        // 3. Retrieve for each query variant via hybrid retrieval
        Map<String, RetrievedDocument> allDocs = new LinkedHashMap<>();

        for (String query : queries) {
            List<RetrievedDocument> results = hybridRetrievalService.retrieve(tenantId, query, topK);
            for (RetrievedDocument doc : results) {
                String key = doc.getContentType() + ":" + doc.getContentId() + ":" + doc.getText().hashCode();
                allDocs.putIfAbsent(key, doc);
            }
        }

        // 4. If HyDE embedding is available, also do a vector-only search with it
        if (hydeEmbedding != null) {
            List<ContentEmbedding> hydeResults = embeddingRepository.findTopKBySimilarity(
                tenantId, hydeEmbedding, topK, aiProperties.getEmbedding().getSimilarityThreshold());
            for (ContentEmbedding ce : hydeResults) {
                String key = ce.getContentType() + ":" + ce.getContentId() + ":" + ce.getContentText().hashCode();
                allDocs.putIfAbsent(key, RetrievedDocument.fromContentEmbedding(ce));
            }
        }

        if (allDocs.isEmpty()) {
            return "No specific information available.";
        }

        // 5. Re-rank all candidates
        List<RetrievedDocument> candidates = new ArrayList<>(allDocs.values());
        List<RetrievedDocument> reranked = reRankingService.reRank(tenantId, question, candidates, topK);

        // 6. Graph RAG: enrich with graph context
        List<String> retrievedTexts = reranked.stream()
            .map(RetrievedDocument::getText)
            .collect(Collectors.toList());
        GraphRagService.GraphContext graphCtx = graphRagService.enrichWithGraphContext(question, retrievedTexts);
        String graphContextStr = graphRagService.buildGraphContextString(graphCtx);

        // 7. Build raw context
        StringBuilder rawContext = new StringBuilder();
        for (RetrievedDocument doc : reranked) {
            rawContext.append("--- ").append(doc.getContentType()).append(" ---\n")
                      .append(doc.getContextText())
                      .append("\n\n");
        }

        // 8. Append graph context
        if (!graphContextStr.isEmpty()) {
            rawContext.append(graphContextStr).append("\n");
        }

        // 9. Contextual compression
        String compressed = compressionService.compress(tenantId, question, rawContext.toString());

        log.info("Advanced RAG tenantId={} queries={} totalCandidates={} reranked={} graphSkills={} compressed={}→{}chars",
            tenantId, queries.size(), allDocs.size(), reranked.size(),
            graphCtx.mentionedSkills().size(),
            rawContext.length(), compressed.length());

        return compressed;
    }

    /**
     * Triggers async RAGAS evaluation after a response is generated.
     * Call this from the chat service after sending the response.
     */
    @Async
    public void evaluateAsync(Long tenantId, String question, String context, String answer) {
        ragasService.evaluate(tenantId, question, context, answer);
    }

    private String retrieveHybrid(Long tenantId, String question) {
        int topK = aiProperties.getEmbedding().getTopK();

        List<RetrievedDocument> candidates = hybridRetrievalService.retrieve(tenantId, question, topK * 2);

        if (candidates.isEmpty()) {
            return "No specific information available.";
        }

        List<RetrievedDocument> reranked = reRankingService.reRank(tenantId, question, candidates, topK);

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
