package com.interviewme.aichat.repository;

import com.interviewme.aichat.model.ContentEmbedding;
import com.interviewme.aichat.model.ContentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ContentEmbeddingRepository extends JpaRepository<ContentEmbedding, Long> {

    Optional<ContentEmbedding> findByTenantIdAndContentTypeAndContentId(
        Long tenantId, ContentType contentType, Long contentId);

    void deleteByTenantIdAndContentTypeAndContentId(
        Long tenantId, ContentType contentType, Long contentId);

    void deleteByTenantIdAndContentType(Long tenantId, ContentType contentType);

    @Query(value = """
        INSERT INTO content_embedding (tenant_id, content_type, content_id, content_text, embedding, created_at, updated_at)
        VALUES (:tenantId, :contentType, :contentId, :contentText, CAST(:embedding AS vector), NOW(), NOW())
        ON CONFLICT (tenant_id, content_type, content_id)
        DO UPDATE SET content_text = :contentText, embedding = CAST(:embedding AS vector), updated_at = NOW()
        """, nativeQuery = true)
    @org.springframework.data.jpa.repository.Modifying
    void upsertEmbedding(
        @Param("tenantId") Long tenantId,
        @Param("contentType") String contentType,
        @Param("contentId") Long contentId,
        @Param("contentText") String contentText,
        @Param("embedding") String embedding);

    @Query(value = """
        SELECT ce.* FROM content_embedding ce
        WHERE ce.tenant_id = :tenantId
          AND 1 - (ce.embedding <=> CAST(:embedding AS vector)) > :threshold
        ORDER BY ce.embedding <=> CAST(:embedding AS vector)
        LIMIT :topK
        """, nativeQuery = true)
    List<ContentEmbedding> findTopKBySimilarity(
        @Param("tenantId") Long tenantId,
        @Param("embedding") String embedding,
        @Param("topK") int topK,
        @Param("threshold") double threshold);
}
