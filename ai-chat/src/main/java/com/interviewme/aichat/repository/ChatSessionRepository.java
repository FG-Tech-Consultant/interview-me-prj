package com.interviewme.aichat.repository;

import com.interviewme.aichat.model.ChatSession;
import com.interviewme.aichat.model.ChatSessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {

    Optional<ChatSession> findBySessionTokenAndStatus(UUID sessionToken, ChatSessionStatus status);

    long countByTenantIdAndCreatedAtAfter(Long tenantId, Instant after);

    long countByTenantIdAndCreatedAtBetween(Long tenantId, Instant start, Instant end);
}
