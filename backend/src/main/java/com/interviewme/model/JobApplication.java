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
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "job_application")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class JobApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "job_posting_id", nullable = false)
    private Long jobPostingId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "candidate_name", nullable = false, length = 255)
    private String candidateName;

    @Column(name = "candidate_email", nullable = false, length = 255)
    private String candidateEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ApplicationStatus status = ApplicationStatus.SUBMITTED;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ApplicationSource source = ApplicationSource.CHATBOT;

    @Column(name = "fit_score")
    private Integer fitScore;

    @Type(JsonBinaryType.class)
    @Column(name = "matched_skills", columnDefinition = "jsonb")
    private List<String> matchedSkills;

    @Type(JsonBinaryType.class)
    @Column(name = "gap_skills", columnDefinition = "jsonb")
    private List<String> gapSkills;

    @Column(name = "candidate_summary", columnDefinition = "TEXT")
    private String candidateSummary;

    @Column(name = "linkedin_pdf_path", length = 500)
    private String linkedinPdfPath;

    @Column(name = "chat_session_token")
    private UUID chatSessionToken;

    @Type(JsonBinaryType.class)
    @Column(name = "onboarding_data", columnDefinition = "jsonb")
    private Map<String, Object> onboardingData;

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
