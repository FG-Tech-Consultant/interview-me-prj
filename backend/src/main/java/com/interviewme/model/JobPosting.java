package com.interviewme.model;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.Type;

import java.time.OffsetDateTime;
import java.util.List;

@Entity
@Table(name = "job_posting")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class JobPosting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "company_id")
    private Long companyId;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, length = 100, unique = true)
    private String slug;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String requirements;

    @Column(columnDefinition = "TEXT")
    private String benefits;

    @Column(length = 255)
    private String location;

    @Column(name = "work_model", length = 50)
    private String workModel;

    @Column(name = "salary_range", length = 100)
    private String salaryRange;

    @Column(name = "experience_level", length = 50)
    private String experienceLevel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private JobPostingStatus status = JobPostingStatus.ACTIVE;

    @Type(JsonBinaryType.class)
    @Column(name = "required_skills", columnDefinition = "jsonb")
    private List<String> requiredSkills;

    @Type(JsonBinaryType.class)
    @Column(name = "nice_to_have_skills", columnDefinition = "jsonb")
    private List<String> niceToHaveSkills;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
