package com.interviewme.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "experience_project_skill")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExperienceProjectSkill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "experience_project_id", nullable = false)
    private Long experienceProjectId;

    @Column(name = "user_skill_id", nullable = false)
    private Long userSkillId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}
