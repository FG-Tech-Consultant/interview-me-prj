package com.interviewme.exports.repository;

import com.interviewme.exports.model.ExportHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ExportHistoryRepository extends JpaRepository<ExportHistory, Long> {

    Page<ExportHistory> findByTenantIdOrderByCreatedAtDesc(Long tenantId, Pageable pageable);

    Optional<ExportHistory> findByIdAndTenantId(Long id, Long tenantId);

    Page<ExportHistory> findByTenantIdAndTypeOrderByCreatedAtDesc(Long tenantId, String type, Pageable pageable);

    Page<ExportHistory> findByTenantIdAndStatusOrderByCreatedAtDesc(Long tenantId, String status, Pageable pageable);

    @Query("SELECT e FROM ExportHistory e WHERE e.tenantId = :tenantId AND e.type = :type AND e.status = :status ORDER BY e.createdAt DESC")
    Page<ExportHistory> findByTenantIdAndTypeAndStatus(
            @Param("tenantId") Long tenantId,
            @Param("type") String type,
            @Param("status") String status,
            Pageable pageable);
}
