# Build Plan

Tracks implementation progress phase by phase.
Each phase is committed and pushed to GitHub on completion.

**Legend:** ⬜ Not started · 🔄 In progress · ✅ Done

---

## Phase Completion Rule

Before committing any phase, Claude **must**:
1. Mark every item in the phase ✅
2. Run `mvn compile` — zero errors
3. Run the verification steps for that phase against the live app
4. Add a row to the Commit Log
5. Commit and push to GitHub

Do **not** move to the next phase until all five steps are done.

---

## Cross-Phase Wiring

These hooks are left open by earlier phases and must be closed by later ones.

| Hook | Opened | Closed | What to do when closing |
|---|---|---|---|
| `PriceScheduler.setWatchlistedSymbols()` | Phase 3 | ~~**Phase 6**~~ ✅ | `PriceScheduler` now injects `WatchlistRepository` directly |
| Broadcast prices → `/topic/prices` | Phase 3 (TODO comment) | ~~**Phase 8**~~ ✅ | `PriceScheduler` injects `PriceBroadcaster`, calls `broadcast(symbol, price)` per tick |
| Leaderboard update after trade | Phase 5 (TODO comment) | ~~**Phase 7**~~ ✅ | `TradeServiceImpl` calls `leaderboardService.updateRoi(userId)` after every trade |
| Broadcast portfolio after trade | Phase 5 (TODO comment) | ~~**Phase 8**~~ ✅ | `TradeServiceImpl` injects `PortfolioBroadcaster`, calls `broadcast(userId)` after BUY/SELL |
| Broadcast trade confirmation | Phase 5 (TODO comment) | ~~**Phase 8**~~ ✅ | `TradeServiceImpl` injects `TradeBroadcaster`, calls `broadcast(userId, response)` after BUY/SELL |

---

## Phase 1 — Common Foundation ✅
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

**Verification**
```powershell
# App starts, health returns UP
curl http://localhost:8080/actuator/health
# Expected: {"status":"UP"}

# Swagger UI loads in browser
# http://localhost:8080/swagger-ui.html
```

---

## Phase 2 — Auth Module ✅
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

**Verification**
```powershell
# Register
curl -X POST http://localhost:8080/api/auth/register `
  -H "Content-Type: application/json" `
  -d '{"username":"alice","email":"alice@example.com","password":"secret123"}'
# Expected: 201 {"success":true,"data":{"userId":1,"username":"alice"}}

# Login → copy accessToken + refreshToken from response
curl -X POST http://localhost:8080/api/auth/login `
  -H "Content-Type: application/json" `
  -d '{"email":"alice@example.com","password":"secret123"}'
# Expected: 200 with accessToken, refreshToken, expiresIn

# Protected route without token → 401
curl http://localhost:8080/api/portfolio
# Expected: 401

# Refresh (rotates token pair)
curl -X POST http://localhost:8080/api/auth/refresh `
  -H "Content-Type: application/json" `
  -d '{"refreshToken":"<from-login>"}'
# Expected: 200 new token pair

# Logout
curl -X POST http://localhost:8080/api/auth/logout `
  -H "Authorization: Bearer <accessToken>"
# Expected: 204
```

---

## Phase 3 — Market Module ✅
> Finnhub HTTP client, Redis price cache, 60s scheduler, price + candle endpoints.

| # | File | Status |
|---|---|---|
| 3.1 | `market/entity/StockPrice` | ✅ |
| 3.2 | `market/repository/StockPriceRepository` | ✅ |
| 3.3 | `market/client/FinnhubClient` | ✅ |
| 3.4 | `market/cache/PriceCache` | ✅ |
| 3.5 | `market/scheduler/PriceScheduler` | ✅ |
| 3.6 | `market/dto/` — `PriceResponse`, `CandleResponse` | ✅ |
| 3.7 | `market/service/MarketService` + impl | ✅ |
| 3.8 | `market/controller/MarketController` | ✅ |

**Verification**
```powershell
# Price — first call hits Finnhub, caches in Redis
curl http://localhost:8080/api/market/price/AAPL
# Expected: 200 {"success":true,"data":{"symbol":"AAPL","price":...,"updatedAt":...}}

