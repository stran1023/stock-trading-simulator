package com.tradingapp.trading.service;

import com.tradingapp.trading.dto.TradeRequest;
import com.tradingapp.trading.dto.TradeResponse;

public interface TradeService {
    TradeResponse buy(Long userId, TradeRequest request);
    TradeResponse sell(Long userId, TradeRequest request);
}
