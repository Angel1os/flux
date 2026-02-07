-- Create portfolios table
CREATE TABLE IF NOT EXISTS portfolios (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    cash_balance NUMERIC(19, 8) NOT NULL DEFAULT 0,
    total_value NUMERIC(19, 8) NOT NULL DEFAULT 0,
    realized_pnl NUMERIC(19, 8) NOT NULL DEFAULT 0,
    unrealized_pnl NUMERIC(19, 8) NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT unique_user_portfolio_name UNIQUE (user_id, name)
);

CREATE INDEX IF NOT EXISTS idx_portfolios_user_id ON portfolios(user_id);

-- Create positions table
CREATE TABLE IF NOT EXISTS positions (
    id UUID PRIMARY KEY,
    portfolio_id UUID NOT NULL REFERENCES portfolios(id) ON DELETE CASCADE,
    symbol VARCHAR(32) NOT NULL,
    quantity NUMERIC(19, 8) NOT NULL,
    average_price NUMERIC(19, 8) NOT NULL,
    current_price NUMERIC(19, 8),
    total_cost NUMERIC(19, 8) NOT NULL,
    current_value NUMERIC(19, 8),
    unrealized_pnl NUMERIC(19, 8) DEFAULT 0,
    unrealized_pnl_percent NUMERIC(10, 6),
    last_updated TIMESTAMP NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT unique_portfolio_symbol UNIQUE (portfolio_id, symbol)
);

CREATE INDEX IF NOT EXISTS idx_positions_portfolio_id ON positions(portfolio_id);
CREATE INDEX IF NOT EXISTS idx_positions_symbol ON positions(symbol);

-- Create transactions table
CREATE TABLE IF NOT EXISTS transactions (
    id UUID PRIMARY KEY,
    portfolio_id UUID NOT NULL REFERENCES portfolios(id) ON DELETE CASCADE,
    order_id UUID,
    symbol VARCHAR(32),
    type VARCHAR(20) NOT NULL,
    quantity NUMERIC(19, 8),
    price NUMERIC(19, 8),
    total_amount NUMERIC(19, 8) NOT NULL,
    fees NUMERIC(19, 8) DEFAULT 0,
    status VARCHAR(20) NOT NULL,
    timestamp TIMESTAMP NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_transactions_portfolio_id ON transactions(portfolio_id);
CREATE INDEX IF NOT EXISTS idx_transactions_order_id ON transactions(order_id);
CREATE INDEX IF NOT EXISTS idx_transactions_timestamp ON transactions(timestamp DESC);
