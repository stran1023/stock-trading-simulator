package com.tradingapp.market.scheduler;

import com.tradingapp.market.service.MarketService;
import com.tradingapp.market.repository.StockPriceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PriceScheduler {

    private static final Logger log = LoggerFactory.getLogger(PriceScheduler.class);

    private final MarketService marketService;
    private final StockPriceRepository stockPriceRepository;

    public PriceScheduler(MarketService marketService, StockPriceRepository stockPriceRepository) {
        this.marketService = marketService;
        this.stockPriceRepository = stockPriceRepository;
    }

    // Symbols to refresh are set externally by WatchlistSchedulerBridge (wired in Phase 6).
    // Until then, falls back to all symbols already persisted in stock_prices.
    private volatile List<String> watchlistedSymbols = List.of();

    public void setWatchlistedSymbols(List<String> symbols) {
        this.watchlistedSymbols = symbols;
    }

    @Scheduled(fixedDelayString = "${market.price.refresh-interval-ms}")
    public void refreshPrices() {
        List<String> symbols = watchlistedSymbols.isEmpty()
                ? stockPriceRepository.findAllSymbols()
                : watchlistedSymbols;

        if (symbols.isEmpty()) return;

        for (String symbol : symbols) {
            try {
                marketService.getPriceValue(symbol);
            } catch (Exception e) {
                log.warn("Failed to refresh price for {}: {}", symbol, e.getMessage());
            }
        }

        // Phase 8: PriceBroadcaster will be wired here to push ticks to /topic/prices
    }
}
