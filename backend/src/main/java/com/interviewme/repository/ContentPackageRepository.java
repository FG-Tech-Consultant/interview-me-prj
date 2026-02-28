package com.interviewme.repository;

import com.interviewme.model.ContentPackage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ContentPackageRepository extends JpaRepository<ContentPackage, Long> {

    List<ContentPackage> findByProfileIdAndDeletedAtIsNullOrderByCreatedAtDesc(Long profileId);

    Optional<ContentPackage> findByIdAndDeletedAtIsNull(Long id);

    @Query(value = "SELECT * FROM content_package WHERE slug = :slug AND deleted_at IS NULL", nativeQuery = true)
    Optional<ContentPackage> findBySlugGlobally(@Param("slug") String slug);

    @Query(value = "SELECT * FROM content_package WHERE access_token = :accessToken AND deleted_at IS NULL", nativeQuery = true)
    Optional<ContentPackage> findByAccessTokenGlobally(@Param("accessToken") String accessToken);

    @Query(value = "SELECT EXISTS(SELECT 1 FROM content_package WHERE slug = :slug AND deleted_at IS NULL)", nativeQuery = true)
    boolean existsBySlugGlobally(@Param("slug") String slug);
}
