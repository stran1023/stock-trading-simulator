package com.tradingapp.integration;

import com.tradingapp.market.client.FinnhubClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Full trade-flow integration test against the local PostgreSQL + Redis stack.
 * Each test generates a unique email/username via UUID to avoid conflicts across runs.
 * Requires: docker-compose up -d db redis
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        "JWT_SECRET=d09db74bc4e7cf0523db7ef58eaa6ac9cce9fdd6c8fb1f8d4d5dab5291a73836",
        "FINNHUB_API_KEY=test-key",
        "spring.datasource.url=jdbc:postgresql://localhost:5432/tradingapp",
        "spring.datasource.username=postgres",
        "spring.datasource.password=postgres",
        "spring.data.redis.host=localhost",
        "spring.data.redis.port=6379"
})
class TradeFlowIntegrationTest {

    @MockBean
    FinnhubClient finnhubClient;

    @Autowired
    TestRestTemplate restTemplate;

    @BeforeEach
    void stubFinnhub() {
        when(finnhubClient.getQuote(anyString())).thenReturn(new BigDecimal("150.00"));
    }

    @Test
    void tradeFlow_buyPortfolioSell() {
        String token = registerAndLogin();

        // ── Buy 5 shares ─────────────────────────────────────────────
        ResponseEntity<Map> buyRes = post("/api/trade/buy", token,
                Map.of("symbol", "AAPL", "quantity", 5));

        assertThat(buyRes.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> buyData = data(buyRes);
        assertThat(buyData.get("symbol")).isEqualTo("AAPL");
        assertThat(buyData.get("quantity")).isEqualTo(5);
        // remainingBalance = 1_000_000 - 5 × actualPrice (use response price, not the mock)
        BigDecimal buyPrice = new BigDecimal(buyData.get("price").toString());
        BigDecimal expectedBalance = new BigDecimal("1000000.00").subtract(buyPrice.multiply(BigDecimal.valueOf(5)));
        assertThat(new BigDecimal(buyData.get("remainingBalance").toString()))
                .isEqualByComparingTo(expectedBalance);

        // ── Portfolio shows AAPL holding ─────────────────────────────
        ResponseEntity<Map> portfolioRes = get("/api/portfolio", token);
        assertThat(portfolioRes.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> holdings = (List<Map<String, Object>>) data(portfolioRes).get("holdings");
        assertThat(holdings).hasSize(1);
        assertThat(holdings.get(0).get("symbol")).isEqualTo("AAPL");
        assertThat(holdings.get(0).get("quantity")).isEqualTo(5);

        // ── Sell 3 shares ────────────────────────────────────────────
        ResponseEntity<Map> sellRes = post("/api/trade/sell", token,
                Map.of("symbol", "AAPL", "quantity", 3));

        assertThat(sellRes.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(data(sellRes).get("quantity")).isEqualTo(3);

        // ── Portfolio now shows 2 shares remaining ───────────────────
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> holdingsAfter = (List<Map<String, Object>>)
                data(get("/api/portfolio", token)).get("holdings");
        assertThat(holdingsAfter).hasSize(1);
        assertThat(holdingsAfter.get(0).get("quantity")).isEqualTo(2);
    }

    @Test
    void buy_insufficientBalance_returns400() {
        String token = registerAndLogin();

        ResponseEntity<Map> res = post("/api/trade/buy", token,
                Map.of("symbol", "AAPL", "quantity", 100000));

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(res.getBody().get("success")).isEqualTo(false);
    }

    @Test
    void sell_oversell_returns400() {
        String token = registerAndLogin();

        post("/api/trade/buy", token, Map.of("symbol", "AAPL", "quantity", 2));

        ResponseEntity<Map> sellRes = post("/api/trade/sell", token,
                Map.of("symbol", "AAPL", "quantity", 5));

        assertThat(sellRes.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void portfolio_noHoldings_returnsCashOnly() {
        String token = registerAndLogin();

        ResponseEntity<Map> res = get("/api/portfolio", token);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> pd = data(res);
        assertThat(new BigDecimal(pd.get("cash").toString())).isEqualByComparingTo("1000000.0000");
        @SuppressWarnings("unchecked")
        List<?> holdings = (List<?>) pd.get("holdings");
        assertThat(holdings).isEmpty();
    }

    // ── helpers ────────────────────────────────────────────────────

    private String registerAndLogin() {
        String id = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        String email = "trader-" + id + "@example.com";
        String username = "trader" + id;

        post("/api/auth/register", null,
                Map.of("username", username, "email", email, "password", "secret123"));

        ResponseEntity<Map> loginRes = post("/api/auth/login", null,
                Map.of("email", email, "password", "secret123"));

        @SuppressWarnings("unchecked")
        Map<String, Object> loginData = (Map<String, Object>) loginRes.getBody().get("data");
        return (String) loginData.get("accessToken");
    }

    private ResponseEntity<Map> post(String path, String token, Map<?, ?> body) {
        return restTemplate.exchange(path, HttpMethod.POST,
                new HttpEntity<>(body, jsonHeaders(token)), Map.class);
    }

    private ResponseEntity<Map> get(String path, String token) {
        return restTemplate.exchange(path, HttpMethod.GET,
                new HttpEntity<>(jsonHeaders(token)), Map.class);
    }

    private HttpHeaders jsonHeaders(String token) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        if (token != null) h.setBearerAuth(token);
        return h;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> data(ResponseEntity<Map> res) {
        return (Map<String, Object>) res.getBody().get("data");
    }
}
