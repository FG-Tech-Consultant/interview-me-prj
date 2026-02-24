package com.interviewme.linkedin.repository;

import com.interviewme.linkedin.model.LinkedInSectionScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LinkedInSectionScoreRepository extends JpaRepository<LinkedInSectionScore, Long> {

    List<LinkedInSectionScore> findByAnalysisIdOrderBySectionName(Long analysisId);

    Optional<LinkedInSectionScore> findByAnalysisIdAndSectionName(Long analysisId, String sectionName);
}