# Second call served from Redis cache (check Redis: GET stock:price:AAPL)
curl http://localhost:8080/api/market/price/AAPL

# Candle history (30 days OHLCV)
curl http://localhost:8080/api/market/history/AAPL
# Expected: 200 array of {time, open, high, low, close, volume}

# Invalid symbol → 404
curl http://localhost:8080/api/market/price/INVALIDSYMBOL999
# Expected: 404
```

---

## Phase 4 — Portfolio Module ✅
> Holdings, transaction ledger, live P&L computation.

| # | File | Status |
|---|---|---|
| 4.1 | `portfolio/entity/Holding` | ✅ |
| 4.2 | `portfolio/entity/Transaction` | ✅ |
| 4.3 | `portfolio/repository/HoldingRepository` | ✅ |
| 4.4 | `portfolio/repository/TransactionRepository` | ✅ |
| 4.5 | `portfolio/dto/` — `PortfolioResponse`, `HoldingDto`, `TransactionDto` | ✅ |
| 4.6 | `portfolio/service/PortfolioService` + impl | ✅ |
| 4.7 | `portfolio/controller/PortfolioController` | ✅ |

**Verification**
```powershell
# Login first, then:

# Empty portfolio (fresh user)
curl http://localhost:8080/api/portfolio `
  -H "Authorization: Bearer <accessToken>"
# Expected: cash=1000000, totalValue=1000000, totalPnl=0, holdings=[]

# Empty transaction history
curl http://localhost:8080/api/portfolio/history `
  -H "Authorization: Bearer <accessToken>"
# Expected: {"success":true,"data":[]}
```

---

## Phase 5 — Trading Module ✅
> Transactional BUY / SELL engine with balance and holding validation.

| # | File | Status |
|---|---|---|
| 5.1 | `trading/dto/` — `TradeRequest`, `TradeResponse` | ✅ |
| 5.2 | `trading/service/TradeService` + impl | ✅ |
| 5.3 | `trading/controller/TradeController` | ✅ |

**Open hooks to add TODO comments for:**
- `LeaderboardService.updateRoi()` call (closed in Phase 7)
- `PortfolioBroadcaster` call (closed in Phase 8)
- `TradeBroadcaster` call (closed in Phase 8)

**Verification**
```powershell
# Buy 5 shares of AAPL
curl -X POST http://localhost:8080/api/trade/buy `
  -H "Authorization: Bearer <accessToken>" `
  -H "Content-Type: application/json" `
  -d '{"symbol":"AAPL","quantity":5}'
# Expected: 200 {symbol, quantity, price, total, remainingBalance}

# Portfolio now has AAPL holding
curl http://localhost:8080/api/portfolio -H "Authorization: Bearer <accessToken>"
# Expected: holdings contains AAPL with quantity=5

# Transaction history shows the BUY
curl http://localhost:8080/api/portfolio/history -H "Authorization: Bearer <accessToken>"

# Sell 3 shares
curl -X POST http://localhost:8080/api/trade/sell `
  -H "Authorization: Bearer <accessToken>" `
  -H "Content-Type: application/json" `
  -d '{"symbol":"AAPL","quantity":3}'
# Expected: 200, holding quantity drops to 2

# Oversell → 400
curl -X POST http://localhost:8080/api/trade/sell `
  -H "Authorization: Bearer <accessToken>" `
  -H "Content-Type: application/json" `
  -d '{"symbol":"AAPL","quantity":999}'
# Expected: 400 insufficient holding

# Overspend → 400
curl -X POST http://localhost:8080/api/trade/buy `
  -H "Authorization: Bearer <accessToken>" `
  -H "Content-Type: application/json" `
  -d '{"symbol":"AAPL","quantity":100000}'
# Expected: 400 insufficient balance
```

---

## Phase 6 — Watchlist Module ✅
> Per-user symbol watchlist; symbols feed the price scheduler.

| # | File | Status |
|---|---|---|
| 6.1 | `watchlist/entity/Watchlist` | ✅ |
| 6.2 | `watchlist/repository/WatchlistRepository` | ✅ |
| 6.3 | `watchlist/service/WatchlistService` + impl | ✅ |
| 6.4 | `watchlist/controller/WatchlistController` | ✅ |

**Cross-phase wiring closed ✅:**
- `PriceScheduler` now injects `WatchlistRepository` directly and calls `findAllDistinctSymbols()` each tick.

**Verification**
```powershell
# Add symbol
curl -X POST http://localhost:8080/api/watchlist/AAPL `
  -H "Authorization: Bearer <accessToken>"
