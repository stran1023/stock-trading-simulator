-- V1__init_schema.sql
-- Initial schema for the stock trading simulator.
-- All monetary values use DECIMAL(19,4) to avoid floating-point rounding errors.

-- ── Users ────────────────────────────────────────────────────────
CREATE TABLE users (
    id           BIGSERIAL    PRIMARY KEY,
    username     VARCHAR(50)  NOT NULL,
    email        VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role         VARCHAR(20)  NOT NULL DEFAULT 'USER',
    balance      DECIMAL(19,4) NOT NULL DEFAULT 1000000.0000,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX idx_users_email    ON users(email);
CREATE UNIQUE INDEX idx_users_username ON users(username);

-- ── Holdings ─────────────────────────────────────────────────────
-- One row per (user, symbol). Deleted when quantity reaches 0 after a sell.
CREATE TABLE holdings (
    id            BIGSERIAL    PRIMARY KEY,
    user_id       BIGINT       NOT NULL REFERENCES users(id),
    symbol        VARCHAR(10)  NOT NULL,
    quantity      INT          NOT NULL CHECK (quantity > 0),
    average_price DECIMAL(19,4) NOT NULL
);

CREATE UNIQUE INDEX idx_holdings_user_symbol ON holdings(user_id, symbol);

-- ── Transactions ─────────────────────────────────────────────────
-- Append-only ledger. Rows are never updated or deleted.
CREATE TABLE transactions (
    id        BIGSERIAL    PRIMARY KEY,
    user_id   BIGINT       NOT NULL REFERENCES users(id),
    symbol    VARCHAR(10)  NOT NULL,
    type      VARCHAR(4)   NOT NULL CHECK (type IN ('BUY', 'SELL')),
    quantity  INT          NOT NULL,
    price     DECIMAL(19,4) NOT NULL,
    timestamp TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_transactions_user_time ON transactions(user_id, timestamp DESC);

-- ── Watchlists ───────────────────────────────────────────────────
CREATE TABLE watchlists (
    id      BIGSERIAL   PRIMARY KEY,
    user_id BIGINT      NOT NULL REFERENCES users(id),
    symbol  VARCHAR(10) NOT NULL
);

CREATE UNIQUE INDEX idx_watchlists_user_symbol ON watchlists(user_id, symbol);

-- ── Stock prices ─────────────────────────────────────────────────
-- Persistence layer for last-known prices (primary cache is Redis).
-- Survives Redis restarts.
CREATE TABLE stock_prices (
    symbol        VARCHAR(10)   PRIMARY KEY,
    current_price DECIMAL(19,4) NOT NULL,
    updated_at    TIMESTAMPTZ   NOT NULL DEFAULT now()
);

-- ── Refresh tokens ───────────────────────────────────────────────
-- Raw token is never stored — only its SHA-256 hash.
-- Valid when: revoked = false AND expires_at > now().
CREATE TABLE refresh_tokens (
    id         BIGSERIAL    PRIMARY KEY,
    user_id    BIGINT       NOT NULL REFERENCES users(id),
    token_hash VARCHAR(255) NOT NULL,
    expires_at TIMESTAMPTZ  NOT NULL,
    revoked    BOOLEAN      NOT NULL DEFAULT false
);

CREATE UNIQUE INDEX idx_refresh_tokens_hash ON refresh_tokens(token_hash);
CREATE INDEX        idx_refresh_tokens_user ON refresh_tokens(user_id);
