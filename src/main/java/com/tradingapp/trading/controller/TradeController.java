package com.tradingapp.trading.controller;

import com.tradingapp.auth.entity.User;
import com.tradingapp.common.response.ApiResponse;
import com.tradingapp.trading.dto.TradeRequest;
import com.tradingapp.trading.dto.TradeResponse;
import com.tradingapp.trading.service.TradeService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/trade")
public class TradeController {

    private final TradeService tradeService;

    public TradeController(TradeService tradeService) {
        this.tradeService = tradeService;
    }

    @PostMapping("/buy")
    public ResponseEntity<ApiResponse<TradeResponse>> buy(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody TradeRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(tradeService.buy(user.getId(), request)));
    }

    @PostMapping("/sell")
    public ResponseEntity<ApiResponse<TradeResponse>> sell(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody TradeRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(tradeService.sell(user.getId(), request)));
    }
}
