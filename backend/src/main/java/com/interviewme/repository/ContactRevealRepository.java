package com.interviewme.repository;

import com.interviewme.model.ContactReveal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;

public interface ContactRevealRepository extends JpaRepository<ContactReveal, Long> {
    Optional<ContactReveal> findByTenantIdAndVisitorIdAndExpiresAtAfter(Long tenantId, Long visitorId, Instant now);
}
