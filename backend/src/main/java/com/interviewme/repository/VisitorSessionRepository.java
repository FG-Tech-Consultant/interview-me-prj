package com.interviewme.repository;

import com.interviewme.model.VisitorSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VisitorSessionRepository extends JpaRepository<VisitorSession, Long> {
    Optional<VisitorSession> findBySessionToken(String sessionToken);
    Page<VisitorSession> findByVisitorId(Long visitorId, Pageable pageable);
    List<VisitorSession> findByVisitorIdOrderByStartedAtDesc(Long visitorId);
    Page<VisitorSession> findByTenantId(Long tenantId, Pageable pageable);
}
