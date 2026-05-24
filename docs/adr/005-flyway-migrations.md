# ADR 005 — Flyway for Database Migrations

**Date:** 2026-05-24  
**Status:** Active

## Decision

Use Flyway to manage all database schema changes. Hibernate is set to `ddl-auto: validate` — it validates entities against the schema but never modifies it.

## Why

`ddl-auto: create` or `update` is convenient in early development but dangerous in any shared or production environment: it can silently drop columns, miss indexes, or diverge between environments. Using it means there is no migration history and no way to reproduce the schema from scratch reliably.

Flyway gives:
- A versioned, reproducible history of every schema change (`V1__`, `V2__`, etc.)
- An audit trail of what ran and when (the `flyway_schema_history` table)
- Safe rollout — migrations run on startup before the app accepts traffic, so the schema and code are always in sync
- Compatibility with Testcontainers — Flyway runs against the containerized DB in integration tests automatically

## Consequences

- All schema changes go in `src/main/resources/db/migration/` as `V{n}__{description}.sql`
- Migration files are immutable once committed — never edit a file that has already run
- To change the schema, add a new migration file (`V2__...sql`, `V3__...sql`, etc.)
- `spring.jpa.hibernate.ddl-auto=validate` means the app will fail to start if entities don't match the DB schema — catch mismatches early
- Integration tests use Testcontainers + Flyway automatically; no manual DB seeding needed for schema setup
