package com.interviewme.repository;

import com.interviewme.model.VisitorChatLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VisitorChatLogRepository extends JpaRepository<VisitorChatLog, Long> {
    List<VisitorChatLog> findByVisitorSessionIdOrderByCreatedAtAsc(Long visitorSessionId);
    Page<VisitorChatLog> findByVisitorSessionId(Long visitorSessionId, Pageable pageable);
}
