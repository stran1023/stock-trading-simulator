# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

A production-style paper trading platform (simplified Robinhood/TradingView simulator) built with Java 21 + Spring Boot 3. Users get $1,000,000 virtual cash to trade real-time stock prices.

## Build & Run

```powershell
# Build (skip tests)
mvn package -DskipTests

# Run locally (requires PostgreSQL + Redis running)
mvn spring-boot:run

# Run tests
mvn test

# Run a single test class
mvn test -Dtest=SomeTestClassName

# Start all infrastructure (PostgreSQL + Redis)
docker-compose up -d db redis

# Start full stack (app + infra)
docker-compose up --build
```

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3 |
| Database | PostgreSQL (via Spring Data JPA / Hibernate) |
| Cache | Redis (via Spring Data Redis) |
| Security | Spring Security + JWT (access + refresh token pair) |
| Real-time | WebSocket (STOMP protocol) |
| Market data | Finnhub API (free tier, 60 req/min) |
| Build | Maven |
| Containers | Docker + docker-compose |
| Docs | SpringDoc OpenAPI / Swagger UI |
| Testing | JUnit 5 + Mockito + Testcontainers |

## Package Structure

Root package: `com.tradingapp`

```
src/main/java/com/tradingapp/
├── TradingApplication.java
├── auth/
│   ├── controller/
│   ├── service/
│   ├── dto/
│   ├── entity/          # User entity
│   ├── repository/
│   └── security/        # JWT filter, token provider, UserDetailsService
├── user/
├── portfolio/           # Holdings, P&L, transaction history
├── trading/             # Buy/sell engine, order validation
├── market/              # Finnhub integration, Redis price cache, scheduler
├── websocket/           # STOMP config, price/portfolio/trade broadcasts
├── leaderboard/
└── common/
    ├── config/          # Security config, Redis config, CORS, OpenAPI
    ├── exception/       # GlobalExceptionHandler, domain exceptions
    ├── response/        # ApiResponse<T> wrapper
    └── util/
```

## Architecture

**Modular monolith** with clean layered architecture within each module:
`Controller → Service (interface + impl) → Repository → Entity`

Key rules:
- Constructor injection only — no `@Autowired` field injection
- Service interfaces always — implementations in `service/impl/`
- DTOs for all API boundaries — use Java records where the DTO is read-only
- `@Transactional` on service methods that write to the DB, not on controllers
- Global exception handler in `common/exception/GlobalExceptionHandler.java`
- Wrap all API responses in `ApiResponse<T>` for consistent shape

## Key Domain Decisions

**Default balance**: New users start with **$1,000,000** virtual cash.

**JWT**: Access token (15 min) + refresh token (7 days) pair.
- Access token in `Authorization: Bearer <token>` header
- Refresh token stored in DB; endpoint `POST /api/auth/refresh`
- Refresh tokens are rotated on each use (old one invalidated)

**Market data (Finnhub)**:
- API key via env var `FINNHUB_API_KEY`
- Latest prices cached in Redis with a short TTL (configurable, default 60s)
- Scheduled task refreshes prices for all watchlisted symbols every 60s
- Key pattern: `stock:price:{symbol}`

**Redis usage**:
- `stock:price:{SYMBOL}` — latest price (TTL: 60s)
- `leaderboard:roi` — sorted set for ranking
- WebSocket session metadata

**WebSocket (STOMP) topics**:
- `/topic/prices` — broadcast price ticks (all users)
- `/topic/portfolio/{userId}` — live P&L updates
- `/topic/trades/{userId}` — trade execution confirmations

## Database Schema

```sql
users         (id, username, email, password_hash, role, balance, created_at)
holdings      (id, user_id, symbol, quantity, average_price)
transactions  (id, user_id, symbol, type[BUY|SELL], quantity, price, timestamp)
watchlists    (id, user_id, symbol)
stock_prices  (symbol, current_price, updated_at)
refresh_tokens(id, user_id, token_hash, expires_at, revoked)
```

## REST API

```
POST /api/auth/register
POST /api/auth/login
POST /api/auth/refresh
POST /api/auth/logout

GET  /api/portfolio
GET  /api/portfolio/history

POST /api/trade/buy
POST /api/trade/sell

GET  /api/market/price/{symbol}
GET  /api/market/history/{symbol}

POST   /api/watchlist/{symbol}
DELETE /api/watchlist/{symbol}
GET    /api/watchlist

GET  /api/leaderboard
```

## Trade Execution Flow

**BUY**: Validate JWT → fetch latest price (Redis/Finnhub) → check balance ≥ cost → deduct balance → upsert holding (update avg price) → save transaction → broadcast WebSocket update

**SELL**: Validate JWT → validate holding quantity ≥ requested → fetch price → reduce/remove holding → credit balance → save transaction → broadcast WebSocket update

Both flows must be wrapped in a single `@Transactional` service call to ensure atomicity.

## Environment Variables

```
SPRING_DATASOURCE_URL
SPRING_DATASOURCE_USERNAME
SPRING_DATASOURCE_PASSWORD
SPRING_DATA_REDIS_HOST
SPRING_DATA_REDIS_PORT
JWT_SECRET                  # HS256 signing key (min 256-bit)
JWT_ACCESS_EXPIRATION_MS    # default 900000 (15 min)
JWT_REFRESH_EXPIRATION_MS   # default 604800000 (7 days)
FINNHUB_API_KEY
```

## Testing

- Unit tests: service layer with Mockito-mocked repositories
- Integration tests: `@SpringBootTest` + Testcontainers (PostgreSQL) for repository and API tests
- Place integration tests in `src/test/java/com/tradingapp/integration/`
- Use `@Sql` scripts in `src/test/resources/sql/` for DB seeding

## OpenAPI

Swagger UI available at `http://localhost:8080/swagger-ui.html` when running locally.
All endpoints documented via SpringDoc annotations.
