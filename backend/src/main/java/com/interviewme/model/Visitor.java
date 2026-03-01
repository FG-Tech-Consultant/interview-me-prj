package com.interviewme.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Filter;

import java.time.Instant;

@Entity
@Table(name = "visitor")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class Visitor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "profile_id", nullable = false)
    private Long profileId;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, length = 200)
    private String company;

    @Column(name = "job_role", nullable = false, length = 200)
    private String jobRole;

    @Column(name = "linkedin_url", length = 500)
    private String linkedinUrl;

    @Column(name = "contact_email", length = 300)
    private String contactEmail;

    @Column(name = "contact_whatsapp", length = 50)
    private String contactWhatsapp;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "visitor_token", nullable = false, unique = true, length = 100)
    private String visitorToken;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}
