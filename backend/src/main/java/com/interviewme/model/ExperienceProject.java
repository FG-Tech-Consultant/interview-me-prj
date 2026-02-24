package com.interviewme.model;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.Type;

import com.interviewme.model.JobExperience;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "experience_project")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class ExperienceProject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "job_experience_id", nullable = false)
    private Long jobExperienceId;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String context;

    @Column(length = 255)
    private String role;

    @Column(name = "team_size")
    private Integer teamSize;

    @Type(JsonBinaryType.class)
    @Column(name = "tech_stack", columnDefinition = "jsonb")
    private List<String> techStack;

    @Column(name = "architecture_type", length = 100)
    private String architectureType;

    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metrics;

    @Column(columnDefinition = "TEXT")
    private String outcomes;

    @Column(nullable = false, length = 20)
    private String visibility = "private";

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Version
    @Column(nullable = false)
    private Long version = 0L;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_experience_id", insertable = false, updatable = false)
    private JobExperience jobExperience;

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
