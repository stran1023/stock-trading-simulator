package com.tradingapp.market.service;

import com.tradingapp.market.cache.PriceCache;
import com.tradingapp.market.client.FinnhubClient;
import com.tradingapp.market.dto.CandleResponse;
import com.tradingapp.market.dto.PriceResponse;
import com.tradingapp.market.entity.StockPrice;
import com.tradingapp.market.repository.StockPriceRepository;
import com.tradingapp.market.service.impl.MarketServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MarketServiceTest {

    @Mock PriceCache priceCache;
    @Mock FinnhubClient finnhubClient;
    @Mock StockPriceRepository stockPriceRepository;

    @InjectMocks MarketServiceImpl marketService;

    static final BigDecimal PRICE = new BigDecimal("150.00");

    // ── getPrice ───────────────────────────────────────────────────

    @Test
    void getPrice_cacheHit_returnsCachedPriceWithoutCallingFinnhub() {
        when(priceCache.get("AAPL")).thenReturn(Optional.of(PRICE));

        PriceResponse res = marketService.getPrice("aapl");

        assertThat(res.symbol()).isEqualTo("AAPL");
        assertThat(res.price()).isEqualByComparingTo(PRICE);
        verify(finnhubClient, never()).getQuote(any());
    }

    @Test
    void getPrice_cacheMiss_fetchesFromFinnhubAndCaches() {
        when(priceCache.get("AAPL")).thenReturn(Optional.empty());
        when(finnhubClient.getQuote("AAPL")).thenReturn(PRICE);
        when(stockPriceRepository.findById("AAPL")).thenReturn(Optional.empty());
        when(stockPriceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PriceResponse res = marketService.getPrice("AAPL");

        assertThat(res.price()).isEqualByComparingTo(PRICE);
        verify(finnhubClient).getQuote("AAPL");
        verify(priceCache).set("AAPL", PRICE);
    }

    // ── getPriceValue ──────────────────────────────────────────────

    @Test
    void getPriceValue_cacheHit_returnsBigDecimalWithoutFinnhub() {
        when(priceCache.get("TSLA")).thenReturn(Optional.of(new BigDecimal("800.00")));

        BigDecimal result = marketService.getPriceValue("TSLA");

        assertThat(result).isEqualByComparingTo("800.00");
        verify(finnhubClient, never()).getQuote(any());
    }

    @Test
    void getPriceValue_cacheMiss_fetchesAndPersists() {
        when(priceCache.get("TSLA")).thenReturn(Optional.empty());
        when(finnhubClient.getQuote("TSLA")).thenReturn(new BigDecimal("800.00"));
        when(stockPriceRepository.findById("TSLA")).thenReturn(Optional.of(new StockPrice()));
        when(stockPriceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        BigDecimal result = marketService.getPriceValue("TSLA");

        assertThat(result).isEqualByComparingTo("800.00");
        verify(stockPriceRepository).save(any());
    }

    // ── getCandles ─────────────────────────────────────────────────

    @Test
    void getCandles_delegatesToFinnhubClient() {
        List<CandleResponse> candles = List.of(
                new CandleResponse(1700000000L, new BigDecimal("148"), new BigDecimal("152"),
                        new BigDecimal("147"), new BigDecimal("150"), 1000000L)
        );
        when(finnhubClient.getCandles(eq("AAPL"), anyLong(), anyLong())).thenReturn(candles);

        List<CandleResponse> result = marketService.getCandles("AAPL");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).close()).isEqualByComparingTo("150");
        verify(finnhubClient).getCandles(eq("AAPL"), anyLong(), anyLong());
    }

    @Test
    void getPrice_symbolUppercasedBeforeLookup() {
        when(priceCache.get("MSFT")).thenReturn(Optional.of(new BigDecimal("300.00")));

        PriceResponse res = marketService.getPrice("msft");

        assertThat(res.symbol()).isEqualTo("MSFT");
        verify(priceCache).get("MSFT");
    }
}
