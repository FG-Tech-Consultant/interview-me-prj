package com.interviewme.billing.mapper;

import com.interviewme.billing.dto.TransactionPageResponse;
import com.interviewme.billing.dto.TransactionResponse;
import com.interviewme.billing.dto.WalletResponse;
import com.interviewme.billing.model.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class BillingMapperTest {

    @Nested
    @DisplayName("toWalletResponse")
    class ToWalletResponse {

        @Test
        @DisplayName("should map all fields from CoinWallet")
        void shouldMapAllFields() {
            CoinWallet wallet = createTestWallet();

            WalletResponse response = BillingMapper.toWalletResponse(wallet);

            assertThat(response.id()).isEqualTo(1L);
            assertThat(response.tenantId()).isEqualTo(10L);
            assertThat(response.balance()).isEqualTo(500L);
            assertThat(response.createdAt()).isEqualTo(wallet.getCreatedAt());
            assertThat(response.updatedAt()).isEqualTo(wallet.getUpdatedAt());
        }

        @Test
        @DisplayName("should handle zero balance")
        void shouldHandleZeroBalance() {
            CoinWallet wallet = createTestWallet();
            wallet.setBalance(0L);

            WalletResponse response = BillingMapper.toWalletResponse(wallet);

            assertThat(response.balance()).isEqualTo(0L);
        }

        @Test
        @DisplayName("should handle null timestamps")
        void shouldHandleNullTimestamps() {
            CoinWallet wallet = createTestWallet();
            wallet.setCreatedAt(null);
            wallet.setUpdatedAt(null);

            WalletResponse response = BillingMapper.toWalletResponse(wallet);

            assertThat(response.createdAt()).isNull();
            assertThat(response.updatedAt()).isNull();
        }
    }

    @Nested
    @DisplayName("toTransactionResponse")
    class ToTransactionResponse {

        @Test
        @DisplayName("should map all fields from CoinTransaction")
        void shouldMapAllFields() {
            CoinTransaction tx = createTestTransaction(TransactionType.EARN, 100, RefType.ADMIN_GRANT);

            TransactionResponse response = BillingMapper.toTransactionResponse(tx);

            assertThat(response.id()).isEqualTo(1L);
            assertThat(response.type()).isEqualTo("EARN");
            assertThat(response.amount()).isEqualTo(100);
            assertThat(response.description()).isEqualTo("Test transaction");
            assertThat(response.refType()).isEqualTo("ADMIN_GRANT");
            assertThat(response.refId()).isEqualTo("ref-001");
            assertThat(response.createdAt()).isEqualTo(tx.getCreatedAt());
        }

        @Test
        @DisplayName("should map SPEND transaction with negative amount")
        void shouldMapSpendTransaction() {
            CoinTransaction tx = createTestTransaction(TransactionType.SPEND, -30, RefType.EXPORT);

            TransactionResponse response = BillingMapper.toTransactionResponse(tx);

            assertThat(response.type()).isEqualTo("SPEND");
            assertThat(response.amount()).isEqualTo(-30);
            assertThat(response.refType()).isEqualTo("EXPORT");
        }

        @Test
        @DisplayName("should map REFUND transaction")
        void shouldMapRefundTransaction() {
            CoinTransaction tx = createTestTransaction(TransactionType.REFUND, 30, RefType.EXPORT_REFUND);

            TransactionResponse response = BillingMapper.toTransactionResponse(tx);

            assertThat(response.type()).isEqualTo("REFUND");
            assertThat(response.amount()).isEqualTo(30);
        }

        @Test
        @DisplayName("should handle null refType")
        void shouldHandleNullRefType() {
            CoinTransaction tx = createTestTransaction(TransactionType.EARN, 100, null);

            TransactionResponse response = BillingMapper.toTransactionResponse(tx);

            assertThat(response.refType()).isNull();
        }

        @Test
        @DisplayName("should handle null description")
        void shouldHandleNullDescription() {
            CoinTransaction tx = createTestTransaction(TransactionType.EARN, 100, RefType.ADMIN_GRANT);
            tx.setDescription(null);

            TransactionResponse response = BillingMapper.toTransactionResponse(tx);

            assertThat(response.description()).isNull();
        }
    }

    @Nested
    @DisplayName("toPageResponse")
    class ToPageResponse {

        @Test
        @DisplayName("should map page with transactions")
        void shouldMapPageWithTransactions() {
            CoinTransaction tx1 = createTestTransaction(TransactionType.EARN, 100, RefType.ADMIN_GRANT);
            tx1.setId(1L);
            CoinTransaction tx2 = createTestTransaction(TransactionType.SPEND, -30, RefType.EXPORT);
            tx2.setId(2L);

            Page<CoinTransaction> page = new PageImpl<>(
                    List.of(tx1, tx2), PageRequest.of(0, 10), 2
            );

            TransactionPageResponse response = BillingMapper.toPageResponse(page);

            assertThat(response.content()).hasSize(2);
            assertThat(response.page()).isEqualTo(0);
            assertThat(response.size()).isEqualTo(10);
            assertThat(response.totalElements()).isEqualTo(2);
            assertThat(response.totalPages()).isEqualTo(1);
        }

        @Test
        @DisplayName("should handle empty page")
        void shouldHandleEmptyPage() {
            Page<CoinTransaction> page = new PageImpl<>(
                    Collections.emptyList(), PageRequest.of(0, 10), 0
            );

            TransactionPageResponse response = BillingMapper.toPageResponse(page);

            assertThat(response.content()).isEmpty();
            assertThat(response.totalElements()).isEqualTo(0);
            assertThat(response.totalPages()).isEqualTo(0);
        }

        @Test
        @DisplayName("should map pagination info for multi-page results")
        void shouldMapMultiPageResults() {
            CoinTransaction tx = createTestTransaction(TransactionType.EARN, 100, RefType.ADMIN_GRANT);

            Page<CoinTransaction> page = new PageImpl<>(
                    List.of(tx), PageRequest.of(1, 5), 12
            );

            TransactionPageResponse response = BillingMapper.toPageResponse(page);

            assertThat(response.page()).isEqualTo(1);
            assertThat(response.size()).isEqualTo(5);
            assertThat(response.totalElements()).isEqualTo(12);
            assertThat(response.totalPages()).isEqualTo(3);
        }

        @Test
        @DisplayName("should correctly map each transaction in the page")
        void shouldCorrectlyMapEachTransaction() {
            CoinTransaction earnTx = createTestTransaction(TransactionType.EARN, 100, RefType.ADMIN_GRANT);
            earnTx.setId(1L);
            earnTx.setDescription("Grant");
            CoinTransaction spendTx = createTestTransaction(TransactionType.SPEND, -50, RefType.EXPORT);
            spendTx.setId(2L);
            spendTx.setDescription("Export");

            Page<CoinTransaction> page = new PageImpl<>(
                    List.of(earnTx, spendTx), PageRequest.of(0, 10), 2
            );

            TransactionPageResponse response = BillingMapper.toPageResponse(page);

            assertThat(response.content().get(0).type()).isEqualTo("EARN");
            assertThat(response.content().get(0).description()).isEqualTo("Grant");
            assertThat(response.content().get(1).type()).isEqualTo("SPEND");
            assertThat(response.content().get(1).description()).isEqualTo("Export");
        }
    }

    private CoinWallet createTestWallet() {
        CoinWallet wallet = new CoinWallet();
        wallet.setId(1L);
        wallet.setTenantId(10L);
        wallet.setBalance(500L);
        wallet.setCreatedAt(Instant.now());
        wallet.setUpdatedAt(Instant.now());
        wallet.setVersion(0L);
        return wallet;
    }

    private CoinTransaction createTestTransaction(TransactionType type, int amount, RefType refType) {
        CoinTransaction tx = new CoinTransaction();
        tx.setId(1L);
        tx.setTenantId(10L);
        tx.setWalletId(1L);
        tx.setType(type);
        tx.setAmount(amount);
        tx.setDescription("Test transaction");
        tx.setRefType(refType);
        tx.setRefId("ref-001");
        tx.setCreatedAt(Instant.now());
        return tx;
    }
}