# Expected: 201

# Add another
curl -X POST http://localhost:8080/api/watchlist/TSLA `
  -H "Authorization: Bearer <accessToken>"

# Get watchlist
curl http://localhost:8080/api/watchlist `
  -H "Authorization: Bearer <accessToken>"
# Expected: {"success":true,"data":["AAPL","TSLA"]}

# Remove
curl -X DELETE http://localhost:8080/api/watchlist/TSLA `
  -H "Authorization: Bearer <accessToken>"
# Expected: 204

# After 60s: check app logs — scheduler should refresh AAPL price
```

---

## Phase 7 — Leaderboard Module ✅
> Global ROI ranking backed by Redis sorted set; updated on every trade.

| # | File | Status |
|---|---|---|
| 7.1 | `leaderboard/dto/LeaderboardEntry` | ✅ |
| 7.2 | `leaderboard/service/LeaderboardService` + impl | ✅ |
| 7.3 | `leaderboard/controller/LeaderboardController` | ✅ |

**Cross-phase wiring closed ✅:**
- `TradeServiceImpl` now calls `leaderboardService.updateRoi(userId)` after every BUY and SELL.

**Verification**
```powershell
# Leaderboard (no auth needed)
curl http://localhost:8080/api/leaderboard
# Expected: {"success":true,"data":[{"rank":1,"username":"alice","roi":...,"totalValue":...}]}

# Make a trade then re-check — ROI should update
curl -X POST http://localhost:8080/api/trade/buy `
  -H "Authorization: Bearer <accessToken>" `
  -H "Content-Type: application/json" `
  -d '{"symbol":"AAPL","quantity":1}'
curl http://localhost:8080/api/leaderboard
# Expected: alice's totalValue and roi reflect the trade
```

---

## Phase 8 — WebSocket Module ✅
> STOMP broker config and broadcasters for prices, portfolio, and trades.

| # | File | Status |
|---|---|---|
| 8.1 | `websocket/config/WebSocketConfig` | ✅ |
| 8.2 | `websocket/broadcaster/PriceBroadcaster` | ✅ |
| 8.3 | `websocket/broadcaster/PortfolioBroadcaster` | ✅ |
| 8.4 | `websocket/broadcaster/TradeBroadcaster` | ✅ |

**Cross-phase wiring closed ✅:**
- `PriceBroadcaster` injected into `PriceScheduler.refreshPrices()` — broadcasts each price tick to `/topic/prices`.
- `PortfolioBroadcaster` + `TradeBroadcaster` injected into `TradeServiceImpl` — fires after every BUY and SELL.

**Verification**
```
# Use a STOMP client (e.g. Swagger UI WebSocket tab, or wscat):
# Connect: ws://localhost:8080/ws

# Subscribe /topic/prices
# Wait up to 60s — should receive price tick JSON

# Subscribe /topic/portfolio/{userId}
# Make a trade — should receive updated P&L immediately

# Subscribe /topic/trades/{userId}
# Make a trade — should receive trade confirmation immediately
```

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

**Verification**
```powershell
# All tests pass
mvn test
# Expected: BUILD SUCCESS, 0 failures

# Integration tests only
mvn test -Dtest="*IntegrationTest"
```

---

## Commit Log

| Phase | Commit message | Date |
|---|---|---|
| — | Initial commit | 2026-05-24 |
| 1 | feat: phase 1 — common foundation | 2026-05-24 |
| 2 | feat: phase 2 — auth module | 2026-05-25 |
| 3 | feat: phase 3 — market module | 2026-05-25 |
| 4 | feat: phase 4 — portfolio module | 2026-05-25 |
| 5 | feat: phase 5 — trading module | 2026-05-25 |
| 6 | feat: phase 6 — watchlist module | 2026-05-25 |
| 7 | feat: phase 7 — leaderboard module | 2026-05-25 |
| 8 | feat: phase 8 — websocket module | 2026-05-25 |
