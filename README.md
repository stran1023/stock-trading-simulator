# Stock Trading Simulator

A production-style **paper trading platform** built with Java 21 + Spring Boot 3. Users receive $1,000,000 in virtual cash and can trade real-time stock prices without financial risk — a simplified Robinhood / TradingView experience.

---

## Features

- **Real-time prices** — Live stock quotes from Finnhub, cached in Redis and broadcast over WebSocket
- **Buy & Sell** — Full order execution with balance and holding validation
- **Portfolio tracking** — Live P&L, cost-basis, and transaction history
- **Watchlist** — Per-user symbol watchlists that drive the price refresh scheduler
- **Leaderboard** — Global ROI ranking backed by a Redis sorted set
- **JWT authentication** — Access + refresh token pair; refresh tokens rotate on every use
- **WebSocket (STOMP)** — Real-time price ticks, portfolio updates, and trade confirmations

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3 |
| Database | PostgreSQL |
| Cache | Redis |
| Security | Spring Security + JWT |
| Real-time | WebSocket (STOMP) |
| Market data | Finnhub API |
| Build | Maven |
| Containers | Docker + docker-compose |
| API Docs | SpringDoc OpenAPI / Swagger UI |
| Testing | JUnit 5 + Mockito + Testcontainers |

---

## Quick Start

### Prerequisites
- Java 21, Maven 3.9+
- Docker + docker-compose
- A free [Finnhub API key](https://finnhub.io)

### 1. Configure environment

Create a `.env` file or export variables:

```env
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/tradingapp
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=postgres
SPRING_DATA_REDIS_HOST=localhost
SPRING_DATA_REDIS_PORT=6379
JWT_SECRET=<random-256-bit-hex>
JWT_ACCESS_EXPIRATION_MS=900000
JWT_REFRESH_EXPIRATION_MS=604800000
FINNHUB_API_KEY=<your-key>
```

### 2. Start infrastructure

```powershell
docker-compose up -d db redis
```

### 3. Run the app

```powershell
mvn spring-boot:run
```

The app starts at `http://localhost:8080`.  
Swagger UI: `http://localhost:8080/swagger-ui.html`

### Full Docker stack

```powershell
docker-compose up --build
```

---

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│                     Client (HTTP / WS)                  │
└────────────────────────┬────────────────────────────────┘
                         │
┌────────────────────────▼────────────────────────────────┐
│                  Spring Boot 3 App                      │
│                                                         │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌────────┐ │
│  │   auth   │  │ trading  │  │portfolio │  │ market │ │
│  └──────────┘  └──────────┘  └──────────┘  └───┬────┘ │
│                                                  │      │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐       │      │
│  │watchlist │  │leaderboard│  │websocket │       │      │
│  └──────────┘  └──────────┘  └──────────┘       │      │
└────────────┬─────────────────────────────────────┼──────┘
             │                                     │
    ┌────────▼────────┐                   ┌────────▼───────┐
    │   PostgreSQL    │                   │     Redis      │
    │                 │                   │                │
    │ users           │                   │ stock:price:*  │
    │ holdings        │                   │ leaderboard:roi│
    │ transactions    │                   │ WS sessions    │
    │ watchlists      │                   └────────────────┘
    │ stock_prices    │
    │ refresh_tokens  │                   ┌────────────────┐
    └─────────────────┘                   │  Finnhub API   │
                                          │ (market data)  │
                                          └────────────────┘
```

**Modular monolith** — one deployable JAR, clean module boundaries enforced by package structure. Each module owns its controller, service, repository, and entities. Cross-cutting concerns (security, error handling, response wrapping) live in `common/`.

See [`docs/architecture.md`](docs/architecture.md) for module responsibilities and data flow details.

---

## REST API

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/auth/register` | Create account |
| POST | `/api/auth/login` | Get token pair |
| POST | `/api/auth/refresh` | Rotate refresh token |
| POST | `/api/auth/logout` | Revoke refresh token |
| GET | `/api/portfolio` | Holdings + live P&L |
| GET | `/api/portfolio/history` | Transaction history |
| POST | `/api/trade/buy` | Buy shares |
| POST | `/api/trade/sell` | Sell shares |
| GET | `/api/market/price/{symbol}` | Current price |
| GET | `/api/market/history/{symbol}` | OHLCV candles |
| POST | `/api/watchlist/{symbol}` | Add to watchlist |
| DELETE | `/api/watchlist/{symbol}` | Remove from watchlist |
| GET | `/api/watchlist` | List watchlist |
| GET | `/api/leaderboard` | ROI rankings |

Full request/response shapes and error codes: [`docs/api.md`](docs/api.md)

---

## Project Docs

| File | Purpose |
|---|---|
| [`docs/architecture.md`](docs/architecture.md) | Module breakdown, data flows, security |
| [`docs/conventions.md`](docs/conventions.md) | Coding rules, naming, patterns |
| [`docs/api.md`](docs/api.md) | Full API contract |
| [`docs/workflow.md`](docs/workflow.md) | Dev setup, branching, testing |
| [`docs/database.md`](docs/database.md) | Schema, relationships, indexes |
| [`docs/adr/`](docs/adr/) | Architecture decision records |

---

## Running Tests

```powershell
# All tests
mvn test

# Single class
mvn test -Dtest=TradeServiceTest
```

Unit tests mock repositories with Mockito. Integration tests use Testcontainers to spin up a real PostgreSQL instance — no local DB required for tests.
