package com.interviewme.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Filter;

import java.time.Instant;

@Entity
@Table(name = "visitor_chat_log")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class VisitorChatLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "visitor_session_id", nullable = false)
    private Long visitorSessionId;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(nullable = false, length = 20)
    private String role; // VISITOR or ASSISTANT

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "tokens_used")
    private Integer tokensUsed;

    @Column(name = "llm_request", columnDefinition = "TEXT")
    private String llmRequest;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}
