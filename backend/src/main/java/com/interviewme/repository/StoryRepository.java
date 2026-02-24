package com.interviewme.repository;

import com.interviewme.model.Story;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StoryRepository extends JpaRepository<Story, Long> {

    @Query("SELECT s FROM Story s WHERE s.experienceProjectId = :projectId AND s.deletedAt IS NULL ORDER BY s.createdAt DESC")
    List<Story> findByExperienceProjectIdAndDeletedAtIsNullOrderByCreatedAtDesc(
        @Param("projectId") Long projectId);

    Optional<Story> findByIdAndDeletedAtIsNull(Long id);

    @Query("SELECT s FROM Story s WHERE s.experienceProjectId IN :projectIds AND s.deletedAt IS NULL")
    List<Story> findByExperienceProjectIdInAndDeletedAtIsNull(
        @Param("projectIds") List<Long> projectIds);

    @Query("SELECT COUNT(s) FROM Story s WHERE s.experienceProjectId = :projectId AND s.deletedAt IS NULL")
    int countByExperienceProjectIdAndDeletedAtIsNull(@Param("projectId") Long projectId);

    @Query("""
        SELECT s FROM Story s
        JOIN ExperienceProject p ON s.experienceProjectId = p.id
        JOIN com.interviewme.profile.model.JobExperience j ON p.jobExperienceId = j.id
        WHERE j.profileId = :profileId
        AND s.visibility = 'public'
        AND s.deletedAt IS NULL
        AND p.deletedAt IS NULL
        AND j.deletedAt IS NULL
        ORDER BY s.createdAt DESC
        """)
    List<Story> findPublicStoriesByProfileId(@Param("profileId") Long profileId);

    @Query("SELECT s FROM Story s WHERE s.experienceProjectId IN :projectIds AND s.visibility = :visibility AND s.deletedAt IS NULL")
    List<Story> findByExperienceProjectIdInAndVisibilityAndDeletedAtIsNull(
        @Param("projectIds") List<Long> projectIds,
        @Param("visibility") String visibility);
}
