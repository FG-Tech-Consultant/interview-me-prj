package com.interviewme.billing.exception;

import lombok.Getter;

@Getter
public class DuplicateRefundException extends RuntimeException {

    private final Long originalTransactionId;

    public DuplicateRefundException(Long originalTransactionId) {
        super("This transaction has already been refunded: " + originalTransactionId);
        this.originalTransactionId = originalTransactionId;
    }
}
