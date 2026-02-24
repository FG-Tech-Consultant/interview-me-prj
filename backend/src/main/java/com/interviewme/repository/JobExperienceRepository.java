package com.interviewme.repository;

import com.interviewme.model.JobExperience;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JobExperienceRepository extends JpaRepository<JobExperience, Long> {

    @Query("SELECT j FROM JobExperience j WHERE j.profileId = :profileId AND j.deletedAt IS NULL ORDER BY j.startDate DESC")
    List<JobExperience> findByProfileIdAndDeletedAtIsNullOrderByStartDateDesc(@Param("profileId") Long profileId);

    @Query("SELECT j FROM JobExperience j WHERE j.profileId = :profileId AND j.visibility = :visibility AND j.deletedAt IS NULL ORDER BY j.startDate DESC")
    List<JobExperience> findByProfileIdAndVisibilityAndDeletedAtIsNull(
        @Param("profileId") Long profileId,
        @Param("visibility") String visibility
    );

    Optional<JobExperience> findByIdAndDeletedAtIsNull(Long id);

    Optional<JobExperience> findByIdAndProfileIdAndDeletedAtIsNull(Long id, Long profileId);
}
