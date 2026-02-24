package com.interviewme.linkedin.repository;

import com.interviewme.linkedin.model.LinkedInAnalysis;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LinkedInAnalysisRepository extends JpaRepository<LinkedInAnalysis, Long> {

    Optional<LinkedInAnalysis> findByIdAndTenantId(Long id, Long tenantId);

    Page<LinkedInAnalysis> findByProfileIdAndTenantIdOrderByCreatedAtDesc(Long profileId, Long tenantId, Pageable pageable);

    List<LinkedInAnalysis> findByStatusIn(List<String> statuses);
}
