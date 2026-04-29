package com.interviewme.aichat.repository;

import com.interviewme.aichat.model.ContentType;
import com.interviewme.aichat.model.DocumentChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, Long> {

    void deleteByTenantIdAndContentTypeAndContentId(Long tenantId, ContentType contentType, Long contentId);

    List<DocumentChunk> findByTenantIdAndContentTypeAndContentIdOrderByChunkIndex(
        Long tenantId, ContentType contentType, Long contentId);

    /**
     * BM25 full-text search on chunk_text using PostgreSQL tsvector/tsquery.
     * Returns chunks ranked by ts_rank_cd (cover density ranking).
     */
    @Query(value = """
        SELECT dc.* FROM document_chunk dc
        WHERE dc.tenant_id = :tenantId
          AND dc.search_vector @@ plainto_tsquery('english', :query)
        ORDER BY ts_rank_cd(dc.search_vector, plainto_tsquery('english', :query)) DESC
        LIMIT :topK
        """, nativeQuery = true)
    List<DocumentChunk> findByBm25(
        @Param("tenantId") Long tenantId,
        @Param("query") String query,
        @Param("topK") int topK);

    /**
     * pgvector cosine similarity search on chunk embeddings.
     */
    @Query(value = """
        SELECT dc.* FROM document_chunk dc
        WHERE dc.tenant_id = :tenantId
          AND dc.embedding IS NOT NULL
          AND 1 - (dc.embedding <=> CAST(:embedding AS vector)) > :threshold
        ORDER BY dc.embedding <=> CAST(:embedding AS vector)
        LIMIT :topK
        """, nativeQuery = true)
    List<DocumentChunk> findByVectorSimilarity(
        @Param("tenantId") Long tenantId,
        @Param("embedding") String embedding,
        @Param("topK") int topK,
        @Param("threshold") double threshold);

    /**
     * Upsert a chunk with embedding.
     */
    @Modifying
    @Query(value = """
        INSERT INTO document_chunk (tenant_id, content_type, content_id, parent_chunk_id, chunk_index, chunk_text, embedding, created_at)
        VALUES (:tenantId, :contentType, :contentId, :parentChunkId, :chunkIndex, :chunkText, CAST(:embedding AS vector), NOW())
        """, nativeQuery = true)
    void insertChunk(
        @Param("tenantId") Long tenantId,
        @Param("contentType") String contentType,
        @Param("contentId") Long contentId,
        @Param("parentChunkId") Long parentChunkId,
        @Param("chunkIndex") int chunkIndex,
        @Param("chunkText") String chunkText,
        @Param("embedding") String embedding);

    /**
     * BM25 search on content_embedding table (for non-chunked content).
     */
    @Query(value = """
        SELECT ce.* FROM content_embedding ce
        WHERE ce.tenant_id = :tenantId
          AND ce.search_vector @@ plainto_tsquery('english', :query)
        ORDER BY ts_rank_cd(ce.search_vector, plainto_tsquery('english', :query)) DESC
        LIMIT :topK
        """, nativeQuery = true)
    List<Object[]> findContentEmbeddingByBm25(
        @Param("tenantId") Long tenantId,
        @Param("query") String query,
        @Param("topK") int topK);
}
