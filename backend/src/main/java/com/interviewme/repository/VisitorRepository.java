package com.interviewme.repository;

import com.interviewme.model.Visitor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface VisitorRepository extends JpaRepository<Visitor, Long> {
    Optional<Visitor> findByVisitorToken(String visitorToken);
    Page<Visitor> findByProfileId(Long profileId, Pageable pageable);
    Page<Visitor> findByTenantId(Long tenantId, Pageable pageable);
    long countByProfileId(Long profileId);

    @Query("SELECT COUNT(DISTINCT v.id) FROM Visitor v JOIN VisitorSession vs ON v.id = vs.visitorId WHERE v.profileId = :profileId AND vs.messageCount > 0")
    long countChatVisitorsByProfileId(Long profileId);

    @Query("SELECT v FROM Visitor v ORDER BY v.createdAt DESC")
    Page<Visitor> findAllOrderByCreatedAtDesc(Pageable pageable);

    @Query(value = "SELECT COUNT(*) FROM visitor", nativeQuery = true)
    long countAllVisitors();
}
