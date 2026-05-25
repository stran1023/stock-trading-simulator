package com.tradingapp.market.service.impl;

import com.tradingapp.market.cache.PriceCache;
import com.tradingapp.market.client.FinnhubClient;
import com.tradingapp.market.dto.CandleResponse;
import com.tradingapp.market.dto.PriceResponse;
import com.tradingapp.market.entity.StockPrice;
import com.tradingapp.market.repository.StockPriceRepository;
import com.tradingapp.market.service.MarketService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;

@Service
public class MarketServiceImpl implements MarketService {

    private final PriceCache priceCache;
    private final FinnhubClient finnhubClient;
    private final StockPriceRepository stockPriceRepository;

    public MarketServiceImpl(PriceCache priceCache,
                             FinnhubClient finnhubClient,
                             StockPriceRepository stockPriceRepository) {
        this.priceCache = priceCache;
        this.finnhubClient = finnhubClient;
        this.stockPriceRepository = stockPriceRepository;
    }

    @Override
    @Transactional
    public PriceResponse getPrice(String symbol) {
        String upper = symbol.toUpperCase();
        OffsetDateTime now = OffsetDateTime.now();

        return priceCache.get(upper)
                .map(price -> new PriceResponse(upper, price, now))
                .orElseGet(() -> {
                    BigDecimal price = fetchAndCache(upper);
                    return new PriceResponse(upper, price, now);
                });
    }

    @Override
    @Transactional
    public BigDecimal getPriceValue(String symbol) {
        String upper = symbol.toUpperCase();
        return priceCache.get(upper).orElseGet(() -> fetchAndCache(upper));
    }

    @Override
    public List<CandleResponse> getCandles(String symbol) {
        long to = Instant.now().getEpochSecond();
        long from = to - (30L * 24 * 60 * 60);
        return finnhubClient.getCandles(symbol.toUpperCase(), from, to);
    }

    private BigDecimal fetchAndCache(String symbol) {
        BigDecimal price = finnhubClient.getQuote(symbol);
        priceCache.set(symbol, price);
        persistPrice(symbol, price);
        return price;
    }

    private void persistPrice(String symbol, BigDecimal price) {
        StockPrice entity = stockPriceRepository.findById(symbol).orElse(new StockPrice());
        entity.setSymbol(symbol);
        entity.setCurrentPrice(price);
        entity.setUpdatedAt(OffsetDateTime.now());
        stockPriceRepository.save(entity);
    }
}
