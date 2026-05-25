package com.tradingapp.market.scheduler;

import com.tradingapp.market.repository.StockPriceRepository;
import com.tradingapp.market.service.MarketService;
import com.tradingapp.watchlist.repository.WatchlistRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PriceScheduler {

    private static final Logger log = LoggerFactory.getLogger(PriceScheduler.class);

    private final MarketService marketService;
    private final WatchlistRepository watchlistRepository;
    private final StockPriceRepository stockPriceRepository;

    public PriceScheduler(MarketService marketService,
                          WatchlistRepository watchlistRepository,
                          StockPriceRepository stockPriceRepository) {
        this.marketService = marketService;
        this.watchlistRepository = watchlistRepository;
        this.stockPriceRepository = stockPriceRepository;
    }

    @Scheduled(fixedDelayString = "${market.price.refresh-interval-ms}")
    public void refreshPrices() {
        List<String> symbols = watchlistRepository.findAllDistinctSymbols();
        if (symbols.isEmpty()) {
            symbols = stockPriceRepository.findAllSymbols();
        }
        if (symbols.isEmpty()) return;

        for (String symbol : symbols) {
            try {
                marketService.getPriceValue(symbol);
            } catch (Exception e) {
                log.warn("Failed to refresh price for {}: {}", symbol, e.getMessage());
            }
        }

        // TODO Phase 8: priceBroadcaster.broadcast() — push ticks to /topic/prices
    }
}
