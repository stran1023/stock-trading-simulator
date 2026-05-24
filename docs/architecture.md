# Architecture

## Overview

**Modular monolith** — one Spring Boot JAR, feature code split into self-contained modules. Each module owns its full vertical slice: `Controller → Service (interface + impl) → Repository → Entity`. Cross-cutting concerns live in `common/`.

Choosing a monolith over microservices keeps the deployment simple (one process, one `docker-compose`), keeps transactions atomic, and avoids distributed-systems overhead at this scale. Module boundaries are still clean enough to extract a service later if needed. See [ADR 001](adr/001-modular-monolith.md).

---

## Module Responsibilities

### `auth`
Handles registration, login, logout, and token refresh. Owns the `User` entity and `RefreshToken` entity. Issues JWT access tokens and opaque refresh tokens. The JWT filter (`security/JwtAuthFilter`) validates tokens on every request and loads the principal into the security context.

### `user`
User profile management (read/update profile). Thin module — most user state lives in `auth` (credentials) and `portfolio` (balance).

### `portfolio`
Tracks what the user holds and what they've traded.
- `holdings` — current open positions with quantity and average cost basis
- `transactions` — append-only ledger of every BUY and SELL
- Computes live P&L by fetching current prices from `market` and comparing against average cost

### `trading`
The core order engine. Handles BUY and SELL requests:
- Validates inputs (quantity > 0, symbol exists)
- Fetches the current price from `market`
- Enforces business rules (balance check for BUY, quantity check for SELL)
- Writes the transaction and updates the holding atomically (one `@Transactional` call)
- Publishes a WebSocket event on completion

### `market`
Everything to do with stock prices:
- **`FinnhubClient`** — HTTP client for Finnhub REST API (quote + candles)
- **`PriceCache`** — reads/writes `stock:price:{SYMBOL}` in Redis (TTL 60s)
- **`PriceScheduler`** — `@Scheduled` task that runs every 60s, fetches fresh prices for all watchlisted symbols, updates Redis, and broadcasts to `/topic/prices`
- **`MarketController`** — exposes `GET /api/market/price/{symbol}` and `GET /api/market/history/{symbol}`

Price lookup priority: Redis hit → Finnhub API call → write result to Redis.

### `websocket`
STOMP configuration and message broadcasting.
- Configures the WebSocket message broker and STOMP endpoints
- `PriceBroadcaster` — sends price ticks to `/topic/prices`
- `PortfolioBroadcaster` — sends P&L snapshots to `/topic/portfolio/{userId}`
- `TradeBroadcaster` — sends trade confirmations to `/topic/trades/{userId}`

Broadcasting is triggered by `market` (price updates) and `trading` (completed trades).

### `leaderboard`
Computes and returns the global ROI ranking.
- Stores `{userId → roi}` in a Redis sorted set (`leaderboard:roi`) for O(log n) rank queries
- Updated on every trade completion
- `GET /api/leaderboard` returns the top-N users

### `common`
No business logic — only infrastructure:
- `config/` — `SecurityConfig`, `RedisConfig`, `CorsConfig`, `OpenApiConfig`
- `exception/` — `GlobalExceptionHandler` (`@RestControllerAdvice`) maps domain exceptions to HTTP status codes
- `response/` — `ApiResponse<T>` response envelope
- `util/` — shared utilities

---

## Trade Execution Flow

### BUY

```
POST /api/trade/buy  { symbol, quantity }
        │
        ▼
[TradeController] — extract userId from JWT principal
        │
        ▼
[TradeService] @Transactional
  1. Fetch current price from PriceCache (Redis) or Finnhub
  2. Compute cost = quantity × price
  3. Load user balance; assert balance ≥ cost (throw InsufficientBalanceException if not)
  4. Deduct balance: user.balance -= cost
  5. Upsert holding:
       if holding exists → newAvgPrice = (oldQty × oldAvg + qty × price) / (oldQty + qty)
       if no holding    → insert with averagePrice = price
  6. Append Transaction(type=BUY, ...)
  7. Save all changes (single DB flush)
        │
        ▼
[TradeBroadcaster] — push confirmation to /topic/trades/{userId}
[PortfolioBroadcaster] — push updated P&L to /topic/portfolio/{userId}
[LeaderboardService] — update ROI in Redis sorted set
```

