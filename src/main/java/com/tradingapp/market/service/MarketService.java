package com.tradingapp.market.service;

import com.tradingapp.market.dto.CandleResponse;
import com.tradingapp.market.dto.PriceResponse;

import java.math.BigDecimal;
import java.util.List;

public interface MarketService {
    PriceResponse getPrice(String symbol);
    BigDecimal getPriceValue(String symbol);
    List<CandleResponse> getCandles(String symbol);
}
