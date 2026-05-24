package com.tradingapp.common.exception;

public class SymbolNotFoundException extends RuntimeException {
    public SymbolNotFoundException(String symbol) {
        super("Symbol not found or price unavailable: " + symbol);
    }
}
