package com.tradingapp.market.dto;

import java.math.BigDecimal;

public record CandleResponse(long time, BigDecimal open, BigDecimal high, BigDecimal low, BigDecimal close, long volume) {}
