package com.interviewme.aichat.service;

import com.interviewme.aichat.model.ContentEmbedding;
import com.interviewme.aichat.model.ContentType;
import com.interviewme.aichat.repository.ContentEmbeddingRepository;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.Query;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;

@Slf4j
public class ProfileContentRetriever implements ContentRetriever {

    private final ContentEmbeddingRepository embeddingRepository;
    private final OllamaEmbeddingModel embeddingModel;
    private final ContentType contentType;
    private final int topK;
    private final double threshold;

    public ProfileContentRetriever(ContentEmbeddingRepository embeddingRepository,
                                   OllamaEmbeddingModel embeddingModel,
                                   ContentType contentType,
                                   int topK,
                                   double threshold) {
        this.embeddingRepository = embeddingRepository;
        this.embeddingModel = embeddingModel;
        this.contentType = contentType;
        this.topK = topK;
        this.threshold = threshold;
    }

    @Override
    public List<Content> retrieve(Query query) {
        Object chatMemoryId = query.metadata().chatMemoryId();
        if (chatMemoryId == null) {
            log.warn("No chatMemoryId (tenantId) in query metadata for content type {}", contentType);
            return Collections.emptyList();
        }
        Long tenantId = (Long) chatMemoryId;

        float[] embedding;
        try {
            var response = embeddingModel.embed("search_query: " + query.text());
            embedding = response.content().vector();
        } catch (Exception e) {
            log.warn("Embedding generation failed for query routing (type={}): {}", contentType, e.getMessage());
            return Collections.emptyList();
        }

        String embeddingStr = EmbeddingService.floatArrayToString(embedding);

        List<ContentEmbedding> results = embeddingRepository.findTopKBySimilarityAndType(
                tenantId, embeddingStr, contentType.name(), topK, threshold);

        log.debug("ProfileContentRetriever type={} found {} results for tenantId={}",
                contentType, results.size(), tenantId);

        return results.stream()
                .map(ce -> {
                    TextSegment segment = TextSegment.from(
                            "--- " + ce.getContentType() + " ---\n" + ce.getContentText());
                    return Content.from(segment);
                })
                .toList();
    }

    public ContentType getContentType() {
        return contentType;
    }
}
