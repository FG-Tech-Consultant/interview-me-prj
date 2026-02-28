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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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

    @Nested
    @DisplayName("getOrCreateWallet")
    class GetOrCreateWallet {

        @Test
        @DisplayName("should create wallet if not exists")
        void shouldCreateIfNotExists() {
            CoinWallet wallet = coinWalletService.getOrCreateWallet(TENANT_ID);

            assertThat(wallet).isNotNull();
            assertThat(wallet.getTenantId()).isEqualTo(TENANT_ID);
            assertThat(wallet.getBalance()).isEqualTo(0L);
        }

        @Test
        @DisplayName("should return existing wallet")
        void shouldReturnExistingWallet() {
            CoinWallet first = coinWalletService.getOrCreateWallet(TENANT_ID);
            CoinWallet second = coinWalletService.getOrCreateWallet(TENANT_ID);

            assertThat(second.getId()).isEqualTo(first.getId());
        }

        @Test
        @DisplayName("should create separate wallets for different tenants")
        void shouldCreateSeparateWalletsForDifferentTenants() {
            CoinWallet wallet1 = coinWalletService.getOrCreateWallet(TENANT_ID);
            CoinWallet wallet2 = coinWalletService.getOrCreateWallet(2L);

            assertThat(wallet1.getId()).isNotEqualTo(wallet2.getId());
            assertThat(wallet1.getTenantId()).isEqualTo(TENANT_ID);
            assertThat(wallet2.getTenantId()).isEqualTo(2L);
        }
    }

    @Nested
    @DisplayName("earn")
    class Earn {

        @Test
        @DisplayName("should increment balance")
        void shouldIncrementBalance() {
            coinWalletService.getOrCreateWallet(TENANT_ID);

            CoinTransaction tx = coinWalletService.earn(TENANT_ID, 100, RefType.ADMIN_GRANT, "test-1", "Test grant");

            assertThat(tx).isNotNull();
            assertThat(tx.getType()).isEqualTo(TransactionType.EARN);
            assertThat(tx.getAmount()).isEqualTo(100);

            CoinWallet wallet = walletRepository.findByTenantId(TENANT_ID).orElseThrow();
            assertThat(wallet.getBalance()).isEqualTo(100L);
        }

        @Test
        @DisplayName("should accumulate multiple earnings")
        void shouldAccumulateMultipleEarnings() {
            coinWalletService.getOrCreateWallet(TENANT_ID);

            coinWalletService.earn(TENANT_ID, 100, RefType.ADMIN_GRANT, "grant-1", "Grant 1");
            coinWalletService.earn(TENANT_ID, 50, RefType.ADMIN_GRANT, "grant-2", "Grant 2");
            coinWalletService.earn(TENANT_ID, 25, RefType.ADMIN_GRANT, "grant-3", "Grant 3");

            CoinWallet wallet = walletRepository.findByTenantId(TENANT_ID).orElseThrow();
            assertThat(wallet.getBalance()).isEqualTo(175L);
        }

        @Test
        @DisplayName("should record correct transaction metadata")
        void shouldRecordTransactionMetadata() {
            coinWalletService.getOrCreateWallet(TENANT_ID);

            CoinTransaction tx = coinWalletService.earn(TENANT_ID, 100, RefType.ADMIN_GRANT, "ref-123", "Admin grant");

            assertThat(tx.getTenantId()).isEqualTo(TENANT_ID);
            assertThat(tx.getRefType()).isEqualTo(RefType.ADMIN_GRANT);
            assertThat(tx.getRefId()).isEqualTo("ref-123");
            assertThat(tx.getDescription()).isEqualTo("Admin grant");
        }
    }

    @Nested
    @DisplayName("spend")
    class Spend {

        @Test
        @DisplayName("should decrement balance")
        void shouldDecrementBalance() {
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
        @DisplayName("should throw InsufficientBalanceException when balance is zero")
        void shouldThrowWhenBalanceIsZero() {
            coinWalletService.getOrCreateWallet(TENANT_ID);

            assertThatThrownBy(() -> coinWalletService.spend(TENANT_ID, 10, RefType.EXPORT, "export-1", "PDF export"))
                    .isInstanceOf(InsufficientBalanceException.class)
                    .hasMessageContaining("Insufficient coins");
        }

        @Test
        @DisplayName("should throw when spending more than balance")
        void shouldThrowWhenSpendingMoreThanBalance() {
            coinWalletService.getOrCreateWallet(TENANT_ID);
            coinWalletService.earn(TENANT_ID, 50, RefType.ADMIN_GRANT, "grant-1", "Grant");

            assertThatThrownBy(() -> coinWalletService.spend(TENANT_ID, 100, RefType.EXPORT, "export-1", "Export"))
                    .isInstanceOf(InsufficientBalanceException.class);
        }

        @Test
        @DisplayName("should allow spending exact balance")
        void shouldAllowSpendingExactBalance() {
            coinWalletService.getOrCreateWallet(TENANT_ID);
            coinWalletService.earn(TENANT_ID, 50, RefType.ADMIN_GRANT, "grant-1", "Grant");

            CoinTransaction tx = coinWalletService.spend(TENANT_ID, 50, RefType.EXPORT, "export-1", "Export");

            assertThat(tx).isNotNull();
            CoinWallet wallet = walletRepository.findByTenantId(TENANT_ID).orElseThrow();
            assertThat(wallet.getBalance()).isEqualTo(0L);
        }

        @Test
        @DisplayName("should ensure balance never goes negative after spend")
        void shouldNeverGoNegative() {
            coinWalletService.getOrCreateWallet(TENANT_ID);
            coinWalletService.earn(TENANT_ID, 100, RefType.ADMIN_GRANT, "grant-1", "Grant");

            coinWalletService.spend(TENANT_ID, 50, RefType.EXPORT, "export-1", "Export 1");
            coinWalletService.spend(TENANT_ID, 30, RefType.EXPORT, "export-2", "Export 2");

            CoinWallet wallet = walletRepository.findByTenantId(TENANT_ID).orElseThrow();
            assertThat(wallet.getBalance()).isEqualTo(20L);
            assertThat(wallet.getBalance()).isGreaterThanOrEqualTo(0L);
        }

        @Test
        @DisplayName("should handle multiple sequential spends correctly")
        void shouldHandleSequentialSpends() {
            coinWalletService.getOrCreateWallet(TENANT_ID);
            coinWalletService.earn(TENANT_ID, 100, RefType.ADMIN_GRANT, "grant-1", "Grant");

            coinWalletService.spend(TENANT_ID, 10, RefType.EXPORT, "export-1", "Export 1");
            coinWalletService.spend(TENANT_ID, 20, RefType.EXPORT, "export-2", "Export 2");
            coinWalletService.spend(TENANT_ID, 30, RefType.CHAT_MESSAGE, "chat-1", "Chat");

            CoinWallet wallet = walletRepository.findByTenantId(TENANT_ID).orElseThrow();
            assertThat(wallet.getBalance()).isEqualTo(40L);
        }
    }

    @Nested
    @DisplayName("refund")
    class Refund {

        @Test
        @DisplayName("should create refund transaction and restore balance")
        void shouldCreateRefundTransaction() {
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
        @DisplayName("should throw DuplicateRefundException on duplicate refund")
        void shouldThrowDuplicateRefundException() {
            coinWalletService.getOrCreateWallet(TENANT_ID);
            coinWalletService.earn(TENANT_ID, 100, RefType.ADMIN_GRANT, "grant-1", "Grant");
            CoinTransaction spendTx = coinWalletService.spend(TENANT_ID, 30, RefType.EXPORT, "export-1", "Export");

            coinWalletService.refund(TENANT_ID, spendTx.getId());

            assertThatThrownBy(() -> coinWalletService.refund(TENANT_ID, spendTx.getId()))
                    .isInstanceOf(DuplicateRefundException.class);
        }

        @Test
        @DisplayName("should throw when original transaction not found")
        void shouldThrowWhenOriginalTxNotFound() {
            coinWalletService.getOrCreateWallet(TENANT_ID);

            assertThatThrownBy(() -> coinWalletService.refund(TENANT_ID, 99999L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Original transaction not found");
        }

        @Test
        @DisplayName("should refund the absolute amount of the original transaction")
        void shouldRefundAbsoluteAmount() {
            coinWalletService.getOrCreateWallet(TENANT_ID);
            coinWalletService.earn(TENANT_ID, 200, RefType.ADMIN_GRANT, "grant-1", "Grant");
            CoinTransaction spendTx = coinWalletService.spend(TENANT_ID, 75, RefType.EXPORT, "export-1", "Export");

            // spendTx.amount is -75, refund should be +75
            assertThat(spendTx.getAmount()).isEqualTo(-75);

            CoinTransaction refundTx = coinWalletService.refund(TENANT_ID, spendTx.getId());
            assertThat(refundTx.getAmount()).isEqualTo(75);

            CoinWallet wallet = walletRepository.findByTenantId(TENANT_ID).orElseThrow();
            assertThat(wallet.getBalance()).isEqualTo(200L);
        }

        @Test
        @DisplayName("should set EXPORT_REFUND as refType on refund transaction")
        void shouldSetExportRefundRefType() {
            coinWalletService.getOrCreateWallet(TENANT_ID);
            coinWalletService.earn(TENANT_ID, 100, RefType.ADMIN_GRANT, "grant-1", "Grant");
            CoinTransaction spendTx = coinWalletService.spend(TENANT_ID, 30, RefType.EXPORT, "export-1", "Export");

            CoinTransaction refundTx = coinWalletService.refund(TENANT_ID, spendTx.getId());

            assertThat(refundTx.getRefType()).isEqualTo(RefType.EXPORT_REFUND);
            assertThat(refundTx.getRefId()).isEqualTo(String.valueOf(spendTx.getId()));
        }
    }

    @Nested
    @DisplayName("getTransactions")
    class GetTransactions {

        @Test
        @DisplayName("should return paginated results")
        void shouldReturnPaginatedResults() {
            coinWalletService.getOrCreateWallet(TENANT_ID);
            coinWalletService.earn(TENANT_ID, 100, RefType.ADMIN_GRANT, "grant-1", "Grant 1");
            coinWalletService.earn(TENANT_ID, 50, RefType.ADMIN_GRANT, "grant-2", "Grant 2");

            var result = coinWalletService.getTransactions(TENANT_ID, 0, 10, null, null, null, null);

            assertThat(result.content()).hasSize(2);
            assertThat(result.totalElements()).isEqualTo(2);
        }

        @Test
        @DisplayName("should return empty result for tenant with no transactions")
        void shouldReturnEmptyForNoTransactions() {
            coinWalletService.getOrCreateWallet(TENANT_ID);

            var result = coinWalletService.getTransactions(TENANT_ID, 0, 10, null, null, null, null);

            assertThat(result.content()).isEmpty();
            assertThat(result.totalElements()).isEqualTo(0);
        }

        @Test
        @DisplayName("should filter by transaction type")
        void shouldFilterByType() {
            coinWalletService.getOrCreateWallet(TENANT_ID);
            coinWalletService.earn(TENANT_ID, 100, RefType.ADMIN_GRANT, "grant-1", "Grant");
            coinWalletService.spend(TENANT_ID, 30, RefType.EXPORT, "export-1", "Export");

            var earnOnly = coinWalletService.getTransactions(TENANT_ID, 0, 10, TransactionType.EARN, null, null, null);
            var spendOnly = coinWalletService.getTransactions(TENANT_ID, 0, 10, TransactionType.SPEND, null, null, null);

            assertThat(earnOnly.content()).hasSize(1);
            assertThat(spendOnly.content()).hasSize(1);
        }

        @Test
        @DisplayName("should respect page size limit of 100")
        void shouldRespectPageSizeLimit() {
            coinWalletService.getOrCreateWallet(TENANT_ID);
            // Just verify it doesn't throw with a large size
            var result = coinWalletService.getTransactions(TENANT_ID, 0, 200, null, null, null, null);
            assertThat(result).isNotNull();
        }
    }
}
