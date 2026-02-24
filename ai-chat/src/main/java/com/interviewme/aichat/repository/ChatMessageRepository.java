package com.interviewme.aichat.repository;

import com.interviewme.aichat.model.ChatMessage;
import com.interviewme.aichat.model.ChatMessageRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findTop10BySessionIdOrderByCreatedAtDesc(Long sessionId);

    long countBySessionIdAndRoleAndCreatedAtAfter(Long sessionId, ChatMessageRole role, Instant after);

    long countByTenantIdAndCreatedAtBetween(Long tenantId, Instant start, Instant end);

    long countByTenantId(Long tenantId);
}
