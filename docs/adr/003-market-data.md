# ADR 003 — Finnhub + Redis for Market Data

**Date:** 2026-05-24  
**Status:** Active

## Decision

Use Finnhub (free tier) as the market data source. Cache prices in Redis under `stock:price:{SYMBOL}` with a 60-second TTL. A scheduled task refreshes prices for all watchlisted symbols every 60 seconds.

## Why

Finnhub free tier provides real stock prices at 60 req/min — enough for a paper trading simulator without paying for a data feed.

Without caching, every price fetch would hit Finnhub directly, burning the rate limit fast (60 requests/min across all users and symbols). Redis acts as a buffer: the scheduled task does the bulk fetching, and individual user requests read from cache.

## Consequences

- `FINNHUB_API_KEY` must be set as an env var.
- Price staleness: up to 60 seconds between a real price move and what users see. Acceptable for paper trading.
- Redis key pattern: `stock:price:{SYMBOL}` (uppercase symbol).
- If Redis is unavailable, the app falls back to direct Finnhub calls — watch for rate limit (429) errors under load.
- Only symbols on at least one user's watchlist are actively refreshed. Prices for un-watched symbols may be stale or missing from cache.
