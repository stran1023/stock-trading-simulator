package com.tradingapp.common.exception;

public class InsufficientHoldingException extends RuntimeException {
    public InsufficientHoldingException(String message) {
        super(message);
    }
}
