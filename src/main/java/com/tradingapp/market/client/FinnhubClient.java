package com.tradingapp.market.client;

import com.tradingapp.common.exception.SymbolNotFoundException;
import com.tradingapp.market.dto.CandleResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
public class FinnhubClient {

    private final RestClient restClient;

    @Value("${finnhub.api-key}")
    private String apiKey;

    @Value("${finnhub.base-url}")
    private String baseUrl;

    public FinnhubClient(RestClient.Builder builder) {
        this.restClient = builder.build();
    }

    public BigDecimal getQuote(String symbol) {
        FinnhubQuoteResponse response = restClient.get()
                .uri(baseUrl + "/quote?symbol={symbol}&token={token}", symbol, apiKey)
                .retrieve()
                .body(FinnhubQuoteResponse.class);

        if (response == null || response.currentPrice() == null
                || response.currentPrice().compareTo(BigDecimal.ZERO) == 0) {
            throw new SymbolNotFoundException(symbol);
        }
        return response.currentPrice();
    }

    public List<CandleResponse> getCandles(String symbol, long from, long to) {
        FinnhubCandleResponse response = restClient.get()
                .uri(baseUrl + "/stock/candle?symbol={symbol}&resolution=D&from={from}&to={to}&token={token}",
                        symbol, from, to, apiKey)
                .retrieve()
                .body(FinnhubCandleResponse.class);

        if (response == null || !"ok".equals(response.status())) {
            throw new SymbolNotFoundException(symbol);
        }

        List<CandleResponse> candles = new ArrayList<>();
        for (int i = 0; i < response.timestamp().size(); i++) {
            candles.add(new CandleResponse(
                    response.timestamp().get(i),
                    response.open().get(i),
                    response.high().get(i),
                    response.low().get(i),
                    response.close().get(i),
                    response.volume().get(i)
            ));
        }
        return candles;
    }
}
