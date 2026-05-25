package com.tradingapp.portfolio.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record TransactionDto(
        Long id,
        String symbol,
        String type,
        int quantity,
        BigDecimal price,
        OffsetDateTime timestamp
) {}
