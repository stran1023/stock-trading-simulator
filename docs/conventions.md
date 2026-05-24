# Coding Conventions

## Package Structure

Root package: `com.tradingapp`

Each feature module follows the same internal layout:
```
{module}/
├── controller/
├── service/
│   └── impl/
├── dto/
├── entity/
└── repository/
```

Cross-cutting concerns live in `common/`:
- `common/config/` — Spring Security, Redis, CORS, OpenAPI beans
- `common/exception/` — `GlobalExceptionHandler` + domain exceptions
- `common/response/` — `ApiResponse<T>` wrapper
- `common/util/`

## Dependency Injection

**Constructor injection only.** Never use `@Autowired` on fields or setters.

```java
// Correct
@Service
public class TradeServiceImpl implements TradeService {
    private final TradeRepository tradeRepo;
    private final PriceService priceService;

    public TradeServiceImpl(TradeRepository tradeRepo, PriceService priceService) {
        this.tradeRepo = tradeRepo;
        this.priceService = priceService;
    }
}

// Wrong — never do this
@Autowired
private TradeRepository tradeRepo;
```

## Services

- Always define a service interface; put the implementation in `service/impl/`.
- Name the impl class `{Name}ServiceImpl`.
- Put `@Transactional` on service methods that write to the DB — never on controllers.
- Read-only service methods can omit `@Transactional` unless they span multiple reads that must be consistent.

## DTOs

- Use DTOs at every API boundary — never expose JPA entities directly.
- Use **Java records** for read-only (response) DTOs.
- Use regular classes for request DTOs that need validation annotations (`@NotBlank`, etc.).

```java
// Response DTO — record is fine
public record PortfolioDto(BigDecimal totalValue, BigDecimal cash, List<HoldingDto> holdings) {}

// Request DTO — class with validation
public class TradeRequest {
    @NotBlank
    private String symbol;
    @Positive
    private int quantity;
}
```

## API Responses

Wrap every response in `ApiResponse<T>` for a consistent envelope:

```json
{ "success": true, "data": { ... } }
{ "success": false, "error": "Insufficient balance" }
```

Never return a raw entity or a bare value from a controller.

## Exception Handling

- Throw domain exceptions (defined in `common/exception/`) from the service layer.
- `GlobalExceptionHandler` in `common/exception/GlobalExceptionHandler.java` catches them and maps to HTTP status codes.
- Never catch-and-swallow exceptions silently.

## Naming

| Thing | Convention | Example |
|---|---|---|
| Packages | lowercase, singular | `trading`, `portfolio` |
| Classes | PascalCase | `TradeServiceImpl` |
| Methods | camelCase, verb-first | `executeBuy`, `fetchPrice` |
| Constants | UPPER_SNAKE | `MAX_TRADE_QUANTITY` |
| DB columns | snake_case | `average_price` |
| Redis keys | `{domain}:{type}:{id}` | `stock:price:AAPL` |

## Trade Execution Rules

Both buy and sell must run inside a single `@Transactional` service method — never split across two transactions.

BUY invariants to enforce before committing:
1. Balance ≥ (quantity × price)
2. Holding upsert uses weighted-average price

SELL invariants:
1. Holding quantity ≥ requested quantity
2. Remove the holding row entirely when quantity reaches 0