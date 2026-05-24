# ADR 004 — $1,000,000 Starting Balance

**Date:** 2026-05-24  
**Status:** Active

## Decision

Every new user starts with $1,000,000 virtual cash.

## Why

$1M is large enough that users can trade meaningful positions in high-priced stocks (e.g. NVDA, TSLA) without immediately hitting $0, and small enough that the leaderboard ROI metric is still meaningful — a 10% gain is notable.

Lower amounts (e.g. $10k) would force users into fractional-share logic, which adds complexity this simulator doesn't need. Higher amounts (e.g. $10M) make percentage gains feel trivial.

## Consequences

- Stored in `users.balance` as a `DECIMAL` column.
- All trade validation compares `balance` against `quantity × price` at execution time.
- Balance is updated atomically inside the `@Transactional` trade service method.
- If you change this default later, existing users are unaffected (balance is set at registration time, not derived).
