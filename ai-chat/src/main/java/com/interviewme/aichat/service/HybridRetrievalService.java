package com.interviewme.aichat.service;

import com.interviewme.aichat.client.EmbeddingClient;
import com.interviewme.aichat.config.AiProperties;
import com.interviewme.aichat.model.ContentEmbedding;
import com.interviewme.aichat.model.DocumentChunk;
import com.interviewme.aichat.repository.ContentEmbeddingRepository;
import com.interviewme.aichat.repository.DocumentChunkRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Hybrid retrieval: BM25 (full-text) + pgvector (semantic) fused via
 * Reciprocal Rank Fusion (RRF). Searches both document_chunk and
 * content_embedding tables.
 */
@Service
@Slf4j
public class HybridRetrievalService {

    private static final int RRF_K = 60; // RRF constant (standard value from original paper)

    private final DocumentChunkRepository chunkRepository;
    private final ContentEmbeddingRepository embeddingRepository;
    private final EmbeddingClient embeddingClient;
    private final AiProperties aiProperties;

    public HybridRetrievalService(DocumentChunkRepository chunkRepository,
                                  ContentEmbeddingRepository embeddingRepository,
                                  @Nullable EmbeddingClient embeddingClient,
                                  AiProperties aiProperties) {
        this.chunkRepository = chunkRepository;
        this.embeddingRepository = embeddingRepository;
        this.embeddingClient = embeddingClient;
        this.aiProperties = aiProperties;
    }

    @Transactional(readOnly = true)
    public List<RetrievedDocument> retrieve(Long tenantId, String query, int topK) {
        int fetchK = topK * 3; // Over-fetch for fusion

        // 1. BM25 retrieval (chunks)
        List<DocumentChunk> bm25Chunks = chunkRepository.findByBm25(tenantId, query, fetchK);

        // 2. BM25 retrieval (content_embedding fallback for non-chunked content)
        List<ContentEmbedding> bm25Embeddings = bm25ContentEmbeddings(tenantId, query, fetchK);

        // 3. Vector retrieval (chunks)
        List<DocumentChunk> vectorChunks = List.of();
        List<ContentEmbedding> vectorEmbeddings = List.of();
        if (embeddingClient != null) {
            String embeddingStr = generateQueryEmbedding(query);
            if (embeddingStr != null) {
                vectorChunks = chunkRepository.findByVectorSimilarity(
                    tenantId, embeddingStr, fetchK, aiProperties.getEmbedding().getSimilarityThreshold());
                vectorEmbeddings = embeddingRepository.findTopKBySimilarity(
                    tenantId, embeddingStr, fetchK, aiProperties.getEmbedding().getSimilarityThreshold());
            }
        }

        // 4. Build unified document map and apply RRF
        Map<String, RetrievedDocument> docMap = new LinkedHashMap<>();

        // Add BM25 chunk results
        for (int i = 0; i < bm25Chunks.size(); i++) {
            DocumentChunk chunk = bm25Chunks.get(i);
            String key = "chunk:" + chunk.getId();
            RetrievedDocument doc = docMap.computeIfAbsent(key, k -> RetrievedDocument.fromChunk(chunk));
            doc.addBm25Rank(i + 1);
        }

        // Add BM25 content_embedding results
        for (int i = 0; i < bm25Embeddings.size(); i++) {
            ContentEmbedding ce = bm25Embeddings.get(i);
            String key = "ce:" + ce.getId();
            RetrievedDocument doc = docMap.computeIfAbsent(key, k -> RetrievedDocument.fromContentEmbedding(ce));
            doc.addBm25Rank(i + 1);
        }

        // Add vector chunk results
        for (int i = 0; i < vectorChunks.size(); i++) {
            DocumentChunk chunk = vectorChunks.get(i);
            String key = "chunk:" + chunk.getId();
            RetrievedDocument doc = docMap.computeIfAbsent(key, k -> RetrievedDocument.fromChunk(chunk));
            doc.addVectorRank(i + 1);
        }

        // Add vector content_embedding results
        for (int i = 0; i < vectorEmbeddings.size(); i++) {
            ContentEmbedding ce = vectorEmbeddings.get(i);
            String key = "ce:" + ce.getId();
            RetrievedDocument doc = docMap.computeIfAbsent(key, k -> RetrievedDocument.fromContentEmbedding(ce));
            doc.addVectorRank(i + 1);
        }

        // 5. Compute RRF scores and sort
        List<RetrievedDocument> results = new ArrayList<>(docMap.values());
        results.forEach(doc -> doc.computeRrfScore(RRF_K));
        results.sort(Comparator.comparingDouble(RetrievedDocument::getRrfScore).reversed());

        // 6. Parent expansion: if a child chunk is selected, include parent text for context
        expandParentContext(results, tenantId);

        List<RetrievedDocument> topResults = results.stream().limit(topK).collect(Collectors.toList());

        log.info("Hybrid retrieval tenantId={} query='{}' bm25Chunks={} bm25Ce={} vecChunks={} vecCe={} fused={}",
            tenantId,
            query.length() > 50 ? query.substring(0, 50) + "..." : query,
            bm25Chunks.size(), bm25Embeddings.size(),
            vectorChunks.size(), vectorEmbeddings.size(),
            topResults.size());

        return topResults;
    }

