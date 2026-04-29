package com.interviewme.repository;

import com.interviewme.model.JobPosting;
import com.interviewme.model.JobPostingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JobPostingRepository extends JpaRepository<JobPosting, Long> {

    Optional<JobPosting> findBySlugAndDeletedAtIsNull(String slug);

    Optional<JobPosting> findBySlugAndStatusAndDeletedAtIsNull(String slug, JobPostingStatus status);

    List<JobPosting> findByTenantIdAndDeletedAtIsNullOrderByCreatedAtDesc(Long tenantId);

    List<JobPosting> findByTenantIdAndStatusAndDeletedAtIsNullOrderByCreatedAtDesc(Long tenantId, JobPostingStatus status);

    List<JobPosting> findByCompanyIdAndDeletedAtIsNullOrderByCreatedAtDesc(Long companyId);

    boolean existsBySlugAndDeletedAtIsNull(String slug);
}
