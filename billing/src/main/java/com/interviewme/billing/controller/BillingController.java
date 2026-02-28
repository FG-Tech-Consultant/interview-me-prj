package com.interviewme.billing.controller;

import com.interviewme.billing.dto.FeatureCostResponse;
import com.interviewme.billing.dto.QuotaStatusResponse;
import com.interviewme.billing.dto.TransactionPageResponse;
import com.interviewme.billing.dto.WalletResponse;
import com.interviewme.billing.model.FeatureType;
import com.interviewme.billing.model.RefType;
import com.interviewme.billing.model.TransactionType;
import com.interviewme.billing.service.CoinWalletService;
import com.interviewme.billing.service.FreeTierService;
import com.interviewme.common.util.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/billing")
@RequiredArgsConstructor
@Slf4j
public class BillingController {

    private final CoinWalletService coinWalletService;
    private final FreeTierService freeTierService;

    @GetMapping("/wallet")
    @Transactional
    public ResponseEntity<WalletResponse> getWallet() {
        Long tenantId = TenantContext.getCurrentTenantId();
        log.info("GET /api/billing/wallet - tenantId: {}", tenantId);

        WalletResponse wallet = coinWalletService.getWallet(tenantId);
        return ResponseEntity.ok(wallet);
    }

    @GetMapping("/transactions")
    @Transactional(readOnly = true)
    public ResponseEntity<TransactionPageResponse> getTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) RefType refType,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to) {
        Long tenantId = TenantContext.getCurrentTenantId();
        log.info("GET /api/billing/transactions - tenantId: {}, page: {}, size: {}", tenantId, page, size);

        TransactionPageResponse transactions = coinWalletService.getTransactions(
                tenantId, page, size, type, refType, from, to);
        return ResponseEntity.ok(transactions);
    }

    @GetMapping("/costs")
    @Transactional(readOnly = true)
    public ResponseEntity<FeatureCostResponse> getFeatureCosts() {
        log.info("GET /api/billing/costs");

        FeatureCostResponse costs = coinWalletService.getFeatureCosts();
        return ResponseEntity.ok(costs);
    }

    @GetMapping("/quota/{featureType}")
    @Transactional(readOnly = true)
    public ResponseEntity<QuotaStatusResponse> getQuotaStatus(@PathVariable FeatureType featureType) {
        Long tenantId = TenantContext.getCurrentTenantId();
        log.info("GET /api/billing/quota/{} - tenantId: {}", featureType, tenantId);

        QuotaStatusResponse quota = freeTierService.getQuotaStatus(tenantId, featureType);
        return ResponseEntity.ok(quota);
    }
}