    private List<ContentEmbedding> bm25ContentEmbeddings(Long tenantId, String query, int topK) {
        try {
            List<Object[]> rows = chunkRepository.findContentEmbeddingByBm25(tenantId, query, topK);
            return rows.stream().map(row -> {
                ContentEmbedding ce = new ContentEmbedding();
                ce.setId(((Number) row[0]).longValue());
                ce.setTenantId(((Number) row[1]).longValue());
                ce.setContentType(com.interviewme.aichat.model.ContentType.valueOf((String) row[2]));
                ce.setContentId(((Number) row[3]).longValue());
                ce.setContentText((String) row[4]);
                return ce;
            }).collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("BM25 search on content_embedding failed: {}", e.getMessage());
            return List.of();
        }
    }

    private String generateQueryEmbedding(String query) {
        try {
            float[] emb = embeddingClient.embed("search_query: " + query);
            return EmbeddingService.floatArrayToString(emb);
        } catch (Exception e) {
            log.warn("Query embedding generation failed: {}", e.getMessage());
            return null;
        }
    }

    private void expandParentContext(List<RetrievedDocument> docs, Long tenantId) {
        for (RetrievedDocument doc : docs) {
            if (doc.getParentChunkId() != null) {
                chunkRepository.findById(doc.getParentChunkId()).ifPresent(parent ->
                    doc.setParentText(parent.getChunkText())
                );
            }
        }
    }

    /**
     * Represents a retrieved document with RRF fusion scores.
     */
    public static class RetrievedDocument {
        private final String text;
        private final String contentType;
        private final Long contentId;
        private Long parentChunkId;
        private String parentText;
        private Integer bm25Rank;
        private Integer vectorRank;
        private double rrfScore;

        private RetrievedDocument(String text, String contentType, Long contentId, Long parentChunkId) {
            this.text = text;
            this.contentType = contentType;
            this.contentId = contentId;
            this.parentChunkId = parentChunkId;
        }

        static RetrievedDocument fromChunk(DocumentChunk chunk) {
            return new RetrievedDocument(chunk.getChunkText(), chunk.getContentType().name(),
                chunk.getContentId(), chunk.getParentChunkId());
        }

        static RetrievedDocument fromContentEmbedding(ContentEmbedding ce) {
            return new RetrievedDocument(ce.getContentText(), ce.getContentType().name(),
                ce.getContentId(), null);
        }

        void addBm25Rank(int rank) { this.bm25Rank = rank; }
        void addVectorRank(int rank) { this.vectorRank = rank; }

        void computeRrfScore(int k) {
            double score = 0;
            if (bm25Rank != null) score += 1.0 / (k + bm25Rank);
            if (vectorRank != null) score += 1.0 / (k + vectorRank);
            this.rrfScore = score;
        }

        public String getText() { return text; }
        public String getContentType() { return contentType; }
        public Long getContentId() { return contentId; }
        public Long getParentChunkId() { return parentChunkId; }
        public String getParentText() { return parentText; }
        public void setParentText(String parentText) { this.parentText = parentText; }
        public Integer getBm25Rank() { return bm25Rank; }
        public Integer getVectorRank() { return vectorRank; }
        public double getRrfScore() { return rrfScore; }

        /**
         * Returns the best text for context: parent text if available (broader context),
         * otherwise the chunk/embedding text itself.
         */
        public String getContextText() {
            return parentText != null ? parentText : text;
        }
    }
}
