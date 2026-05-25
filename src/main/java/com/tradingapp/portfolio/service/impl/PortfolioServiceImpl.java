package com.tradingapp.portfolio.service.impl;

import com.tradingapp.auth.repository.UserRepository;
import com.tradingapp.common.exception.ResourceNotFoundException;
import com.tradingapp.market.service.MarketService;
import com.tradingapp.portfolio.dto.HoldingDto;
import com.tradingapp.portfolio.dto.PortfolioResponse;
import com.tradingapp.portfolio.dto.TransactionDto;
import com.tradingapp.portfolio.entity.Holding;
import com.tradingapp.portfolio.repository.HoldingRepository;
import com.tradingapp.portfolio.repository.TransactionRepository;
import com.tradingapp.portfolio.service.PortfolioService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class PortfolioServiceImpl implements PortfolioService {

    private final HoldingRepository holdingRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final MarketService marketService;

    public PortfolioServiceImpl(HoldingRepository holdingRepository,
                                TransactionRepository transactionRepository,
                                UserRepository userRepository,
                                MarketService marketService) {
        this.holdingRepository = holdingRepository;
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
        this.marketService = marketService;
    }

    @Override
    public PortfolioResponse getPortfolio(Long userId) {
        BigDecimal cash = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"))
                .getBalance();

        List<Holding> holdings = holdingRepository.findByUserId(userId);

        List<HoldingDto> holdingDtos = holdings.stream().map(h -> {
            BigDecimal currentPrice = marketService.getPriceValue(h.getSymbol());
            BigDecimal pnl = currentPrice.subtract(h.getAveragePrice())
                    .multiply(BigDecimal.valueOf(h.getQuantity()));
            return new HoldingDto(h.getSymbol(), h.getQuantity(), h.getAveragePrice(), currentPrice, pnl);
        }).toList();

        BigDecimal holdingsValue = holdingDtos.stream()
                .map(dto -> dto.currentPrice().multiply(BigDecimal.valueOf(dto.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalPnl = holdingDtos.stream()
                .map(HoldingDto::pnl)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new PortfolioResponse(cash, cash.add(holdingsValue), totalPnl, holdingDtos);
    }

    @Override
    public Page<TransactionDto> getHistory(Long userId, Pageable pageable) {
        return transactionRepository.findByUserIdOrderByTimestampDesc(userId, pageable)
                .map(t -> new TransactionDto(t.getId(), t.getSymbol(), t.getType(),
                        t.getQuantity(), t.getPrice(), t.getTimestamp()));
    }
}
