package com.tradingapp.trading.service.impl;

import com.tradingapp.auth.entity.User;
import com.tradingapp.auth.repository.UserRepository;
import com.tradingapp.common.exception.InsufficientBalanceException;
import com.tradingapp.common.exception.InsufficientHoldingException;
import com.tradingapp.common.exception.ResourceNotFoundException;
import com.tradingapp.market.service.MarketService;
import com.tradingapp.portfolio.entity.Holding;
import com.tradingapp.portfolio.entity.Transaction;
import com.tradingapp.portfolio.repository.HoldingRepository;
import com.tradingapp.portfolio.repository.TransactionRepository;
import com.tradingapp.trading.dto.TradeRequest;
import com.tradingapp.trading.dto.TradeResponse;
import com.tradingapp.trading.service.TradeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

@Service
public class TradeServiceImpl implements TradeService {

    private final UserRepository userRepository;
    private final HoldingRepository holdingRepository;
    private final TransactionRepository transactionRepository;
    private final MarketService marketService;

    public TradeServiceImpl(UserRepository userRepository,
                            HoldingRepository holdingRepository,
                            TransactionRepository transactionRepository,
                            MarketService marketService) {
        this.userRepository = userRepository;
        this.holdingRepository = holdingRepository;
        this.transactionRepository = transactionRepository;
        this.marketService = marketService;
    }

    @Override
    @Transactional
    public TradeResponse buy(Long userId, TradeRequest request) {
        String symbol = request.getSymbol().toUpperCase();
        int quantity = request.getQuantity();

        BigDecimal price = marketService.getPriceValue(symbol);
        BigDecimal cost = price.multiply(BigDecimal.valueOf(quantity));

        User user = loadUser(userId);
        if (user.getBalance().compareTo(cost) < 0) {
            throw new InsufficientBalanceException(
                    "Insufficient balance. Required: " + cost + ", available: " + user.getBalance());
        }

        user.setBalance(user.getBalance().subtract(cost));
        userRepository.save(user);

        Optional<Holding> existing = holdingRepository.findByUserIdAndSymbol(userId, symbol);
        if (existing.isPresent()) {
            Holding h = existing.get();
            BigDecimal newAvg = h.getAveragePrice()
                    .multiply(BigDecimal.valueOf(h.getQuantity()))
                    .add(price.multiply(BigDecimal.valueOf(quantity)))
                    .divide(BigDecimal.valueOf(h.getQuantity() + quantity), 4, RoundingMode.HALF_UP);
            h.setQuantity(h.getQuantity() + quantity);
            h.setAveragePrice(newAvg);
            holdingRepository.save(h);
        } else {
            Holding h = new Holding();
            h.setUserId(userId);
            h.setSymbol(symbol);
            h.setQuantity(quantity);
            h.setAveragePrice(price);
            holdingRepository.save(h);
        }

        appendTransaction(userId, symbol, "BUY", quantity, price);

        // TODO Phase 7: leaderboardService.updateRoi(userId)
        // TODO Phase 8: portfolioBroadcaster.broadcast(userId)
        // TODO Phase 8: tradeBroadcaster.broadcast(userId, response)

        return new TradeResponse(symbol, quantity, price, cost, user.getBalance());
    }

    @Override
    @Transactional
    public TradeResponse sell(Long userId, TradeRequest request) {
        String symbol = request.getSymbol().toUpperCase();
        int quantity = request.getQuantity();

        Holding holding = holdingRepository.findByUserIdAndSymbol(userId, symbol)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No holding found for symbol: " + symbol));

        if (holding.getQuantity() < quantity) {
            throw new InsufficientHoldingException(
                    "Insufficient holding. Have: " + holding.getQuantity() + ", requested: " + quantity);
        }

        BigDecimal price = marketService.getPriceValue(symbol);
        BigDecimal proceeds = price.multiply(BigDecimal.valueOf(quantity));

        if (holding.getQuantity() == quantity) {
            holdingRepository.delete(holding);
        } else {
            holding.setQuantity(holding.getQuantity() - quantity);
            holdingRepository.save(holding);
        }

        User user = loadUser(userId);
        user.setBalance(user.getBalance().add(proceeds));
        userRepository.save(user);

        appendTransaction(userId, symbol, "SELL", quantity, price);

        // TODO Phase 7: leaderboardService.updateRoi(userId)
        // TODO Phase 8: portfolioBroadcaster.broadcast(userId)
        // TODO Phase 8: tradeBroadcaster.broadcast(userId, response)

        return new TradeResponse(symbol, quantity, price, proceeds, user.getBalance());
    }

    private User loadUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private void appendTransaction(Long userId, String symbol, String type, int quantity, BigDecimal price) {
        Transaction tx = new Transaction();
        tx.setUserId(userId);
        tx.setSymbol(symbol);
        tx.setType(type);
        tx.setQuantity(quantity);
        tx.setPrice(price);
        transactionRepository.save(tx);
    }
}
