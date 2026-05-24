# Database

PostgreSQL. Managed by Spring Data JPA / Hibernate.

---

## Schema

### `users`

| Column | Type | Constraints |
|---|---|---|
| `id` | `BIGSERIAL` | PK |
| `username` | `VARCHAR(50)` | UNIQUE, NOT NULL |
| `email` | `VARCHAR(255)` | UNIQUE, NOT NULL |
| `password_hash` | `VARCHAR(255)` | NOT NULL |
| `role` | `VARCHAR(20)` | NOT NULL, default `USER` |
| `balance` | `DECIMAL(19,4)` | NOT NULL, default `1000000.0000` |
| `created_at` | `TIMESTAMPTZ` | NOT NULL, default `now()` |

`role` values: `USER`, `ADMIN`.

### `holdings`

| Column | Type | Constraints |
|---|---|---|
| `id` | `BIGSERIAL` | PK |
| `user_id` | `BIGINT` | FK → `users.id`, NOT NULL |
| `symbol` | `VARCHAR(10)` | NOT NULL |
| `quantity` | `INT` | NOT NULL, CHECK > 0 |
| `average_price` | `DECIMAL(19,4)` | NOT NULL |

Unique constraint on `(user_id, symbol)` — one row per user per symbol. Row is deleted when quantity reaches 0 after a sell.

### `transactions`

| Column | Type | Constraints |
|---|---|---|
| `id` | `BIGSERIAL` | PK |
| `user_id` | `BIGINT` | FK → `users.id`, NOT NULL |
| `symbol` | `VARCHAR(10)` | NOT NULL |
| `type` | `VARCHAR(4)` | NOT NULL, `BUY` or `SELL` |
| `quantity` | `INT` | NOT NULL |
| `price` | `DECIMAL(19,4)` | NOT NULL (price at execution time) |
| `timestamp` | `TIMESTAMPTZ` | NOT NULL, default `now()` |

Append-only — rows are never updated or deleted. This is the audit ledger.

### `watchlists`

| Column | Type | Constraints |
|---|---|---|
| `id` | `BIGSERIAL` | PK |
| `user_id` | `BIGINT` | FK → `users.id`, NOT NULL |
| `symbol` | `VARCHAR(10)` | NOT NULL |

Unique constraint on `(user_id, symbol)`.

### `stock_prices`

| Column | Type | Constraints |
|---|---|---|
| `symbol` | `VARCHAR(10)` | PK |
| `current_price` | `DECIMAL(19,4)` | NOT NULL |
| `updated_at` | `TIMESTAMPTZ` | NOT NULL |

Optional persistence layer for prices — the primary price store is Redis. This table can be used to persist the last-known price across Redis restarts.

### `refresh_tokens`

| Column | Type | Constraints |
|---|---|---|
| `id` | `BIGSERIAL` | PK |
| `user_id` | `BIGINT` | FK → `users.id`, NOT NULL |
| `token_hash` | `VARCHAR(255)` | UNIQUE, NOT NULL |
| `expires_at` | `TIMESTAMPTZ` | NOT NULL |
| `revoked` | `BOOLEAN` | NOT NULL, default `false` |

The raw refresh token is never stored — only its SHA-256 hash. A token is valid when `revoked = false` AND `expires_at > now()`. On each `/api/auth/refresh` call, the old row is set to `revoked = true` and a new row is inserted.

---

## Entity Relationships

```
users ──< holdings       (one user has many holdings)
users ──< transactions   (one user has many transactions)
users ──< watchlists     (one user has many watchlist entries)
users ──< refresh_tokens (one user has many refresh tokens, only one active at a time)
```

No JPA `@OneToMany` / `@ManyToOne` join between `transactions` and `holdings` — they are independent tables. The relationship is by `user_id` + `symbol` at query time, not by a foreign key.

---

## Indexes

| Table | Index | Reason |
|---|---|---|
| `users` | `UNIQUE (email)` | login lookup |
| `users` | `UNIQUE (username)` | duplicate prevention |
| `holdings` | `UNIQUE (user_id, symbol)` | upsert target |
| `transactions` | `(user_id, timestamp DESC)` | paginated history fetch |
| `watchlists` | `UNIQUE (user_id, symbol)` | add/remove idempotency |
| `refresh_tokens` | `UNIQUE (token_hash)` | token lookup on refresh |
| `refresh_tokens` | `(user_id)` | revoke all tokens for a user |

---

## Key Query Patterns

**Load portfolio** — join `holdings` with current price (from Redis/Finnhub in-app), no DB price join needed.

**Transaction history** — `SELECT * FROM transactions WHERE user_id = ? ORDER BY timestamp DESC` with pagination.

**Watchlist-driven scheduler** — `SELECT DISTINCT symbol FROM watchlists` to get the symbol set for batch price refresh.

**Leaderboard** — ROI ranking served from Redis sorted set; DB is not queried per leaderboard request.

**Token rotation** — `UPDATE refresh_tokens SET revoked = true WHERE token_hash = ?` then `INSERT` new row in one transaction.

---

## Decimal Precision

All monetary values use `DECIMAL(19,4)`:
- 19 digits total, 4 decimal places
- Avoids floating-point rounding errors for prices and balances
- Use `BigDecimal` in Java — never `double` or `float` for money
