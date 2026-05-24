# Dev Workflow

## Local Setup

### Prerequisites
- Java 21
- Maven 3.9+
- Docker + docker-compose

### Environment Variables

Copy and fill in before running:
```
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/tradingapp
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=postgres
SPRING_DATA_REDIS_HOST=localhost
SPRING_DATA_REDIS_PORT=6379
JWT_SECRET=<min-256-bit-random-hex>
JWT_ACCESS_EXPIRATION_MS=900000
JWT_REFRESH_EXPIRATION_MS=604800000
FINNHUB_API_KEY=<your-key>
```

Get a free Finnhub key at finnhub.io (free tier: 60 req/min).

### Running

```powershell
# Start only infra (PostgreSQL + Redis)
docker-compose up -d db redis

# Run the app
mvn spring-boot:run

# Or run the full stack (app + infra) via Docker
docker-compose up --build
```

Swagger UI: http://localhost:8080/swagger-ui.html

---

## Build

```powershell
# Build JAR (skip tests for speed)
mvn package -DskipTests

# Build + run all tests
mvn package
```

---

## Testing

### Unit Tests
- Live in `src/test/java/com/tradingapp/`
- Service layer only; repositories mocked with Mockito.
- Run: `mvn test`
- Run a single class: `mvn test -Dtest=TradeServiceTest`

### Integration Tests
- Live in `src/test/java/com/tradingapp/integration/`
- Use `@SpringBootTest` + Testcontainers (spins up a real PostgreSQL container).
- DB seeded via `@Sql` scripts in `src/test/resources/sql/`.
- Slower — keep them separate so you can run unit tests fast in dev.

### Test Naming
- Unit test class: `{Subject}Test` e.g. `TradeServiceTest`
- Integration test class: `{Subject}IntegrationTest` e.g. `TradeApiIntegrationTest`

---

## Branching

| Branch | Purpose |
|---|---|
| `master` | Stable, deployable |
| `feature/{short-name}` | New feature |
| `fix/{short-name}` | Bug fix |

Merge into `master` via PR. Squash commits on merge to keep history clean.

---

## Adding a New Feature

1. Create branch: `git checkout -b feature/my-feature`
2. Add entity → repository → service interface → service impl → controller.
3. Follow the layered convention: controller calls service, service calls repository.
4. Add unit tests for the service impl.
5. Add integration tests if the feature touches the DB or HTTP layer.
6. Update `docs/api.md` if new endpoints are added.
7. Open PR against `master`.

---

## Finnhub Rate Limit

Free tier: 60 requests/minute. Redis caching (TTL 60s) is the primary guard. If you're testing locally and hitting 429s, reduce the scheduler frequency or mock the Finnhub client in tests.
