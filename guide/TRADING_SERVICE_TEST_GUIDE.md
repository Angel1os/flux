# Trading Service - Complete Testing Guide

## Base URL
```
http://localhost:8081/api/v1/trading/orders
```

## User ID Header
For create/update operations, include:
```
X-User-Id: 550e8400-e29b-41d4-a716-446655440000
```

---

## 1. CREATE ORDER

### Scenario 1.1: Buy Limit Order (GOOGL)
```bash
curl -X POST http://localhost:8081/api/v1/trading/orders \
  -H "Content-Type: application/json" \
  -H "X-User-Id: 550e8400-e29b-41d4-a716-446655440000" \
  -d '{
    "symbol": "GOOGL",
    "type": "BUY",
    "side": "LIMIT",
    "quantity": 10,
    "limitPrice": 145.50
  }'
```

### Scenario 1.2: Sell Market Order (AAPL)
```bash
curl -X POST http://localhost:8081/api/v1/trading/orders \
  -H "Content-Type: application/json" \
  -H "X-User-Id: 550e8400-e29b-41d4-a716-446655440000" \
  -d '{
    "symbol": "AAPL",
    "type": "SELL",
    "side": "MARKET",
    "quantity": 5
  }'
```

### Scenario 1.3: Buy Limit Order (TSLA)
```bash
curl -X POST http://localhost:8081/api/v1/trading/orders \
  -H "Content-Type: application/json" \
  -H "X-User-Id: 550e8400-e29b-41d4-a716-446655440000" \
  -d '{
    "symbol": "TSLA",
    "type": "BUY",
    "side": "LIMIT",
    "quantity": 20,
    "limitPrice": 250.00
  }'
```

### Scenario 1.4: Sell Limit Order (MSFT)
```bash
curl -X POST http://localhost:8081/api/v1/trading/orders \
  -H "Content-Type: application/json" \
  -H "X-User-Id: 550e8400-e29b-41d4-a716-446655440000" \
  -d '{
    "symbol": "MSFT",
    "type": "SELL",
    "side": "LIMIT",
    "quantity": 15,
    "limitPrice": 380.75
  }'
```

### Scenario 1.5: Buy Market Order (BTC - Crypto)
```bash
curl -X POST http://localhost:8081/api/v1/trading/orders \
  -H "Content-Type: application/json" \
  -H "X-User-Id: 550e8400-e29b-41d4-a716-446655440000" \
  -d '{
    "symbol": "BTC",
    "type": "BUY",
    "side": "MARKET",
    "quantity": 0.5
  }'
```

### Scenario 1.6: Buy Limit Order (Fractional Quantity)
```bash
curl -X POST http://localhost:8081/api/v1/trading/orders \
  -H "Content-Type: application/json" \
  -H "X-User-Id: 550e8400-e29b-41d4-a716-446655440000" \
  -d '{
    "symbol": "GOOGL",
    "type": "BUY",
    "side": "LIMIT",
    "quantity": 3.5,
    "limitPrice": 145.50
  }'
```

**Expected Response:**
- Status: `201 Created`
- Order with `PENDING` status
- Order ID (UUID) in response
- Event published to Kafka
- Read model created in Redis

**Save the Order ID from response for next steps!**

---

## 2. GET ALL ORDERS

### Get All Orders
```bash
curl -X GET "http://localhost:8081/api/v1/trading/orders"
```

### Get Orders by User ID
```bash
curl -X GET "http://localhost:8081/api/v1/trading/orders?userId=550e8400-e29b-41d4-a716-446655440000"
```

### Get Orders with Pagination
```bash
curl -X GET "http://localhost:8081/api/v1/trading/orders?page=1&size=10&paginate=true"
```

---

## 3. GET ORDER BY ID

```bash
# Replace {ORDER_ID} with actual order ID from step 1
curl -X GET "http://localhost:8081/api/v1/trading/orders/{ORDER_ID}"
```

**Example:**
```bash
curl -X GET "http://localhost:8081/api/v1/trading/orders/4f15b15b-d7a8-4044-89d4-0e9b04aa95da"
```

---

## 4. GET ORDERS BY SYMBOL

```bash
curl -X GET "http://localhost:8081/api/v1/trading/orders/symbol/GOOGL"
```

---

## 5. GET ORDERS BY STATUS

```bash
curl -X GET "http://localhost:8081/api/v1/trading/orders/status/PENDING"
```

Available statuses: `PENDING`, `EXECUTED`, `CANCELLED`, `REJECTED`, `PARTIALLY_EXECUTED`

---

## 6. UPDATE ORDER

**Note:** Can only update orders with status `PENDING`

```bash
# Replace {ORDER_ID} with actual order ID
curl -X PUT http://localhost:8081/api/v1/trading/orders/{ORDER_ID} \
  -H "Content-Type: application/json" \
  -H "X-User-Id: 550e8400-e29b-41d4-a716-446655440000" \
  -d '{
    "symbol": "GOOGL",
    "type": "BUY",
    "side": "LIMIT",
    "quantity": 15,
    "limitPrice": 148.00
  }'
```

