package com.interviewme.repository;

import com.interviewme.model.ExperienceProject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExperienceProjectRepository extends JpaRepository<ExperienceProject, Long> {

    @Query("SELECT p FROM ExperienceProject p WHERE p.jobExperienceId = :jobExperienceId AND p.deletedAt IS NULL ORDER BY p.createdAt DESC")
    List<ExperienceProject> findByJobExperienceIdAndDeletedAtIsNullOrderByCreatedAtDesc(
        @Param("jobExperienceId") Long jobExperienceId);

    Optional<ExperienceProject> findByIdAndDeletedAtIsNull(Long id);

    @Query("SELECT p FROM ExperienceProject p WHERE p.jobExperienceId IN :jobExperienceIds AND p.deletedAt IS NULL")
    List<ExperienceProject> findByJobExperienceIdInAndDeletedAtIsNull(
        @Param("jobExperienceIds") List<Long> jobExperienceIds);

    @Query("SELECT p FROM ExperienceProject p WHERE p.jobExperienceId IN :jobExperienceIds AND p.visibility = :visibility AND p.deletedAt IS NULL")
    List<ExperienceProject> findByJobExperienceIdInAndVisibilityAndDeletedAtIsNull(
        @Param("jobExperienceIds") List<Long> jobExperienceIds,
        @Param("visibility") String visibility);
}
