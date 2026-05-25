package com.tradingapp.watchlist.controller;

import com.tradingapp.auth.entity.User;
import com.tradingapp.common.response.ApiResponse;
import com.tradingapp.watchlist.service.WatchlistService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/watchlist")
public class WatchlistController {

    private final WatchlistService watchlistService;

    public WatchlistController(WatchlistService watchlistService) {
        this.watchlistService = watchlistService;
    }

    @PostMapping("/{symbol}")
    public ResponseEntity<Void> addSymbol(@AuthenticationPrincipal User user,
                                          @PathVariable String symbol) {
        watchlistService.addSymbol(user.getId(), symbol);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/{symbol}")
    public ResponseEntity<Void> removeSymbol(@AuthenticationPrincipal User user,
                                             @PathVariable String symbol) {
        watchlistService.removeSymbol(user.getId(), symbol);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<String>>> getWatchlist(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.ok(watchlistService.getSymbols(user.getId())));
    }
}