**Example:**
```bash
curl -X PUT http://localhost:8081/api/v1/trading/orders/4f15b15b-d7a8-4044-89d4-0e9b04aa95da \
  -H "Content-Type: application/json" \
  -H "X-User-Id: 550e8400-e29b-41d4-a716-446655440000" \
  -d '{
    "symbol": "GOOGL",
    "type": "BUY",
    "side": "LIMIT",
    "quantity": 15,
    "limitPrice": 148.00
  }'
```

**Expected Response:**
- Status: `200 OK`
- Updated order
- `OrderUpdatedEvent` published to Kafka
- Read model updated in Redis

---

## 7. EXECUTE ORDER (Partial Execution)

**Note:** Can execute orders with status `PENDING` or `PARTIALLY_EXECUTED`

### Scenario 7.1: Partial Execution (50% of order)
```bash
# Replace {ORDER_ID} with actual order ID
# If order quantity is 10, execute 5
curl -X POST "http://localhost:8081/api/v1/trading/orders/{ORDER_ID}/execute?executionPrice=146.00&executionQuantity=5"
```

**Example:**
```bash
curl -X POST "http://localhost:8081/api/v1/trading/orders/4f15b15b-d7a8-4044-89d4-0e9b04aa95da/execute?executionPrice=146.00&executionQuantity=5"
```

**Expected Response:**
- Status: `200 OK`
- Order status: `PARTIALLY_EXECUTED`
- `executedQuantity`: 5 (partial)
- `executedPrice`: 146.00
- `OrderExecutedEvent` published to Kafka
- Read model updated in Redis

### Scenario 7.2: Execute Remaining Quantity (Complete the order)
```bash
# Execute the remaining 5 units to complete the order
curl -X POST "http://localhost:8081/api/v1/trading/orders/{ORDER_ID}/execute?executionPrice=147.50&executionQuantity=5"
```

**Expected Response:**
- Status: `200 OK`
- Order status: `EXECUTED`
- `executedQuantity`: 10 (full)
- `executedPrice`: 147.50 (or weighted average)
- `OrderExecutedEvent` published to Kafka
- Read model updated in Redis

### Scenario 7.3: Full Execution in One Go
```bash
# If order quantity is 10, execute all 10 at once
curl -X POST "http://localhost:8081/api/v1/trading/orders/{ORDER_ID}/execute?executionPrice=145.75&executionQuantity=10"
```

**Expected Response:**
- Status: `200 OK`
- Order status: `EXECUTED`
- `executedQuantity`: 10 (full)
- `executedPrice`: 145.75
- `OrderExecutedEvent` published to Kafka
- Read model updated in Redis

---

## 8. CANCEL ORDER

**Note:** Cannot cancel orders with status `EXECUTED`

```bash
# Replace {ORDER_ID} with actual order ID
curl -X DELETE "http://localhost:8081/api/v1/trading/orders/{ORDER_ID}"
```

**Example:**
```bash
curl -X DELETE "http://localhost:8081/api/v1/trading/orders/4f15b15b-d7a8-4044-89d4-0e9b04aa95da"
```

**Expected Response:**
- Status: `200 OK`
- Order status: `CANCELLED`
- `OrderCancelledEvent` published to Kafka
- Read model updated in Redis

---

## COMPLETE TEST FLOW

### Full End-to-End Test Scenario

```bash
# Step 1: Create a new order
ORDER_RESPONSE=$(curl -s -X POST http://localhost:8081/api/v1/trading/orders \
  -H "Content-Type: application/json" \
  -H "X-User-Id: 550e8400-e29b-41d4-a716-446655440000" \
  -d '{
    "symbol": "GOOGL",
    "type": "BUY",
    "side": "LIMIT",
    "quantity": 10,
    "limitPrice": 145.50
  }')

# Extract order ID (adjust based on your response format)
ORDER_ID=$(echo $ORDER_RESPONSE | jq -r '.data.id')
echo "Created Order ID: $ORDER_ID"

# Step 2: Get the order to verify
curl -X GET "http://localhost:8081/api/v1/trading/orders/$ORDER_ID"

# Step 3: Partially execute (50%)
curl -X POST "http://localhost:8081/api/v1/trading/orders/$ORDER_ID/execute?executionPrice=146.00&executionQuantity=5"

# Step 4: Get order to verify partial execution
curl -X GET "http://localhost:8081/api/v1/trading/orders/$ORDER_ID"

# Step 5: Execute remaining quantity
curl -X POST "http://localhost:8081/api/v1/trading/orders/$ORDER_ID/execute?executionPrice=147.50&executionQuantity=5"

# Step 6: Get order to verify full execution
curl -X GET "http://localhost:8081/api/v1/trading/orders/$ORDER_ID"

# Step 7: Try to cancel (should fail - order is already executed)
curl -X DELETE "http://localhost:8081/api/v1/trading/orders/$ORDER_ID"
```

