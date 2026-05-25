package com.tradingapp.market.controller;

import com.tradingapp.common.response.ApiResponse;
import com.tradingapp.market.dto.CandleResponse;
import com.tradingapp.market.dto.PriceResponse;
import com.tradingapp.market.service.MarketService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/market")
public class MarketController {

    private final MarketService marketService;

    public MarketController(MarketService marketService) {
        this.marketService = marketService;
    }

    @GetMapping("/price/{symbol}")
    public ResponseEntity<ApiResponse<PriceResponse>> getPrice(@PathVariable String symbol) {
        return ResponseEntity.ok(ApiResponse.ok(marketService.getPrice(symbol)));
    }

    @GetMapping("/history/{symbol}")
    public ResponseEntity<ApiResponse<List<CandleResponse>>> getCandles(@PathVariable String symbol) {
        return ResponseEntity.ok(ApiResponse.ok(marketService.getCandles(symbol)));
    }
}
