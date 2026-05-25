package com.tradingapp.portfolio.controller;

import com.tradingapp.auth.entity.User;
import com.tradingapp.common.response.ApiResponse;
import com.tradingapp.portfolio.dto.PortfolioResponse;
import com.tradingapp.portfolio.dto.TransactionDto;
import com.tradingapp.portfolio.service.PortfolioService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/portfolio")
public class PortfolioController {

    private final PortfolioService portfolioService;

    public PortfolioController(PortfolioService portfolioService) {
        this.portfolioService = portfolioService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PortfolioResponse>> getPortfolio(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.ok(portfolioService.getPortfolio(user.getId())));
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<Page<TransactionDto>>> getHistory(
            @AuthenticationPrincipal User user,
            @PageableDefault(size = 20, sort = "timestamp") Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(portfolioService.getHistory(user.getId(), pageable)));
    }
}
