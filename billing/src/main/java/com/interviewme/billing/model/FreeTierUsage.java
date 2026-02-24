package com.interviewme.billing.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "free_tier_usage",
       uniqueConstraints = @UniqueConstraint(
           name = "uq_free_tier_usage_tenant_feature_month",
           columnNames = {"tenant_id", "feature_type", "year_month"}
       ))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FreeTierUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "feature_type", nullable = false, length = 50)
    private FeatureType featureType;

    @Column(name = "usage_count", nullable = false)
    private Integer usageCount = 0;

    @Column(name = "year_month", nullable = false, length = 7)
    private String yearMonth;

    @Column(name = "created_at", nullable = false, updatable = false)
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
