package com.interviewme.billing.repository;

import com.interviewme.billing.model.CoinWallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CoinWalletRepository extends JpaRepository<CoinWallet, Long> {

    Optional<CoinWallet> findByTenantId(Long tenantId);

    boolean existsByTenantId(Long tenantId);
}
