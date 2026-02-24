package com.interviewme.model;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;
import org.hibernate.annotations.Type;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "user_skill")
@Data
@NoArgsConstructor
@AllArgsConstructor
@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "tenantId", type = Long.class))
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class UserSkill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "profile_id", nullable = false)
    private Long profileId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "skill_id", insertable = false, updatable = false)
    private Skill skill;

    @Column(name = "skill_id", nullable = false)
    private Long skillId;

    @Column(name = "years_of_experience", nullable = false)
    private Integer yearsOfExperience = 0;

    @Column(name = "proficiency_depth", nullable = false)
    private Integer proficiencyDepth;

    @Column(name = "last_used_date")
    private LocalDate lastUsedDate;

    @Column(name = "confidence_level", nullable = false, length = 20)
    private String confidenceLevel = "MEDIUM";

    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private List<String> tags;

    @Column(nullable = false, length = 20)
    private String visibility = "private";

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

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
