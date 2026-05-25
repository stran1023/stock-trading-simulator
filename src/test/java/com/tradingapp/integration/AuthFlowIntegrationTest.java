package com.tradingapp.integration;

import com.tradingapp.market.client.FinnhubClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.TestPropertySource;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Full auth-flow integration test against the local PostgreSQL + Redis stack.
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
class AuthFlowIntegrationTest {

    @MockBean
    FinnhubClient finnhubClient;

    @Autowired
    TestRestTemplate restTemplate;

    @Test
    void authFlow_registerLoginRefreshLogout() {
        String id = uuid();
        String email = "auth-" + id + "@example.com";
        String username = "auth" + id;

        // ── 1. Register ──────────────────────────────────────────────
        ResponseEntity<Map> registerRes = post("/api/auth/register", null,
                Map.of("username", username, "email", email, "password", "secret123"));

        assertThat(registerRes.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(body(registerRes).get("success")).isEqualTo(true);
        Map<String, Object> regData = data(registerRes);
        assertThat(regData.get("userId")).isNotNull();
        assertThat(regData.get("username")).isEqualTo(username);

        // ── 2. Login ─────────────────────────────────────────────────
        ResponseEntity<Map> loginRes = post("/api/auth/login", null,
                Map.of("email", email, "password", "secret123"));

        assertThat(loginRes.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> loginData = data(loginRes);
        String accessToken = (String) loginData.get("accessToken");
        String refreshToken = (String) loginData.get("refreshToken");
        assertThat(accessToken).isNotBlank();
        assertThat(refreshToken).isNotBlank();

        // ── 3. Protected route without token → 401 ───────────────────
        ResponseEntity<Map> noAuth = restTemplate.exchange(
                "/api/portfolio", HttpMethod.GET,
                new HttpEntity<>(jsonHeaders(null)), Map.class);
        assertThat(noAuth.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        // ── 4. Refresh rotates token pair ─────────────────────────────
        ResponseEntity<Map> refreshRes = post("/api/auth/refresh", null,
                Map.of("refreshToken", refreshToken));

        assertThat(refreshRes.getStatusCode()).isEqualTo(HttpStatus.OK);
        String newAccessToken = (String) data(refreshRes).get("accessToken");
        assertThat(newAccessToken).isNotBlank();

        // ── 5. Logout ────────────────────────────────────────────────
        ResponseEntity<Void> logoutRes = restTemplate.exchange(
                "/api/auth/logout", HttpMethod.POST,
                new HttpEntity<>(jsonHeaders(newAccessToken)), Void.class);
        assertThat(logoutRes.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void register_duplicateEmail_returns400() {
        String id = uuid();
        String email = "dup-" + id + "@example.com";

        post("/api/auth/register", null,
                Map.of("username", "dup1" + id, "email", email, "password", "secret123"));

        ResponseEntity<Map> res = post("/api/auth/register", null,
                Map.of("username", "dup2" + id, "email", email, "password", "secret123"));

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(body(res).get("success")).isEqualTo(false);
    }

    @Test
    void login_wrongPassword_returns404() {
        ResponseEntity<Map> res = post("/api/auth/login", null,
                Map.of("email", "nobody-" + uuid() + "@example.com", "password", "wrong"));

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ── helpers ────────────────────────────────────────────────────

    private ResponseEntity<Map> post(String path, String token, Map<?, ?> body) {
        return restTemplate.exchange(path, HttpMethod.POST,
                new HttpEntity<>(body, jsonHeaders(token)), Map.class);
    }

    private HttpHeaders jsonHeaders(String bearerToken) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        if (bearerToken != null) h.setBearerAuth(bearerToken);
        return h;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> body(ResponseEntity<Map> res) {
        return res.getBody();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> data(ResponseEntity<Map> res) {
        return (Map<String, Object>) res.getBody().get("data");
    }

    private String uuid() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    }
}
