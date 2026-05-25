package com.tradingapp.watchlist.service.impl;

import com.tradingapp.watchlist.entity.Watchlist;
import com.tradingapp.watchlist.repository.WatchlistRepository;
import com.tradingapp.watchlist.service.WatchlistService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class WatchlistServiceImpl implements WatchlistService {

    private final WatchlistRepository watchlistRepository;

    public WatchlistServiceImpl(WatchlistRepository watchlistRepository) {
        this.watchlistRepository = watchlistRepository;
    }

    @Override
    @Transactional
    public void addSymbol(Long userId, String symbol) {
        String upper = symbol.toUpperCase();
        if (!watchlistRepository.existsByUserIdAndSymbol(userId, upper)) {
            Watchlist entry = new Watchlist();
            entry.setUserId(userId);
            entry.setSymbol(upper);
            watchlistRepository.save(entry);
        }
    }

    @Override
    @Transactional
    public void removeSymbol(Long userId, String symbol) {
        watchlistRepository.deleteByUserIdAndSymbol(userId, symbol.toUpperCase());
    }

    @Override
    public List<String> getSymbols(Long userId) {
        return watchlistRepository.findByUserId(userId)
                .stream()
                .map(Watchlist::getSymbol)
                .toList();
    }
}
