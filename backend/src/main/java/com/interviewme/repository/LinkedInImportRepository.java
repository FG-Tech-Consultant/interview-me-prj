package com.interviewme.repository;

import com.interviewme.model.LinkedInImport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LinkedInImportRepository extends JpaRepository<LinkedInImport, Long> {

    @Query("SELECT li FROM LinkedInImport li WHERE li.tenantId = :tenantId AND li.profileId = :profileId ORDER BY li.createdAt DESC")
    List<LinkedInImport> findByTenantIdAndProfileIdOrderByCreatedAtDesc(
            @Param("tenantId") Long tenantId,
            @Param("profileId") Long profileId);
}
