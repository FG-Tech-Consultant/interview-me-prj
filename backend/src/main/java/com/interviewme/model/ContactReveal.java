package com.interviewme.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "contact_reveal")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContactReveal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "visitor_id", nullable = false)
    private Long visitorId;

    @Column(name = "revealed_at", nullable = false)
    private Instant revealedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        if (revealedAt == null) revealedAt = Instant.now();
    }
}
