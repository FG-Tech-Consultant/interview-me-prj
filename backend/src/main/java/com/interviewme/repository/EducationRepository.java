package com.interviewme.repository;

import com.interviewme.model.Education;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EducationRepository extends JpaRepository<Education, Long> {

    @Query("SELECT e FROM Education e WHERE e.profileId = :profileId AND e.deletedAt IS NULL ORDER BY e.endDate DESC")
    List<Education> findByProfileIdAndDeletedAtIsNullOrderByEndDateDesc(@Param("profileId") Long profileId);

    @Query("SELECT e FROM Education e WHERE e.profileId = :profileId AND e.visibility = :visibility AND e.deletedAt IS NULL ORDER BY e.endDate DESC")
    List<Education> findByProfileIdAndVisibilityAndDeletedAtIsNull(
        @Param("profileId") Long profileId,
        @Param("visibility") String visibility
    );

    Optional<Education> findByIdAndDeletedAtIsNull(Long id);

    @Query("SELECT e FROM Education e WHERE e.profileId = :profileId AND e.deletedAt IS NULL ORDER BY e.startDate DESC")
    List<Education> findByProfileIdAndDeletedAtIsNullOrderByStartDateDesc(@Param("profileId") Long profileId);

    Optional<Education> findByIdAndProfileIdAndDeletedAtIsNull(Long id, Long profileId);
}
