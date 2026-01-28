# GraphQL Testing Guide - Market Data Service

This guide covers all GraphQL use cases: **Queries** and **Subscriptions**.

---

## 📋 Table of Contents

1. [Prerequisites](#prerequisites)
2. [GraphQL Queries (HTTP POST)](#graphql-queries-http-post)
3. [GraphQL Subscriptions (WebSocket)](#graphql-subscriptions-websocket)
4. [Testing Tools](#testing-tools)

---

## Prerequisites

- **JWT Token**: Get from Keycloak or Swagger UI
- **Base URL**: `http://localhost:8082`
- **GraphQL Endpoint**: `POST http://localhost:8082/graphql`
- **GraphiQL UI**: `http://localhost:8082/graphiql` (if enabled)

---

## GraphQL Queries (HTTP POST)

All queries use **POST** to `/graphql` with JSON body.

### 1. Get Current Price for a Symbol

**Use Case**: Fetch the latest price for a specific symbol (e.g., BTC, AAPL).

**Request**:
```bash
curl --location 'http://localhost:8082/graphql' \
  --header 'Authorization: Bearer <JWT_TOKEN>' \
  --header 'Content-Type: application/json' \
  --data '{
    "query": "query($symbol: String!){ currentPrice(symbol: $symbol){ symbol price volume changePercent high24h low24h timestamp source } }",
    "variables": { "symbol": "BTC" }
  }'
```

**Response**:
```json
{
  "data": {
    "currentPrice": {
      "symbol": "BTC",
      "price": 47043.29,
      "volume": 15509.0,
      "changePercent": 1.8795,
      "high24h": 47250.0,
      "low24h": 42750.0,
      "timestamp": "2026-01-28T19:30:00",
      "source": "SIMULATOR"
    }
  }
}
```

**What Changed**: You asked for specific fields (`symbol price volume ...`). GraphQL only returns what you request.

---

### 2. Get All Available Symbols

**Use Case**: List all trading symbols available in the system.

**Request**:
```bash
curl --location 'http://localhost:8082/graphql' \
  --header 'Authorization: Bearer <JWT_TOKEN>' \
  --header 'Content-Type: application/json' \
  --data '{
    "query": "{ symbols }"
  }'
```

**Response**:
```json
{
  "data": {
    "symbols": ["NVDA", "TSLA", "ETH", "AAPL", "GOOGL", "MSFT", "BTC", "AMZN"]
  }
}
```

---

### 3. Get Latest Prices for All Symbols

**Use Case**: Get the most recent price for every symbol at once.

**Request**:
```bash
curl --location 'http://localhost:8082/graphql' \
  --header 'Authorization: Bearer <JWT_TOKEN>' \
  --header 'Content-Type: application/json' \
  --data '{
    "query": "{ latestPrices { symbol price timestamp } }"
  }'
```

**Response**:
```json
{
  "data": {
    "latestPrices": [
      { "symbol": "BTC", "price": 47043.29, "timestamp": "2026-01-28T19:30:00" },
      { "symbol": "ETH", "price": 2741.69, "timestamp": "2026-01-28T19:30:00" },
      { "symbol": "AAPL", "price": 173.76, "timestamp": "2026-01-28T19:30:00" }
    ]
  }
}
```

**What Changed**: You can request **only the fields you need** (`symbol price timestamp`), not the full object.

---

### 4. Get Price History (Date Range)

**Use Case**: Fetch historical prices for a symbol within a date range.

**Request**:
```bash
curl --location 'http://localhost:8082/graphql' \
  --header 'Authorization: Bearer <JWT_TOKEN>' \
  --header 'Content-Type: application/json' \
  --data '{
    "query": "query($symbol: String!, $start: LocalDateTime!, $end: LocalDateTime!){ priceHistory(symbol: $symbol, startDate: $start, endDate: $end){ symbol price timestamp } }",
    "variables": {
      "symbol": "BTC",
      "start": "2026-01-01T00:00:00",
      "end": "2026-01-31T23:59:59"
    }
  }'
```

**Response**:
```json
{
  "data": {
    "priceHistory": [
      { "symbol": "BTC", "price": 45000.0, "timestamp": "2026-01-01T10:00:00" },
      { "symbol": "BTC", "price": 46000.0, "timestamp": "2026-01-01T11:00:00" }
    ]
  }
}
```

---

### 5. Multiple Queries in One Request

**Use Case**: Fetch multiple pieces of data in a single request (e.g., current price + all symbols).

**Request**:
```bash
curl --location 'http://localhost:8082/graphql' \
  --header 'Authorization: Bearer <JWT_TOKEN>' \
  --header 'Content-Type: application/json' \
  --data '{
    "query": "query($symbol: String!){ currentPrice(symbol: $symbol){ symbol price } symbols }",
    "variables": { "symbol": "BTC" }
  }'
```

**Response**:
```json
{
  "data": {
    "currentPrice": { "symbol": "BTC", "price": 47043.29 },
    "symbols": ["NVDA", "TSLA", "ETH", "AAPL", "GOOGL", "MSFT", "BTC", "AMZN"]
  }
}
```

**What Changed**: One HTTP request returns **multiple pieces of data** (no need for multiple REST calls).

---

## GraphQL Subscriptions (WebSocket)

**Important**: Subscriptions use **WebSocket**, not HTTP POST. They stream data in real-time.

### How GraphQL Subscriptions Work

1. **Connect** to WebSocket endpoint (usually `/graphql` over `ws://`)
2. **Send subscription message** over WebSocket
3. **Receive updates** continuously as they happen
4. **Cancel** when done

### Spring GraphQL Subscription Protocol

Spring GraphQL uses **GraphQL over WebSocket** protocol. The WebSocket endpoint is typically:
- **WebSocket URL**: `ws://localhost:8082/graphql` (or `wss://` for HTTPS)

However, Spring GraphQL subscriptions may require specific protocol handling. Let's test with the HTML client below.

---

## Testing Tools

### Option 1: GraphiQL UI (Browser)

1. Open: `http://localhost:8082/graphiql`
2. Enter your JWT token in the headers section:
   ```
   {
     "Authorization": "Bearer <YOUR_JWT_TOKEN>"
   }
   ```
3. Write queries in the left panel
4. Click "Execute Query"

**Note**: GraphiQL may not support subscriptions. Use the HTML test page below for subscriptions.

---

### Option 2: HTML Test Page (Queries + Subscriptions)

See `guide/graphql-test.html` - A complete test page for both queries and subscriptions.

**Features**:
- ✅ Test all queries (currentPrice, symbols, latestPrices, priceHistory)
- ✅ Test subscriptions (priceStream)
- ✅ Real-time updates
- ✅ JWT token management

---

### Option 3: Postman

**For Queries**:
1. Create **POST** request to `http://localhost:8082/graphql`
2. Headers:
   - `Authorization: Bearer <JWT>`
   - `Content-Type: application/json`
3. Body (raw JSON):
   ```json
   {
     "query": "{ symbols }",
     "variables": {}
   }
   ```

**For Subscriptions**:
- Postman WebSocket support for GraphQL subscriptions is limited
- Use the HTML test page or a GraphQL client library

---

### Option 4: curl (Command Line)

**For Queries**:
```bash
curl --location 'http://localhost:8082/graphql' \
  --header 'Authorization: Bearer <JWT>' \
  --header 'Content-Type: application/json' \
  --data '{"query":"{ symbols }"}'
```

**For Subscriptions**:
- curl doesn't support WebSocket subscriptions
- Use the HTML test page or a GraphQL client

---

## Common GraphQL Patterns

### 1. Field Selection (Choose What You Want)

**Request only specific fields**:
```graphql
{ currentPrice(symbol: "BTC") { symbol price } }
```

**vs. Request all fields**:
```graphql
{ currentPrice(symbol: "BTC") { symbol price volume changePercent high24h low24h timestamp source } }
```

### 2. Aliases (Rename Fields)

**Request same query twice with different names**:
```graphql
{
  btcPrice: currentPrice(symbol: "BTC") { price }
  ethPrice: currentPrice(symbol: "ETH") { price }
}
```

### 3. Fragments (Reusable Field Sets)

```graphql
fragment PriceFields on Price {
  symbol
  price
  timestamp
}

query {
  currentPrice(symbol: "BTC") { ...PriceFields }
  latestPrices { ...PriceFields }
}
```

---

## Troubleshooting

### Error: "Can't serialize value (/currentPrice/timestamp)"
- **Fix**: Restart the service after adding the `LocalDateTime` scalar configuration
- The scalar needs to be registered in `GraphQLConfig.java`

### Error: "There is no scalar implementation for 'LocalDateTime'"
- **Fix**: Ensure `graphql-java-extended-scalars` dependency is added and `LocalDateTime` is registered

### Subscriptions Not Working
- **Check**: WebSocket connection is established
- **Check**: JWT token is valid and not expired
- **Check**: Subscription query matches schema (`priceStream(symbol: String!)`)

### 401 Unauthorized
- **Fix**: Include `Authorization: Bearer <JWT>` header
- **Fix**: Ensure token is not expired (get new one from Keycloak)

---

## Next Steps

1. ✅ Test all queries using Postman or curl
2. ✅ Test subscriptions using `guide/graphql-test.html`
3. ✅ Integrate GraphQL into your frontend application
4. ✅ Add more queries/mutations as needed

---

## Summary

- **Queries** = HTTP POST to `/graphql` (one-time data fetch)
- **Subscriptions** = WebSocket to `/graphql` (real-time streaming)
- **GraphQL** = Ask for exactly what you need, get it in one request
- **Schema** = Defines what's available (in `schema.graphqls`)
