package com.interviewme.billing.exception;

import lombok.Getter;

@Getter
public class WalletNotFoundException extends RuntimeException {

    private final Long tenantId;

    public WalletNotFoundException(Long tenantId) {
        super("Wallet not found for tenant: " + tenantId);
        this.tenantId = tenantId;
    }
}
