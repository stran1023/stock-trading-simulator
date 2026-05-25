package com.tradingapp.market.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record PriceResponse(String symbol, BigDecimal price, OffsetDateTime updatedAt) {}
