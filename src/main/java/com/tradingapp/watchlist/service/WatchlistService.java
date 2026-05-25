package com.tradingapp.watchlist.service;

import java.util.List;

public interface WatchlistService {
    void addSymbol(Long userId, String symbol);
    void removeSymbol(Long userId, String symbol);
    List<String> getSymbols(Long userId);
}
