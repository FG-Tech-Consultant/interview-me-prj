package com.interviewme.aichat.service;

import com.interviewme.aichat.client.EmbeddingClient;
import com.interviewme.aichat.model.ContentType;
import com.interviewme.aichat.repository.DocumentChunkRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Splits content into parent-child chunks for fine-grained retrieval.
 * Parent chunks = large context windows (~512 tokens).
 * Child chunks = small retrieval units (~128 tokens) linked to parent.
 */
@Service
@Slf4j
public class ChunkingService {

    private static final int PARENT_CHUNK_SIZE = 2000;  // ~512 tokens
    private static final int CHILD_CHUNK_SIZE = 500;    // ~128 tokens
    private static final int OVERLAP = 100;

    private final DocumentChunkRepository chunkRepository;
    private final EmbeddingClient embeddingClient;

    public ChunkingService(DocumentChunkRepository chunkRepository,
                           @Nullable EmbeddingClient embeddingClient) {
        this.chunkRepository = chunkRepository;
        this.embeddingClient = embeddingClient;
    }

    @Transactional
    public void chunkAndStore(Long tenantId, ContentType contentType, Long contentId, String content) {
        // Delete existing chunks for this content
        chunkRepository.deleteByTenantIdAndContentTypeAndContentId(tenantId, contentType, contentId);

        if (content == null || content.isBlank()) {
            return;
        }

        // If content is short enough, store as single chunk (no parent-child split)
        if (content.length() <= PARENT_CHUNK_SIZE) {
            String embedding = generateEmbedding(content);
            chunkRepository.insertChunk(tenantId, contentType.name(), contentId, null, 0, content, embedding);
            log.debug("Stored single chunk tenantId={} type={} contentId={}", tenantId, contentType, contentId);
            return;
        }

        // Split into parent chunks
        List<String> parentTexts = splitText(content, PARENT_CHUNK_SIZE, OVERLAP);
        int childIndex = 0;

        for (int pi = 0; pi < parentTexts.size(); pi++) {
            final int parentIndex = pi;
            String parentText = parentTexts.get(pi);

            // Insert parent chunk (no embedding — parents are context, not retrieval targets)
            chunkRepository.insertChunk(tenantId, contentType.name(), contentId, null, parentIndex, parentText, null);

            // Flush to get the parent ID via a query
            var parentChunks = chunkRepository.findByTenantIdAndContentTypeAndContentIdOrderByChunkIndex(
                tenantId, contentType, contentId);
            Long parentId = parentChunks.stream()
                .filter(c -> c.getChunkIndex() == parentIndex && c.getParentChunkId() == null)
                .map(c -> c.getId())
                .findFirst()
                .orElse(null);

            // Split parent into child chunks (these get embeddings for retrieval)
            List<String> childTexts = splitText(parentText, CHILD_CHUNK_SIZE, OVERLAP / 2);
            for (String childText : childTexts) {
                String embedding = generateEmbedding(childText);
                chunkRepository.insertChunk(tenantId, contentType.name(), contentId, parentId, childIndex++, childText, embedding);
            }
        }

        log.info("Chunked content tenantId={} type={} contentId={} parents={} totalChildren={}",
            tenantId, contentType, contentId, parentTexts.size(), childIndex);
    }

    private String generateEmbedding(String text) {
        if (embeddingClient == null) {
            return null;
        }
        try {
            float[] emb = embeddingClient.embed("search_document: " + text);
            return EmbeddingService.floatArrayToString(emb);
        } catch (Exception e) {
            log.warn("Embedding generation failed for chunk: {}", e.getMessage());
            return null;
        }
    }

    static List<String> splitText(String text, int chunkSize, int overlap) {
        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + chunkSize, text.length());
            // Try to break at sentence boundary
            if (end < text.length()) {
                int lastPeriod = text.lastIndexOf('.', end);
                if (lastPeriod > start + chunkSize / 2) {
                    end = lastPeriod + 1;
                }
            }
            chunks.add(text.substring(start, end).trim());
            start = end - overlap;
            if (start >= text.length()) break;
        }
        return chunks;
    }
}
