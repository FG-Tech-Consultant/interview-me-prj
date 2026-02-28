package com.interviewme.aichat.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "content_embedding", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"tenant_id", "content_type", "content_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContentEmbedding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "content_type", nullable = false, length = 20)
    private ContentType contentType;

    @Column(name = "content_id", nullable = false)
    private Long contentId;

    @Column(name = "content_text", nullable = false, columnDefinition = "TEXT")
    private String contentText;

    // Stored as pgvector format - native query handles conversion
    @Column(name = "embedding", nullable = false, columnDefinition = "vector(768)")
    private String embedding;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
