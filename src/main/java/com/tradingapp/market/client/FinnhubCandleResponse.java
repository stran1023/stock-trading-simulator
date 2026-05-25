package com.tradingapp.market.client;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.List;

record FinnhubCandleResponse(
        @JsonProperty("c") List<BigDecimal> close,
        @JsonProperty("h") List<BigDecimal> high,
        @JsonProperty("l") List<BigDecimal> low,
        @JsonProperty("o") List<BigDecimal> open,
        @JsonProperty("t") List<Long> timestamp,
        @JsonProperty("v") List<Long> volume,
        @JsonProperty("s") String status
) {}
