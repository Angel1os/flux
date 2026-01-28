-- Creates the write-optimized time-series-ish table for market prices.
-- Flyway will run this on app startup.

CREATE TABLE IF NOT EXISTS prices (
    id UUID PRIMARY KEY,
    symbol VARCHAR(32) NOT NULL,
    price NUMERIC(19, 8) NOT NULL,
    volume BIGINT,
    change_percent NUMERIC(10, 6),
    high_24h NUMERIC(19, 8),
    low_24h NUMERIC(19, 8),
    "timestamp" TIMESTAMP NOT NULL,
    source VARCHAR(32) NOT NULL
);

-- Common query patterns from PriceRepository:
-- 1) latest price by symbol (order by timestamp desc)
CREATE INDEX IF NOT EXISTS idx_prices_symbol_timestamp_desc
    ON prices (symbol, "timestamp" DESC);

-- 2) history by symbol and timestamp range
CREATE INDEX IF NOT EXISTS idx_prices_timestamp
    ON prices ("timestamp");

