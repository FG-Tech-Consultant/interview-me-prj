package com.interviewme.billing.exception;

import lombok.Getter;

@Getter
public class InsufficientBalanceException extends RuntimeException {

    private final int required;
    private final long available;

    public InsufficientBalanceException(int required, long available) {
        super("Insufficient coins. Required: " + required + ", Available: " + available);
        this.required = required;
        this.available = available;
    }
}
