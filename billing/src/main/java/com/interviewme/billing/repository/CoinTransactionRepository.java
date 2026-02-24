package com.interviewme.billing.repository;

import com.interviewme.billing.model.CoinTransaction;
import com.interviewme.billing.model.RefType;
import com.interviewme.billing.model.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;

@Repository
public interface CoinTransactionRepository extends JpaRepository<CoinTransaction, Long> {

    Page<CoinTransaction> findByTenantIdOrderByCreatedAtDesc(Long tenantId, Pageable pageable);

    Page<CoinTransaction> findByTenantIdAndTypeOrderByCreatedAtDesc(
            Long tenantId, TransactionType type, Pageable pageable);

    boolean existsByRefTypeAndRefId(RefType refType, String refId);

    @Query("SELECT t FROM CoinTransaction t WHERE t.tenantId = :tenantId " +
           "AND (:type IS NULL OR t.type = :type) " +
           "AND (:refType IS NULL OR t.refType = :refType) " +
           "AND (:from IS NULL OR t.createdAt >= :from) " +
           "AND (:to IS NULL OR t.createdAt <= :to) " +
           "ORDER BY t.createdAt DESC")
    Page<CoinTransaction> findByFilters(
            @Param("tenantId") Long tenantId,
            @Param("type") TransactionType type,
            @Param("refType") RefType refType,
            @Param("from") Instant from,
            @Param("to") Instant to,
            Pageable pageable);
}