### Alternative Flow: Cancel Before Execution

```bash
# Step 1: Create order
ORDER_RESPONSE=$(curl -s -X POST http://localhost:8081/api/v1/trading/orders \
  -H "Content-Type: application/json" \
  -H "X-User-Id: 550e8400-e29b-41d4-a716-446655440000" \
  -d '{
    "symbol": "AAPL",
    "type": "SELL",
    "side": "LIMIT",
    "quantity": 5,
    "limitPrice": 180.00
  }')

ORDER_ID=$(echo $ORDER_RESPONSE | jq -r '.data.id')

# Step 2: Cancel the order
curl -X DELETE "http://localhost:8081/api/v1/trading/orders/$ORDER_ID"

# Step 3: Try to execute (should fail - order is cancelled)
curl -X POST "http://localhost:8081/api/v1/trading/orders/$ORDER_ID/execute?executionPrice=180.00&executionQuantity=5"
```

---

## VALIDATION SCENARIOS (Error Cases)

### Invalid Order Creation (Missing Limit Price for LIMIT order)
```bash
curl -X POST http://localhost:8081/api/v1/trading/orders \
  -H "Content-Type: application/json" \
  -H "X-User-Id: 550e8400-e29b-41d4-a716-446655440000" \
  -d '{
    "symbol": "GOOGL",
    "type": "BUY",
    "side": "LIMIT",
    "quantity": 10
  }'
```
**Expected:** `400 Bad Request` - "Limit price is required for LIMIT orders"

### Invalid Order Creation (Zero Quantity)
```bash
curl -X POST http://localhost:8081/api/v1/trading/orders \
  -H "Content-Type: application/json" \
  -H "X-User-Id: 550e8400-e29b-41d4-a716-446655440000" \
  -d '{
    "symbol": "GOOGL",
    "type": "BUY",
    "side": "MARKET",
    "quantity": 0
  }'
```
**Expected:** `400 Bad Request` - "Quantity must be greater than zero"

### Invalid Execution (Exceeds Order Quantity)
```bash
# If order quantity is 10, try to execute 15
curl -X POST "http://localhost:8081/api/v1/trading/orders/{ORDER_ID}/execute?executionPrice=146.00&executionQuantity=15"
```
**Expected:** `400 Bad Request` - "Execution quantity cannot exceed order quantity"

### Invalid Execution (Zero Price)
```bash
curl -X POST "http://localhost:8081/api/v1/trading/orders/{ORDER_ID}/execute?executionPrice=0&executionQuantity=5"
```
**Expected:** `400 Bad Request` - "Execution price must be greater than zero"

### Cancel Already Executed Order
```bash
# After executing an order fully, try to cancel
curl -X DELETE "http://localhost:8081/api/v1/trading/orders/{ORDER_ID}"
```
**Expected:** `400 Bad Request` - "Cannot cancel executed order"

### Update Executed Order
```bash
# After executing an order, try to update
curl -X PUT http://localhost:8081/api/v1/trading/orders/{ORDER_ID} \
  -H "Content-Type: application/json" \
  -H "X-User-Id: 550e8400-e29b-41d4-a716-446655440000" \
  -d '{
    "symbol": "GOOGL",
    "type": "BUY",
    "side": "LIMIT",
    "quantity": 15,
    "limitPrice": 148.00
  }'
```
**Expected:** `400 Bad Request` - "Cannot update order with status: EXECUTED"

---

## VERIFYING EVENTS IN KAFKA

After each operation, check Kafka logs or use a Kafka consumer to verify events are published:
- `OrderCreatedEvent` - When order is created
- `OrderUpdatedEvent` - When order is updated
- `OrderExecutedEvent` - When order is executed
- `OrderCancelledEvent` - When order is cancelled

---

## VERIFYING REDIS READ MODELS

After each operation, check Redis to verify read models are updated:
- Order data stored in `OrderReadModel:{orderId}` hash
- Indexes updated: `userId:{userId}` and `symbol:{symbol}` SETs

---

## SWAGGER UI

Access Swagger UI for interactive testing:
```
http://localhost:8081/swagger-ui.html
```

---

## NOTES

1. **Order Status Flow:**
   - `PENDING` → Can be updated, cancelled, or executed
   - `PARTIALLY_EXECUTED` → Can be executed again (remaining quantity) or cancelled
   - `EXECUTED` → Final state, cannot be modified
   - `CANCELLED` → Final state, cannot be modified

2. **Event Sourcing:**
   - All state changes are published as events to Kafka
   - Events are consumed by `OrderEventListener`
   - Read models in Redis are updated via events (CQRS pattern)

3. **User ID:**
   - Include `X-User-Id` header for create/update operations
   - If not provided, a default system user UUID is used

4. **Execution:**
   - Can execute orders multiple times (partial executions)
   - Final execution that completes the order sets status to `EXECUTED`
   - Each execution publishes an `OrderExecutedEvent`
