package com.interviewme.billing.service;

import com.interviewme.billing.config.BillingProperties;
import com.interviewme.billing.dto.FeatureCostResponse;
import com.interviewme.billing.dto.TransactionPageResponse;
import com.interviewme.billing.dto.WalletResponse;
import com.interviewme.billing.event.CoinTransactionEvent;
import com.interviewme.billing.exception.DuplicateRefundException;
import com.interviewme.billing.exception.InsufficientBalanceException;
import com.interviewme.billing.mapper.BillingMapper;
import com.interviewme.billing.model.*;
import com.interviewme.billing.repository.CoinTransactionRepository;
import com.interviewme.billing.repository.CoinWalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CoinWalletService {

    private final CoinWalletRepository walletRepository;
    private final CoinTransactionRepository transactionRepository;
    private final BillingProperties billingProperties;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public WalletResponse getWallet(Long tenantId) {
        log.debug("Getting wallet for tenantId: {}", tenantId);
        CoinWallet wallet = getOrCreateWallet(tenantId);
        return BillingMapper.toWalletResponse(wallet);
    }

    @Transactional
    public CoinTransaction spend(Long tenantId, int amount, RefType refType, String refId, String description) {
        int maxRetries = billingProperties.getRetry().getMaxAttempts();

        for (int attempt = 0; attempt < maxRetries; attempt++) {
            try {
                return doSpend(tenantId, amount, refType, refId, description);
            } catch (OptimisticLockingFailureException e) {
                log.warn("Optimistic lock conflict on spend attempt {}/{} for tenantId: {}",
                         attempt + 1, maxRetries, tenantId);
                if (attempt == maxRetries - 1) {
                    throw e;
                }
                try {
                    Thread.sleep(billingProperties.getRetry().getBackoffMs());
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted during spend retry", ie);
                }
            }
        }
        throw new RuntimeException("Failed to spend coins after " + maxRetries + " attempts");
    }

    private CoinTransaction doSpend(Long tenantId, int amount, RefType refType, String refId, String description) {
        CoinWallet wallet = getOrCreateWallet(tenantId);

        if (wallet.getBalance() < amount) {
            throw new InsufficientBalanceException(amount, wallet.getBalance());
        }

        wallet.setBalance(wallet.getBalance() - amount);
        walletRepository.save(wallet);

        CoinTransaction tx = new CoinTransaction();
        tx.setTenantId(tenantId);
        tx.setWalletId(wallet.getId());
        tx.setType(TransactionType.SPEND);
        tx.setAmount(-amount);
        tx.setDescription(description);
        tx.setRefType(refType);
        tx.setRefId(refId);
        CoinTransaction saved = transactionRepository.save(tx);

        log.info("Coins spent: tenantId={}, amount={}, refType={}, refId={}, newBalance={}",
                 tenantId, amount, refType, refId, wallet.getBalance());

        publishEvent(saved, wallet);
        return saved;
    }

    @Transactional
    public CoinTransaction earn(Long tenantId, int amount, RefType refType, String refId, String description) {
        CoinWallet wallet = getOrCreateWallet(tenantId);

        wallet.setBalance(wallet.getBalance() + amount);
        walletRepository.save(wallet);

        CoinTransaction tx = new CoinTransaction();
        tx.setTenantId(tenantId);
        tx.setWalletId(wallet.getId());
        tx.setType(TransactionType.EARN);
        tx.setAmount(amount);
        tx.setDescription(description);
        tx.setRefType(refType);
        tx.setRefId(refId);
        CoinTransaction saved = transactionRepository.save(tx);

        log.info("Coins earned: tenantId={}, amount={}, refType={}, newBalance={}",
                 tenantId, amount, refType, wallet.getBalance());

        publishEvent(saved, wallet);
        return saved;
    }

    @Transactional
    public CoinTransaction refund(Long tenantId, Long originalTransactionId) {
        // Check for duplicate refund
        String refId = String.valueOf(originalTransactionId);
        if (transactionRepository.existsByRefTypeAndRefId(RefType.EXPORT_REFUND, refId)) {
            throw new DuplicateRefundException(originalTransactionId);
        }

        CoinTransaction originalTx = transactionRepository.findById(originalTransactionId)
                .orElseThrow(() -> new RuntimeException("Original transaction not found: " + originalTransactionId));

        int refundAmount = Math.abs(originalTx.getAmount());
        CoinWallet wallet = getOrCreateWallet(tenantId);

        wallet.setBalance(wallet.getBalance() + refundAmount);
        walletRepository.save(wallet);

        CoinTransaction tx = new CoinTransaction();
        tx.setTenantId(tenantId);
        tx.setWalletId(wallet.getId());
        tx.setType(TransactionType.REFUND);
        tx.setAmount(refundAmount);
        tx.setDescription("Refund for transaction #" + originalTransactionId);
        tx.setRefType(RefType.EXPORT_REFUND);
        tx.setRefId(refId);
        CoinTransaction saved = transactionRepository.save(tx);

        log.info("Coins refunded: tenantId={}, amount={}, originalTxId={}, newBalance={}",
                 tenantId, refundAmount, originalTransactionId, wallet.getBalance());

        publishEvent(saved, wallet);
        return saved;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public CoinWallet getOrCreateWallet(Long tenantId) {
        return walletRepository.findByTenantId(tenantId)
                .orElseGet(() -> {
                    log.info("Auto-creating wallet for tenantId: {}", tenantId);
                    CoinWallet newWallet = new CoinWallet();
                    newWallet.setTenantId(tenantId);
                    newWallet.setBalance(0L);
                    return walletRepository.save(newWallet);
                });
    }

    @Transactional(readOnly = true)
    public TransactionPageResponse getTransactions(Long tenantId, int page, int size,
                                                    TransactionType type, RefType refType,
                                                    Instant from, Instant to) {
        PageRequest pageRequest = PageRequest.of(page, Math.min(size, 100));

        Page<CoinTransaction> txPage = transactionRepository.findByFilters(
                tenantId, type, refType, from, to, pageRequest);

        return BillingMapper.toPageResponse(txPage);
    }

    @Transactional(readOnly = true)
    public FeatureCostResponse getFeatureCosts() {
        Map<String, Integer> freeQuotas = Map.of(
                "CHAT_MESSAGE", billingProperties.getFreeTier().getChatMessagesPerMonth(),
                "LINKEDIN_DRAFT", billingProperties.getFreeTier().getLinkedinDraftsPerMonth()
        );

        return new FeatureCostResponse(billingProperties.getCosts(), freeQuotas);
    }

    private void publishEvent(CoinTransaction tx, CoinWallet wallet) {
        eventPublisher.publishEvent(new CoinTransactionEvent(
                tx.getTenantId(),
                wallet.getId(),
                tx.getId(),
                tx.getType().name(),
                tx.getAmount(),
                tx.getRefType() != null ? tx.getRefType().name() : null,
                tx.getRefId(),
                Instant.now()
        ));
    }
}
