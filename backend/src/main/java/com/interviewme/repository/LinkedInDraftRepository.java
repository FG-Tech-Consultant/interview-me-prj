package com.interviewme.repository;

import com.interviewme.model.LinkedInDraft;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LinkedInDraftRepository extends JpaRepository<LinkedInDraft, Long> {

    Page<LinkedInDraft> findByProfileIdAndDeletedAtIsNullOrderByCreatedAtDesc(Long profileId, Pageable pageable);

    Optional<LinkedInDraft> findByIdAndDeletedAtIsNull(Long id);
}
