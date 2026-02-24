package com.interviewme.billing.mapper;

import com.interviewme.billing.dto.TransactionPageResponse;
import com.interviewme.billing.dto.TransactionResponse;
import com.interviewme.billing.dto.WalletResponse;
import com.interviewme.billing.model.CoinTransaction;
import com.interviewme.billing.model.CoinWallet;
import org.springframework.data.domain.Page;

import java.util.List;

public class BillingMapper {

    private BillingMapper() {}

    public static WalletResponse toWalletResponse(CoinWallet wallet) {
        return new WalletResponse(
                wallet.getId(),
                wallet.getTenantId(),
                wallet.getBalance(),
                wallet.getCreatedAt(),
                wallet.getUpdatedAt()
        );
    }

    public static TransactionResponse toTransactionResponse(CoinTransaction tx) {
        return new TransactionResponse(
                tx.getId(),
                tx.getType().name(),
                tx.getAmount(),
                tx.getDescription(),
                tx.getRefType() != null ? tx.getRefType().name() : null,
                tx.getRefId(),
                tx.getCreatedAt()
        );
    }

    public static TransactionPageResponse toPageResponse(Page<CoinTransaction> page) {
        List<TransactionResponse> content = page.getContent().stream()
                .map(BillingMapper::toTransactionResponse)
                .toList();

        return new TransactionPageResponse(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}
