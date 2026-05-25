package com.tradingapp.market.scheduler;

import com.tradingapp.market.repository.StockPriceRepository;
import com.tradingapp.market.service.MarketService;
import com.tradingapp.watchlist.repository.WatchlistRepository;
import com.tradingapp.websocket.broadcaster.PriceBroadcaster;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class PriceScheduler {

    private static final Logger log = LoggerFactory.getLogger(PriceScheduler.class);

    private final MarketService marketService;
    private final WatchlistRepository watchlistRepository;
    private final StockPriceRepository stockPriceRepository;
    private final PriceBroadcaster priceBroadcaster;

    public PriceScheduler(MarketService marketService,
                          WatchlistRepository watchlistRepository,
                          StockPriceRepository stockPriceRepository,
                          PriceBroadcaster priceBroadcaster) {
        this.marketService = marketService;
        this.watchlistRepository = watchlistRepository;
        this.stockPriceRepository = stockPriceRepository;
        this.priceBroadcaster = priceBroadcaster;
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
                BigDecimal price = marketService.getPriceValue(symbol);
                priceBroadcaster.broadcast(symbol, price);
            } catch (Exception e) {
                log.warn("Failed to refresh price for {}: {}", symbol, e.getMessage());
            }
        }
    }
}
