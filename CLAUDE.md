# CLAUDE.md

Guidance for Claude Code when working in this repository.
Detailed docs live in [`docs/`](docs/) — check there before modifying architecture or conventions.

## Build & Run

```powershell
# Build (skip tests)
mvn package -DskipTests

# Run locally (requires PostgreSQL + Redis)
mvn spring-boot:run

# Run tests
mvn test

# Run a single test class
mvn test -Dtest=SomeTestClassName

# Start infra only (PostgreSQL + Redis)
docker-compose up -d db redis

# Full stack via Docker
docker-compose up --build
```

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3 |
| Database | PostgreSQL (Spring Data JPA / Hibernate) |
| Cache | Redis (Spring Data Redis) |
| Security | Spring Security + JWT (access + refresh pair) |
| Real-time | WebSocket (STOMP) |
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
├── auth/           # JWT security, login, register, token refresh
├── user/
├── portfolio/      # Holdings, P&L, transaction history
├── trading/        # Buy/sell engine, order validation
├── market/         # Finnhub client, Redis price cache, scheduler
├── websocket/      # STOMP config, price/portfolio/trade broadcasts
├── leaderboard/
└── common/
    ├── config/     # Security, Redis, CORS, OpenAPI beans
    ├── exception/  # GlobalExceptionHandler + domain exceptions
    ├── response/   # ApiResponse<T> wrapper
    └── util/
```

See [`docs/architecture.md`](docs/architecture.md) for module responsibilities and data flows.

## Coding Rules

- **Constructor injection only** — never `@Autowired` field injection
- **Service interfaces always** — impl in `service/impl/`, named `{Name}ServiceImpl`
- **DTOs at every API boundary** — Java records for read-only DTOs
- **`@Transactional` on service write methods** — never on controllers
- **Wrap all responses** in `ApiResponse<T>` from `common/response/`
- **Global exception handler** in `common/exception/GlobalExceptionHandler.java`

See [`docs/conventions.md`](docs/conventions.md) for full conventions and naming rules.

## Domain Decisions (summary)

- Starting balance: **$1,000,000** virtual cash per user
- JWT: access token 15 min + refresh token 7 days, rotated on each use
- Market prices: Finnhub → Redis cache (`stock:price:{SYMBOL}`, TTL 60s) → scheduler refreshes every 60s
- Trade atomicity: BUY and SELL each run inside one `@Transactional` service call

See [`docs/adr/`](docs/adr/) for the reasoning behind each decision.

## Environment Variables

```
SPRING_DATASOURCE_URL
SPRING_DATASOURCE_USERNAME
SPRING_DATASOURCE_PASSWORD
SPRING_DATA_REDIS_HOST
SPRING_DATA_REDIS_PORT
JWT_SECRET                  # HS256 key, min 256-bit
JWT_ACCESS_EXPIRATION_MS    # default 900000 (15 min)
JWT_REFRESH_EXPIRATION_MS   # default 604800000 (7 days)
FINNHUB_API_KEY
```

## Testing

- **Unit**: service layer, Mockito-mocked repos → `src/test/java/com/tradingapp/`
- **Integration**: `@SpringBootTest` + Testcontainers → `src/test/java/com/tradingapp/integration/`
- DB seeding via `@Sql` scripts in `src/test/resources/sql/`

See [`docs/workflow.md`](docs/workflow.md) for branching, PR process, and Finnhub rate-limit notes.

## API

Swagger UI: `http://localhost:8080/swagger-ui.html`  
Full contract: [`docs/api.md`](docs/api.md)  
DB schema: [`docs/database.md`](docs/database.md)
