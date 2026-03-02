package com.interviewme.model;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.Type;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "profile")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class Profile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "full_name", nullable = false, length = 255)
    private String fullName;

    @Column(name = "headline", length = 255)
    private String headline;

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    @Column(name = "location", length = 255)
    private String location;

    @Type(JsonBinaryType.class)
    @Column(name = "languages", columnDefinition = "jsonb")
    private List<String> languages;

    @Type(JsonBinaryType.class)
    @Column(name = "professional_links", columnDefinition = "jsonb")
    private Map<String, String> professionalLinks;

    @Type(JsonBinaryType.class)
    @Column(name = "career_preferences", columnDefinition = "jsonb")
    private Map<String, Object> careerPreferences;

    @Column(name = "slug", length = 50, unique = true)
    private String slug;

    @Column(name = "slug_change_count", nullable = false)
    private Integer slugChangeCount = 0;

    @Column(name = "view_count", nullable = false)
    private Long viewCount = 0L;

    @Column(name = "default_visibility", nullable = false, length = 20)
    private String defaultVisibility = "private";

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<JobExperience> jobExperiences;

    @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Education> education;

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
