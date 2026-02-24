package com.interviewme.aichat.exception;

import lombok.Getter;

@Getter
public class ChatRateLimitException extends RuntimeException {

    private final int retryAfterSeconds;

    public ChatRateLimitException(String message) {
        super(message);
        this.retryAfterSeconds = 60;
    }
}
