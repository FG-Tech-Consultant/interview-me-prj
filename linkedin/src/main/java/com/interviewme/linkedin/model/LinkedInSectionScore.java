package com.interviewme.linkedin.model;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

import java.time.Instant;
import java.util.List;

@Entity
@Table(name = "linkedin_section_score")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LinkedInSectionScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "analysis_id", nullable = false)
    private Long analysisId;

    @Column(name = "section_name", nullable = false, length = 100)
    private String sectionName;

    @Column(name = "section_score", nullable = false)
    private Integer sectionScore;

    @Column(name = "quality_explanation", nullable = false, columnDefinition = "TEXT")
    private String qualityExplanation;

    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private List<String> suggestions;

    @Column(name = "raw_content", columnDefinition = "TEXT")
    private String rawContent;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}
