package com.interviewme.billing.service;

import com.interviewme.billing.config.BillingProperties;
import com.interviewme.billing.exception.DuplicateRefundException;
import com.interviewme.billing.exception.InsufficientBalanceException;
import com.interviewme.billing.model.*;
import com.interviewme.billing.repository.CoinTransactionRepository;
import com.interviewme.billing.repository.CoinWalletRepository;
import com.interviewme.security.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CoinWalletServiceTest {

    @Autowired
    private CoinWalletService coinWalletService;

    @Autowired
    private CoinWalletRepository walletRepository;

    @Autowired
    private CoinTransactionRepository transactionRepository;

    private static final Long TENANT_ID = 1L;

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(TENANT_ID);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void getOrCreateWallet_shouldCreateIfNotExists() {
        CoinWallet wallet = coinWalletService.getOrCreateWallet(TENANT_ID);

        assertThat(wallet).isNotNull();
        assertThat(wallet.getTenantId()).isEqualTo(TENANT_ID);
        assertThat(wallet.getBalance()).isEqualTo(0L);
    }

    @Test
    void getOrCreateWallet_shouldReturnExistingWallet() {
        CoinWallet first = coinWalletService.getOrCreateWallet(TENANT_ID);
        CoinWallet second = coinWalletService.getOrCreateWallet(TENANT_ID);

        assertThat(second.getId()).isEqualTo(first.getId());
    }

    @Test
    void earn_shouldIncrementBalance() {
        coinWalletService.getOrCreateWallet(TENANT_ID);

        CoinTransaction tx = coinWalletService.earn(TENANT_ID, 100, RefType.ADMIN_GRANT, "test-1", "Test grant");

        assertThat(tx).isNotNull();
        assertThat(tx.getType()).isEqualTo(TransactionType.EARN);
        assertThat(tx.getAmount()).isEqualTo(100);

        CoinWallet wallet = walletRepository.findByTenantId(TENANT_ID).orElseThrow();
        assertThat(wallet.getBalance()).isEqualTo(100L);
    }

    @Test
    void spend_shouldDecrementBalance() {
        coinWalletService.getOrCreateWallet(TENANT_ID);
        coinWalletService.earn(TENANT_ID, 100, RefType.ADMIN_GRANT, "grant-1", "Initial grant");

        CoinTransaction tx = coinWalletService.spend(TENANT_ID, 30, RefType.EXPORT, "export-1", "PDF export");

        assertThat(tx).isNotNull();
        assertThat(tx.getType()).isEqualTo(TransactionType.SPEND);
        assertThat(tx.getAmount()).isEqualTo(-30);

        CoinWallet wallet = walletRepository.findByTenantId(TENANT_ID).orElseThrow();
        assertThat(wallet.getBalance()).isEqualTo(70L);
    }

    @Test
    void spend_shouldThrowInsufficientBalanceException() {
        coinWalletService.getOrCreateWallet(TENANT_ID);

        assertThatThrownBy(() -> coinWalletService.spend(TENANT_ID, 10, RefType.EXPORT, "export-1", "PDF export"))
                .isInstanceOf(InsufficientBalanceException.class)
                .hasMessageContaining("Insufficient coins");
    }

    @Test
    void refund_shouldCreateRefundTransaction() {
        coinWalletService.getOrCreateWallet(TENANT_ID);
        coinWalletService.earn(TENANT_ID, 100, RefType.ADMIN_GRANT, "grant-1", "Grant");
        CoinTransaction spendTx = coinWalletService.spend(TENANT_ID, 30, RefType.EXPORT, "export-1", "Export");

        CoinTransaction refundTx = coinWalletService.refund(TENANT_ID, spendTx.getId());

        assertThat(refundTx).isNotNull();
        assertThat(refundTx.getType()).isEqualTo(TransactionType.REFUND);
        assertThat(refundTx.getAmount()).isEqualTo(30);

        CoinWallet wallet = walletRepository.findByTenantId(TENANT_ID).orElseThrow();
        assertThat(wallet.getBalance()).isEqualTo(100L);
    }

    @Test
    void refund_shouldThrowDuplicateRefundException() {
        coinWalletService.getOrCreateWallet(TENANT_ID);
        coinWalletService.earn(TENANT_ID, 100, RefType.ADMIN_GRANT, "grant-1", "Grant");
        CoinTransaction spendTx = coinWalletService.spend(TENANT_ID, 30, RefType.EXPORT, "export-1", "Export");

        coinWalletService.refund(TENANT_ID, spendTx.getId());

        assertThatThrownBy(() -> coinWalletService.refund(TENANT_ID, spendTx.getId()))
                .isInstanceOf(DuplicateRefundException.class);
    }

    @Test
    void getTransactions_shouldReturnPaginatedResults() {
        coinWalletService.getOrCreateWallet(TENANT_ID);
        coinWalletService.earn(TENANT_ID, 100, RefType.ADMIN_GRANT, "grant-1", "Grant 1");
        coinWalletService.earn(TENANT_ID, 50, RefType.ADMIN_GRANT, "grant-2", "Grant 2");

        var result = coinWalletService.getTransactions(TENANT_ID, 0, 10, null, null, null, null);

        assertThat(result.content()).hasSize(2);
        assertThat(result.totalElements()).isEqualTo(2);
    }
}
