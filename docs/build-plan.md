# Build Plan

Tracks implementation progress phase by phase.
Each phase is committed and pushed to GitHub on completion.

**Legend:** ⬜ Not started · 🔄 In progress · ✅ Done

---

## Phase 1 — Common Foundation
> Response envelope, global exception handling, Spring config beans.

| # | File | Status |
|---|---|---|
| 1.1 | `common/response/ApiResponse<T>` | ✅ |
| 1.2 | `common/exception/` — domain exceptions | ✅ |
| 1.3 | `common/exception/GlobalExceptionHandler` | ✅ |
| 1.4 | `common/config/RedisConfig` | ✅ |
| 1.5 | `common/config/CorsConfig` | ✅ |
| 1.6 | `common/config/OpenApiConfig` | ✅ |
| 1.7 | `common/config/SecurityConfig` | ✅ |

---

## Phase 2 — Auth Module
> Registration, login, JWT access + refresh token pair, logout.

| # | File | Status |
|---|---|---|
| 2.1 | `auth/entity/User` | ✅ |
| 2.2 | `auth/entity/RefreshToken` | ✅ |
| 2.3 | `auth/repository/UserRepository` | ✅ |
| 2.4 | `auth/repository/RefreshTokenRepository` | ✅ |
| 2.5 | `auth/security/JwtService` | ✅ |
| 2.6 | `auth/security/JwtAuthFilter` | ✅ |
| 2.7 | `auth/dto/` — `RegisterRequest`, `LoginRequest`, `RefreshRequest`, `AuthResponse` | ✅ |
| 2.8 | `auth/service/AuthService` + impl | ✅ |
| 2.9 | `auth/controller/AuthController` | ✅ |

---

## Phase 3 — Market Module
> Finnhub HTTP client, Redis price cache, 60s scheduler, price + candle endpoints.

| # | File | Status |
|---|---|---|
| 3.1 | `market/entity/StockPrice` | ⬜ |
| 3.2 | `market/repository/StockPriceRepository` | ⬜ |
| 3.3 | `market/client/FinnhubClient` | ⬜ |
| 3.4 | `market/cache/PriceCache` | ⬜ |
| 3.5 | `market/scheduler/PriceScheduler` | ⬜ |
| 3.6 | `market/dto/` — `PriceResponse`, `CandleResponse` | ⬜ |
| 3.7 | `market/service/MarketService` + impl | ⬜ |
| 3.8 | `market/controller/MarketController` | ⬜ |

---

## Phase 4 — Portfolio Module
> Holdings, transaction ledger, live P&L computation.

| # | File | Status |
|---|---|---|
| 4.1 | `portfolio/entity/Holding` | ⬜ |
| 4.2 | `portfolio/entity/Transaction` | ⬜ |
| 4.3 | `portfolio/repository/HoldingRepository` | ⬜ |
| 4.4 | `portfolio/repository/TransactionRepository` | ⬜ |
| 4.5 | `portfolio/dto/` — `PortfolioResponse`, `HoldingDto`, `TransactionDto` | ⬜ |
| 4.6 | `portfolio/service/PortfolioService` + impl | ⬜ |
| 4.7 | `portfolio/controller/PortfolioController` | ⬜ |

---

## Phase 5 — Trading Module
> Transactional BUY / SELL engine with balance and holding validation.

| # | File | Status |
|---|---|---|
| 5.1 | `trading/dto/` — `TradeRequest`, `TradeResponse` | ⬜ |
| 5.2 | `trading/service/TradeService` + impl | ⬜ |
| 5.3 | `trading/controller/TradeController` | ⬜ |

---

## Phase 6 — Watchlist Module
> Per-user symbol watchlist; symbols feed the price scheduler.

| # | File | Status |
|---|---|---|
| 6.1 | `watchlist/entity/Watchlist` | ⬜ |
| 6.2 | `watchlist/repository/WatchlistRepository` | ⬜ |
| 6.3 | `watchlist/service/WatchlistService` + impl | ⬜ |
| 6.4 | `watchlist/controller/WatchlistController` | ⬜ |

---

## Phase 7 — Leaderboard Module
> Global ROI ranking backed by Redis sorted set; updated on every trade.

| # | File | Status |
|---|---|---|
| 7.1 | `leaderboard/dto/LeaderboardEntry` | ⬜ |
| 7.2 | `leaderboard/service/LeaderboardService` + impl | ⬜ |
| 7.3 | `leaderboard/controller/LeaderboardController` | ⬜ |

---

## Phase 8 — WebSocket Module
> STOMP broker config and broadcasters for prices, portfolio, and trades.

| # | File | Status |
|---|---|---|
| 8.1 | `websocket/config/WebSocketConfig` | ⬜ |
| 8.2 | `websocket/broadcaster/PriceBroadcaster` | ⬜ |
| 8.3 | `websocket/broadcaster/PortfolioBroadcaster` | ⬜ |
| 8.4 | `websocket/broadcaster/TradeBroadcaster` | ⬜ |

---

## Phase 9 — Tests
> Unit tests (Mockito) and integration tests (Testcontainers).

| # | Coverage | Status |
|---|---|---|
| 9.1 | `AuthService` unit tests | ⬜ |
| 9.2 | `TradeService` unit tests | ⬜ |
| 9.3 | `PortfolioService` unit tests | ⬜ |
| 9.4 | `MarketService` unit tests | ⬜ |
| 9.5 | Auth flow integration test (register → login → refresh → logout) | ⬜ |
| 9.6 | Trade flow integration test (buy → portfolio → sell) | ⬜ |

---

## Commit Log

| Phase | Commit message | Date |
|---|---|---|
| — | Initial commit | 2026-05-24 |
| 1 | feat: phase 1 — common foundation | 2026-05-24 |
| 2 | feat: phase 2 — auth module | 2026-05-25 |
