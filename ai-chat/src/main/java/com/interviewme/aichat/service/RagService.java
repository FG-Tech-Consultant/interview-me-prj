package com.interviewme.aichat.service;

import com.interviewme.aichat.config.AiProperties;
import com.interviewme.aichat.client.EmbeddingClient;
import com.interviewme.aichat.model.ContentEmbedding;
import com.interviewme.aichat.repository.ContentEmbeddingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RagService {

    private final EmbeddingClient embeddingClient;
    private final ContentEmbeddingRepository embeddingRepository;
    private final AiProperties aiProperties;

    @Transactional(readOnly = true)
    public String retrieveContext(Long tenantId, String question) {
        float[] questionEmbedding = embeddingClient.embed(question);
        String embeddingStr = EmbeddingService.floatArrayToString(questionEmbedding);

        List<ContentEmbedding> results = embeddingRepository.findTopKBySimilarity(
                tenantId,
                embeddingStr,
                aiProperties.getEmbedding().getTopK(),
                aiProperties.getEmbedding().getSimilarityThreshold()
        );

        log.info("RAG retrieval tenantId={} question='{}' resultsFound={}", tenantId,
                question.length() > 50 ? question.substring(0, 50) + "..." : question,
                results.size());

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
