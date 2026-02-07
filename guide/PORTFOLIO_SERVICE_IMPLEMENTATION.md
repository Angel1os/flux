# Portfolio Service Implementation

## Overview

The Portfolio Service tracks user portfolios, positions, and performance in real-time. It consumes order events from the Trading Service and updates portfolios accordingly.

## Architecture

### Components

1. **Domain Models**
   - `Portfolio`: User portfolio with cash balance, total value, and P&L
   - `Position`: Holdings for a specific symbol in a portfolio
   - `Transaction`: Historical record of all buy/sell transactions

2. **Services**
   - `PortfolioService`: CRUD operations for portfolios
   - `PositionService`: Position management and valuation
   - `TransactionService`: Transaction history management
   - `MarketDataService`: Fetches current prices from Market Data Service

3. **Event Processing**
   - `OrderEventListener`: Consumes `OrderExecutedEvent` and `OrderCancelledEvent` from Kafka
   - Updates positions, creates transactions, and recalculates portfolio values

4. **Real-time Updates**
   - WebSocket endpoint: `/ws/portfolios`
   - Sends portfolio updates to subscribed clients

## Database Schema

### portfolios
- `id` (UUID)
- `user_id` (UUID)
- `name` (VARCHAR)
- `cash_balance` (NUMERIC)
- `total_value` (NUMERIC)
- `realized_pnl` (NUMERIC)
- `unrealized_pnl` (NUMERIC)

### positions
- `id` (UUID)
- `portfolio_id` (UUID, FK)
- `symbol` (VARCHAR)
- `quantity` (NUMERIC)
- `average_price` (NUMERIC)
- `current_price` (NUMERIC)
- `total_cost` (NUMERIC)
- `current_value` (NUMERIC)
- `unrealized_pnl` (NUMERIC)

### transactions
- `id` (UUID)
- `portfolio_id` (UUID, FK)
- `order_id` (UUID) - Link to Trading Service order
- `symbol` (VARCHAR)
- `type` (VARCHAR) - BUY, SELL, DEPOSIT, WITHDRAWAL
- `quantity` (NUMERIC)
- `price` (NUMERIC)
- `total_amount` (NUMERIC)
- `fees` (NUMERIC)
- `status` (VARCHAR)
- `timestamp` (TIMESTAMP)

## API Endpoints

### Portfolio Management
- `POST /api/v1/portfolios` - Create portfolio
- `GET /api/v1/portfolios` - Get all user portfolios
- `GET /api/v1/portfolios/{id}` - Get portfolio details
- `PUT /api/v1/portfolios/{id}` - Update portfolio
- `DELETE /api/v1/portfolios/{id}` - Delete portfolio
- `GET /api/v1/portfolios/{id}/summary` - Get portfolio summary with positions
- `POST /api/v1/portfolios/{id}/deposit` - Deposit cash
- `POST /api/v1/portfolios/{id}/withdraw` - Withdraw cash

### Position Management
- `GET /api/v1/portfolios/{portfolioId}/positions` - Get all positions
- `GET /api/v1/portfolios/{portfolioId}/positions/{symbol}` - Get position by symbol

### Transaction History
- `GET /api/v1/portfolios/{portfolioId}/transactions` - Get transaction history (paginated)
- `GET /api/v1/portfolios/{portfolioId}/transactions/range` - Get transactions by date range

## Event Flow

### Order Execution Flow

1. **Trading Service** publishes `OrderExecutedEvent` to Kafka topic `order-events` (includes `orderType`: BUY/SELL)
2. **Portfolio Service** consumes the event via `OrderEventListener`
3. **Portfolio Service**:
   - Extracts order type (BUY/SELL) directly from the event
   - Gets or creates user's default portfolio
   - Updates position (add for BUY, subtract for SELL)
   - Creates transaction record
   - Updates portfolio cash balance
   - Recalculates portfolio value and P&L
   - Sends WebSocket update to subscribed clients

### Position Update Logic

**BUY Order:**
- If position doesn't exist: Create new position
- If position exists: Update quantity and recalculate average price
  - New average price = (old total cost + new cost) / new quantity
  - Update total cost

**SELL Order:**
- Calculate realized P&L = (sell price - average price) × quantity
- Update portfolio realized P&L
- Subtract quantity from position
- If quantity reaches zero: Delete position

## Configuration

### Environment Variables

```bash
PORTFOLIO_SERVICE_PORT=8083
PORTFOLIO_DB_NAME=portfolio_db
POSTGRES_HOST=localhost
POSTGRES_PORT=5432
POSTGRES_USER=postgres
POSTGRES_PASSWORD=12345
REDIS_HOST=localhost
REDIS_PORT=6379
KAFKA_BOOTSTRAP_SERVERS=localhost:19092
KEYCLOAK_URL=http://localhost:8080
KEYCLOAK_REALM=trading-platform
```

### Service URLs

- Trading Service: `http://localhost:8081` (default)
- Market Data Service: `http://localhost:8082` (default)

## Running the Service

1. **Create Database:**
   ```sql
   CREATE DATABASE portfolio_db;
   ```

2. **Start the Service:**
   ```bash
   cd portfolio-service
   mvn spring-boot:run
   ```

   Or use the helper script:
   ```bash
   ./start-portfolio-service.sh
   ```

3. **Access Swagger UI:**
   ```
   http://localhost:8083/swagger-ui/index.html
   ```

## WebSocket Connection

### Endpoint
```
ws://localhost:8083/ws/portfolios
```

### Subscribe to Portfolio Updates
```javascript
// Subscribe to specific portfolio
stompClient.subscribe('/topic/portfolios/{portfolioId}', function(message) {
    const portfolio = JSON.parse(message.body);
    console.log('Portfolio updated:', portfolio);
});
```

## Testing

### Create Portfolio
```bash
curl -X POST "http://localhost:8083/api/v1/portfolios?name=Main%20Portfolio" \
  -H "Authorization: Bearer {JWT_TOKEN}"
```

### Get Portfolio Summary
```bash
curl -X GET "http://localhost:8083/api/v1/portfolios/{portfolioId}/summary" \
  -H "Authorization: Bearer {JWT_TOKEN}"
```

### Get Positions
```bash
curl -X GET "http://localhost:8083/api/v1/portfolios/{portfolioId}/positions" \
  -H "Authorization: Bearer {JWT_TOKEN}"
```

## Notes

1. **Default Portfolio**: When an order is executed for a user without a portfolio, a default portfolio is automatically created.

2. **Order Type**: The `OrderExecutedEvent` now includes `orderType` (BUY/SELL) directly, eliminating the need for an extra HTTP call to Trading Service.

3. **Price Updates**: Current prices are fetched from Market Data Service when recalculating portfolio values. For real-time updates, consider subscribing to price events from Market Data Service.

4. **DLQ**: Failed events are sent to `portfolio-events-dlq` topic with retry logic (max 3 attempts).

5. **Security**: All endpoints require OAuth2 authentication via Keycloak. Users can only access their own portfolios.
