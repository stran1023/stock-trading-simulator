package com.tradingapp.market.client;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

record FinnhubQuoteResponse(
        @JsonProperty("c") BigDecimal currentPrice,
        @JsonProperty("h") BigDecimal high,
        @JsonProperty("l") BigDecimal low,
        @JsonProperty("o") BigDecimal open,
        @JsonProperty("pc") BigDecimal previousClose
) {}
