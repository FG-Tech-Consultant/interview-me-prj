package com.interviewme.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "package_skill")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PackageSkill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "package_id", nullable = false)
    private Long packageId;

    @Column(name = "user_skill_id", nullable = false)
    private Long userSkillId;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_skill_id", insertable = false, updatable = false)
    private UserSkill userSkill;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}
