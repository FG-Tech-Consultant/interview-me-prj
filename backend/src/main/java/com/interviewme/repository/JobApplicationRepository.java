package com.interviewme.repository;

import com.interviewme.model.ApplicationStatus;
import com.interviewme.model.JobApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JobApplicationRepository extends JpaRepository<JobApplication, Long> {

    Optional<JobApplication> findByChatSessionToken(UUID chatSessionToken);

    @Query("SELECT ja FROM JobApplication ja WHERE ja.candidateEmail = :email " +
           "AND ja.jobPostingId = :jobPostingId AND ja.deletedAt IS NULL")
    Optional<JobApplication> findByEmailAndJobPosting(
        @Param("email") String email,
        @Param("jobPostingId") Long jobPostingId);

    List<JobApplication> findByJobPostingIdAndDeletedAtIsNull(Long jobPostingId);

    List<JobApplication> findByTenantIdAndStatusAndDeletedAtIsNull(Long tenantId, ApplicationStatus status);

    long countByJobPostingIdAndDeletedAtIsNull(Long jobPostingId);
}
