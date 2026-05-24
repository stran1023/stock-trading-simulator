# ADR 001 — Modular Monolith Architecture

**Date:** 2026-05-24  
**Status:** Active

## Decision

Structure the app as a modular monolith: one deployable JAR, with code split into self-contained feature modules (`auth`, `trading`, `portfolio`, `market`, `leaderboard`, `websocket`) that communicate via direct Java calls.

## Why

Microservices would add distributed-systems overhead (network calls, service discovery, distributed tracing, separate deployments) with no real payoff at this scale. A single-user paper trading simulator doesn't need horizontal scaling per domain.

The modular structure still enforces clean boundaries — each module owns its own controller, service, repository, and entities — so splitting into services later is possible without a full rewrite.

## Consequences

- Simple to run locally: one process, one `docker-compose` file.
- All DB access in one transaction manager — `@Transactional` works without XA.
- Module isolation is enforced by convention (package structure) not by the JVM, so cross-module leakage is possible and must be caught in code review.
