package com.tradingapp.portfolio.service;

import com.tradingapp.portfolio.dto.PortfolioResponse;
import com.tradingapp.portfolio.dto.TransactionDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PortfolioService {
    PortfolioResponse getPortfolio(Long userId);
    Page<TransactionDto> getHistory(Long userId, Pageable pageable);
}
