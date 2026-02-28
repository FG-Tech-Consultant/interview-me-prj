package com.interviewme.integration;

import com.interviewme.billing.exception.InsufficientBalanceException;
import com.interviewme.billing.model.CoinTransaction;
import com.interviewme.billing.model.CoinWallet;
import com.interviewme.billing.model.RefType;
import com.interviewme.billing.repository.CoinWalletRepository;
import com.interviewme.billing.service.CoinWalletService;
import com.interviewme.model.Tenant;
import com.interviewme.repository.TenantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CoinWalletIntegrationTest extends AbstractIntegrationTest {

    @Autowired private CoinWalletService coinWalletService;
    @Autowired private CoinWalletRepository coinWalletRepository;
    @Autowired private TenantRepository tenantRepository;

    private Long tenantId;

    @BeforeEach
    void setUp() {
        Tenant tenant = new Tenant();
        tenant.setName("coin-tenant-" + System.nanoTime());
        tenant = tenantRepository.save(tenant);
        tenantId = tenant.getId();
    }

    @Test
    @Transactional
    void getOrCreateWallet_createsNewWallet() {
        CoinWallet wallet = coinWalletService.getOrCreateWallet(tenantId);
        assertThat(wallet).isNotNull();
        assertThat(wallet.getTenantId()).isEqualTo(tenantId);
        assertThat(wallet.getBalance()).isEqualTo(0L);
    }

    @Test
    @Transactional
    void getOrCreateWallet_returnsExisting() {
        CoinWallet first = coinWalletService.getOrCreateWallet(tenantId);
        CoinWallet second = coinWalletService.getOrCreateWallet(tenantId);
        assertThat(first.getId()).isEqualTo(second.getId());
    }

    @Test
    @Transactional
    void earn_increasesBalance() {
        coinWalletService.getOrCreateWallet(tenantId);

        CoinTransaction tx = coinWalletService.earn(tenantId, 100, RefType.ADMIN_GRANT, "grant-1", "Welcome bonus");
        assertThat(tx).isNotNull();
        assertThat(tx.getAmount()).isEqualTo(100);

        CoinWallet wallet = coinWalletService.getOrCreateWallet(tenantId);
        assertThat(wallet.getBalance()).isEqualTo(100L);
    }

    @Test
    @Transactional
    void spend_decreasesBalance() {
        coinWalletService.getOrCreateWallet(tenantId);
        coinWalletService.earn(tenantId, 100, RefType.ADMIN_GRANT, "grant-1", "Initial grant");

        CoinTransaction spendTx = coinWalletService.spend(tenantId, 30, RefType.EXPORT, "export-1", "Resume export");
        assertThat(spendTx.getAmount()).isEqualTo(-30);

        CoinWallet wallet = coinWalletService.getOrCreateWallet(tenantId);
        assertThat(wallet.getBalance()).isEqualTo(70L);
    }

    @Test
    @Transactional
    void spend_throwsOnInsufficientBalance() {
        coinWalletService.getOrCreateWallet(tenantId);
        coinWalletService.earn(tenantId, 10, RefType.ADMIN_GRANT, "grant-1", "Small grant");

        assertThatThrownBy(() ->
            coinWalletService.spend(tenantId, 50, RefType.EXPORT, "export-1", "Expensive export")
        ).isInstanceOf(InsufficientBalanceException.class);
    }

    @Test
    @Transactional
    void refund_restoresBalance() {
        coinWalletService.getOrCreateWallet(tenantId);
        coinWalletService.earn(tenantId, 100, RefType.ADMIN_GRANT, "grant-1", "Grant");
        CoinTransaction spendTx = coinWalletService.spend(tenantId, 30, RefType.EXPORT, "export-1", "Export");

        CoinTransaction refundTx = coinWalletService.refund(tenantId, spendTx.getId());
        assertThat(refundTx.getAmount()).isEqualTo(30);

        CoinWallet wallet = coinWalletService.getOrCreateWallet(tenantId);
        assertThat(wallet.getBalance()).isEqualTo(100L);
    }

    @Test
    @Transactional
    void multipleTransactions_correctBalance() {
        coinWalletService.getOrCreateWallet(tenantId);

        coinWalletService.earn(tenantId, 200, RefType.ADMIN_GRANT, "g1", "Grant 1");
        coinWalletService.spend(tenantId, 10, RefType.EXPORT, "e1", "Export 1");
        coinWalletService.spend(tenantId, 5, RefType.CHAT_MESSAGE, "c1", "Chat 1");
        coinWalletService.earn(tenantId, 50, RefType.ADMIN_GRANT, "g2", "Grant 2");
        coinWalletService.spend(tenantId, 3, RefType.LINKEDIN_DRAFT, "d1", "Draft 1");

        CoinWallet wallet = coinWalletService.getOrCreateWallet(tenantId);
        // 200 - 10 - 5 + 50 - 3 = 232
        assertThat(wallet.getBalance()).isEqualTo(232L);
    }
}
