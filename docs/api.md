# API Contract

Base URL: `http://localhost:8080/api`

All responses are wrapped in `ApiResponse<T>`:
```json
{ "success": true, "data": { ... } }
{ "success": false, "error": "message" }
```

Interactive docs: `http://localhost:8080/swagger-ui.html`

---

## Auth

### POST /api/auth/register
```json
// Request
{ "username": "alice", "email": "alice@example.com", "password": "secret" }

// Response 201
{ "success": true, "data": { "userId": 1, "username": "alice" } }
```

### POST /api/auth/login
```json
// Request
{ "email": "alice@example.com", "password": "secret" }

// Response 200
{
  "success": true,
  "data": {
    "accessToken": "<jwt>",
    "refreshToken": "<opaque>",
    "expiresIn": 900
  }
}
```

### POST /api/auth/refresh
```json
// Request
{ "refreshToken": "<opaque>" }

// Response 200 — old refresh token is invalidated, new pair issued
{ "success": true, "data": { "accessToken": "<jwt>", "refreshToken": "<new-opaque>", "expiresIn": 900 } }
```

### POST /api/auth/logout
- Header: `Authorization: Bearer <access-token>`
- Revokes the refresh token stored in DB.
- Response 204 no body.

---

## Portfolio

All portfolio endpoints require `Authorization: Bearer <access-token>`.

### GET /api/portfolio
```json
{
  "success": true,
  "data": {
    "cash": 850000.00,
    "totalValue": 1050000.00,
    "totalPnl": 50000.00,
    "holdings": [
      { "symbol": "AAPL", "quantity": 10, "averagePrice": 170.00, "currentPrice": 180.00, "pnl": 100.00 }
    ]
  }
}
```

### GET /api/portfolio/history
Returns paginated transaction history.
```json
{
  "success": true,
  "data": [
    { "id": 1, "symbol": "AAPL", "type": "BUY", "quantity": 10, "price": 170.00, "timestamp": "2026-05-24T10:00:00Z" }
  ]
}
```

---

## Trading

All trade endpoints require `Authorization: Bearer <access-token>`.

### POST /api/trade/buy
```json
// Request
{ "symbol": "AAPL", "quantity": 10 }

// Response 200
{ "success": true, "data": { "symbol": "AAPL", "quantity": 10, "price": 170.00, "total": 1700.00, "remainingBalance": 998300.00 } }
```

**Errors:**
- `400` — insufficient balance, quantity ≤ 0
- `404` — symbol not found / price unavailable
- `401` — missing or expired token

### POST /api/trade/sell
```json
// Request
{ "symbol": "AAPL", "quantity": 5 }

// Response 200
{ "success": true, "data": { "symbol": "AAPL", "quantity": 5, "price": 180.00, "total": 900.00, "remainingBalance": 999200.00 } }
```

**Errors:**
- `400` — insufficient holding quantity, quantity ≤ 0
- `404` — symbol not in holdings

---

## Market Data

No auth required.

### GET /api/market/price/{symbol}
```json
// Response 200
{ "success": true, "data": { "symbol": "AAPL", "price": 180.00, "updatedAt": "2026-05-24T10:01:00Z" } }
```

Price is served from Redis cache (TTL 60s); falls back to Finnhub if stale.

### GET /api/market/history/{symbol}
Returns OHLCV candle data from Finnhub.
```json
{ "success": true, "data": [ { "time": 1716537600, "open": 179.00, "high": 181.00, "low": 178.50, "close": 180.00, "volume": 1200000 } ] }
```

---

## Watchlist

Requires `Authorization: Bearer <access-token>`.

### POST /api/watchlist/{symbol}
Adds `{symbol}` to the authenticated user's watchlist.
- Response 201 no body.

### DELETE /api/watchlist/{symbol}
Removes `{symbol}` from the watchlist.
- Response 204 no body.

### GET /api/watchlist
```json
{ "success": true, "data": ["AAPL", "TSLA", "NVDA"] }
```

---

## Leaderboard

No auth required.

### GET /api/leaderboard
Returns top users ranked by ROI (backed by `leaderboard:roi` Redis sorted set).
```json
{
  "success": true,
  "data": [
    { "rank": 1, "username": "alice", "roi": 12.5, "totalValue": 1125000.00 }
  ]
}
```

---

## WebSocket (STOMP)

Connect to `ws://localhost:8080/ws` and subscribe to topics:

| Topic | Description | Auth |
|---|---|---|
| `/topic/prices` | Price tick broadcasts (all symbols, all users) | No |
| `/topic/portfolio/{userId}` | Live P&L update for a specific user | Must be that user |
| `/topic/trades/{userId}` | Trade execution confirmation | Must be that user |

---

## Common Error Codes

| HTTP | Scenario |
|---|---|
| 400 | Validation failure, business rule violation |
| 401 | Missing / expired / invalid JWT |
| 403 | Authenticated but not authorized |
| 404 | Resource not found |
| 429 | Finnhub rate limit hit (60 req/min) |
| 500 | Unexpected server error |
