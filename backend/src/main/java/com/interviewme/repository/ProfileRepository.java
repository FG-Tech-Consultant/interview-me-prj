package com.interviewme.repository;

import com.interviewme.model.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProfileRepository extends JpaRepository<Profile, Long> {

    Optional<Profile> findByUserIdAndDeletedAtIsNull(Long userId);

    boolean existsByUserIdAndDeletedAtIsNull(Long userId);

    Optional<Profile> findByIdAndDeletedAtIsNull(Long id);

    @Query("SELECT p FROM Profile p WHERE p.id = :id AND p.deletedAt IS NULL AND p.tenantId = :tenantId")
    Optional<Profile> findByIdAndTenantId(@Param("id") Long id, @Param("tenantId") Long tenantId);

    Optional<Profile> findBySlugAndDeletedAtIsNull(String slug);

    boolean existsBySlug(String slug);
}
