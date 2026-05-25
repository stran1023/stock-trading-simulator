package com.tradingapp.trading.dto;

import java.math.BigDecimal;

public record TradeResponse(
        String symbol,
        int quantity,
        BigDecimal price,
        BigDecimal total,
        BigDecimal remainingBalance
) {}
