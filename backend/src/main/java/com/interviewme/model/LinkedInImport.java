package com.interviewme.model;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.Type;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "linkedin_import")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class LinkedInImport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "profile_id", nullable = false)
    private Long profileId;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ImportStatus status;

    @Column(nullable = false)
    private String filename;

    @Column(name = "import_strategy")
    @Enumerated(EnumType.STRING)
    private ImportStrategy importStrategy;

    @Type(JsonBinaryType.class)
    @Column(name = "item_counts", columnDefinition = "jsonb")
    private Map<String, Integer> itemCounts;

    @Type(JsonBinaryType.class)
    @Column(name = "errors", columnDefinition = "jsonb")
    private List<String> errors;

    @Column(name = "imported_at")
    private Instant importedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
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
