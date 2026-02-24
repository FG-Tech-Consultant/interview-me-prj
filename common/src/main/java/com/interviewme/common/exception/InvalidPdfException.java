package com.interviewme.common.exception;

public class InvalidPdfException extends RuntimeException {
    public InvalidPdfException(String message) {
        super(message);
    }
}
