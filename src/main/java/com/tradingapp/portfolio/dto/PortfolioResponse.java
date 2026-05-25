package com.tradingapp.portfolio.dto;

import java.math.BigDecimal;
import java.util.List;

public record PortfolioResponse(
        BigDecimal cash,
        BigDecimal totalValue,
        BigDecimal totalPnl,
        List<HoldingDto> holdings
) {}
