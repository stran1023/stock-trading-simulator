# ADR 002 — JWT Access + Refresh Token Pair

**Date:** 2026-05-24  
**Status:** Active

## Decision

Use a short-lived JWT access token (15 min) paired with a longer-lived refresh token (7 days) stored in the database. Refresh tokens are rotated on every use.

## Why

A single long-lived JWT would be simpler, but it can't be revoked without a blocklist. Storing session state entirely in DB (server-side sessions) would work but doesn't match the stateless REST + WebSocket model we want.

The access + refresh pair balances security (short access token window limits damage if leaked) and usability (refresh token keeps users logged in across the week without re-entering credentials).

Rotation on use means a stolen refresh token can only be used once before the legitimate client detects a mismatch and forces re-login.

## Consequences

- Access token: `Authorization: Bearer <jwt>` header on every request.
- Refresh token: stored as a hash in the `refresh_tokens` table with `revoked` flag.
- Clients must implement token refresh logic (call `POST /api/auth/refresh` on 401).
- `JWT_SECRET` must be at least 256 bits (HS256). Never commit it; use env var.
- Refresh token rotation means concurrent requests with the same refresh token will fail — clients should serialize refresh calls.
