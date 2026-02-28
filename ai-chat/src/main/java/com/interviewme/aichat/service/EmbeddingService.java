package com.interviewme.aichat.service;

import com.interviewme.aichat.client.EmbeddingClient;
import com.interviewme.aichat.model.ContentEmbedding;
import com.interviewme.aichat.model.ContentType;
import com.interviewme.aichat.repository.ContentEmbeddingRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class EmbeddingService {

    private final EmbeddingClient embeddingClient;
    private final ContentEmbeddingRepository embeddingRepository;

    public EmbeddingService(@Nullable EmbeddingClient embeddingClient,
                            ContentEmbeddingRepository embeddingRepository) {
        this.embeddingClient = embeddingClient;
        this.embeddingRepository = embeddingRepository;
        if (embeddingClient == null) {
            log.warn("No EmbeddingClient configured - embedding generation will be skipped");
        }
    }

    @Transactional
    public void generateEmbedding(Long tenantId, ContentType type, Long contentId, String contentText) {
        if (embeddingClient == null) {
            log.debug("Skipping embedding generation - no EmbeddingClient configured");
            return;
        }
        log.info("Generating embedding tenantId={} type={} contentId={}", tenantId, type, contentId);

        float[] embedding = embeddingClient.embed(contentText);
        String embeddingStr = floatArrayToString(embedding);

        ContentEmbedding entity = embeddingRepository
                .findByTenantIdAndContentTypeAndContentId(tenantId, type, contentId)
                .orElse(new ContentEmbedding());

        entity.setTenantId(tenantId);
        entity.setContentType(type);
        entity.setContentId(contentId);
        entity.setContentText(contentText);
        entity.setEmbedding(embeddingStr);

        embeddingRepository.save(entity);
        log.info("Embedding saved tenantId={} type={} contentId={}", tenantId, type, contentId);
    }

    @Transactional
    public void deleteEmbedding(Long tenantId, ContentType type, Long contentId) {
        log.info("Deleting embedding tenantId={} type={} contentId={}", tenantId, type, contentId);
        embeddingRepository.deleteByTenantIdAndContentTypeAndContentId(tenantId, type, contentId);
    }

    public static String floatArrayToString(float[] arr) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < arr.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(arr[i]);
        }
        sb.append("]");
        return sb.toString();
    }
}
