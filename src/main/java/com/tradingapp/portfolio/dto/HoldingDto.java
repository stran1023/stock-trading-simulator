package com.tradingapp.portfolio.dto;

import java.math.BigDecimal;

public record HoldingDto(
        String symbol,
        int quantity,
        BigDecimal averagePrice,
        BigDecimal currentPrice,
        BigDecimal pnl
) {}
