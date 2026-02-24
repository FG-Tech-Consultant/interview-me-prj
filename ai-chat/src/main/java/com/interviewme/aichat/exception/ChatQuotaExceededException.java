package com.interviewme.aichat.exception;

import lombok.Getter;

@Getter
public class ChatQuotaExceededException extends RuntimeException {

    private final Long tenantId;

    public ChatQuotaExceededException(Long tenantId) {
        super("Free chat quota exhausted. Profile owner needs more coins.");
        this.tenantId = tenantId;
    }
}