### SELL

```
POST /api/trade/sell  { symbol, quantity }
        │
        ▼
[TradeController] — extract userId from JWT principal
        │
        ▼
[TradeService] @Transactional
  1. Load holding; assert holding.quantity ≥ quantity (throw InsufficientHoldingException if not)
  2. Fetch current price
  3. Compute proceeds = quantity × price
  4. Reduce holding: holding.quantity -= quantity
     If holding.quantity == 0 → delete the holding row
  5. Credit balance: user.balance += proceeds
  6. Append Transaction(type=SELL, ...)
  7. Save all changes
        │
        ▼
[TradeBroadcaster] / [PortfolioBroadcaster] / [LeaderboardService] (same as BUY)
```

---

## Security & JWT Flow

```
Client                         Server
  │                              │
  │── POST /api/auth/login ─────▶│
  │                     [AuthService]
  │                     - verify credentials
  │                     - generate accessToken (JWT, 15 min, signed HS256)
  │                     - generate refreshToken (opaque, store hash in DB)
  │◀── { accessToken, refreshToken } ──│
  │                              │
  │── GET /api/portfolio ───────▶│
  │   Authorization: Bearer <jwt>│
  │                     [JwtAuthFilter]
  │                     - parse + validate JWT signature & expiry
  │                     - load UserDetails by subject (userId)
  │                     - set SecurityContext
  │                     [PortfolioController]
  │◀── 200 portfolio data ───────│
  │                              │
  │── POST /api/auth/refresh ───▶│
  │   { refreshToken }          │
  │                     [AuthService]
  │                     - look up token hash in DB
  │                     - assert not revoked, not expired
  │                     - revoke old token, issue new pair
  │◀── { accessToken, refreshToken } ──│
```

`JWT_SECRET` must be at least 256 bits. Never commit it — always inject via environment variable. See [ADR 002](adr/002-jwt-auth.md).

---

## Redis Usage

| Key | Type | TTL | Description |
|---|---|---|---|
| `stock:price:{SYMBOL}` | String (JSON) | 60s | Latest price from Finnhub |
| `leaderboard:roi` | Sorted Set | none | `{userId}` → ROI score |
| WebSocket session metadata | Hash | session lifetime | STOMP session state |

Price keys use uppercase symbols (`AAPL`, not `aapl`). The scheduler writes them; individual requests read them and fall back to Finnhub on cache miss. See [ADR 003](adr/003-market-data.md).

---

## WebSocket (STOMP) Architecture

```
Client
  │── CONNECT ws://localhost:8080/ws ──▶ SockJS / STOMP handshake
  │
  │── SUBSCRIBE /topic/prices ──────────▶ receives all price ticks
  │── SUBSCRIBE /topic/portfolio/{uid} ─▶ receives own P&L updates
  │── SUBSCRIBE /topic/trades/{uid} ────▶ receives own trade confirmations
```

`/topic/prices` is public — any connected client receives it. `/topic/portfolio/{uid}` and `/topic/trades/{uid}` should be secured so only the owner can subscribe (enforced via Spring Security's WebSocket message authorization).

Price broadcasts are triggered by `PriceScheduler` (every 60s). Portfolio and trade broadcasts are triggered immediately after a trade completes.

---

## Caching Strategy

Price freshness vs. Finnhub rate limit (60 req/min free tier) is the core tension.

**Solution**: scheduler-driven batch refresh.
1. Every 60s, `PriceScheduler` collects all distinct symbols across all watchlists.
2. For each symbol, calls Finnhub once and writes the result to Redis with TTL 60s.
3. User requests read from Redis — they never hit Finnhub directly unless the cache is cold (first request for a symbol).

This decouples user-facing latency from the external API and keeps Finnhub calls predictable regardless of how many concurrent users are online.
