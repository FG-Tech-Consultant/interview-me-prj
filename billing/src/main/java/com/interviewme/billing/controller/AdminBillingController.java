package com.interviewme.billing.controller;

import com.interviewme.billing.dto.AdminGrantRequest;
import com.interviewme.billing.dto.WalletResponse;
import com.interviewme.billing.model.RefType;
import com.interviewme.billing.service.CoinWalletService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/wallets")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class AdminBillingController {

    private final CoinWalletService coinWalletService;

    @PostMapping("/{tenantId}/grant")
    @Transactional
    public ResponseEntity<WalletResponse> grantCoins(
            @PathVariable Long tenantId,
            @Valid @RequestBody AdminGrantRequest request,
            Authentication authentication) {
        String adminUserId = authentication.getName();
        log.info("POST /api/admin/wallets/{}/grant - adminUserId: {}, amount: {}",
                 tenantId, adminUserId, request.amount());

        String description = request.description() != null
                ? request.description()
                : "Admin grant by user " + adminUserId;

        coinWalletService.earn(tenantId, request.amount(), RefType.ADMIN_GRANT,
                "admin-" + adminUserId, description);

        WalletResponse wallet = coinWalletService.getWallet(tenantId);
        return ResponseEntity.ok(wallet);
    }

    @GetMapping("/{tenantId}")
    @Transactional(readOnly = true)
    public ResponseEntity<WalletResponse> getWallet(@PathVariable Long tenantId) {
        log.info("GET /api/admin/wallets/{}", tenantId);

        WalletResponse wallet = coinWalletService.getWallet(tenantId);
        return ResponseEntity.ok(wallet);
    }
}
