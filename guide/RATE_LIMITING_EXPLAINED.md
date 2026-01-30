# Rate Limiting with Redis - How It Works

## Overview
Spring Cloud Gateway uses **Redis** to implement **Token Bucket Algorithm** for rate limiting.

## Architecture

```
Client Request
    ↓
API Gateway (RequestRateLimiter Filter)
    ↓
KeyResolver → Extracts "user123" or "192.168.1.1"
    ↓
Redis Lookup: "request_rate_limiter.user123"
    ↓
Token Bucket Check:
  - Has tokens? → Allow request, decrement tokens
  - No tokens? → Reject (HTTP 429)
```

## Redis Data Structure

### Keys Stored in Redis:
```
request_rate_limiter.user123
request_rate_limiter.192.168.1.1
request_rate_limiter.admin
```

### Values (Hash):
```
{
  "tokens": 15,           # Current tokens available
  "last_refill": 1234567890,  # Timestamp of last token refill
  "replenish_rate": 10,   # Tokens per second
  "burst_capacity": 20    # Max tokens
}
```

## How Token Bucket Works

### Example: Trading Service Route
```yaml
redis-rate-limiter.replenishRate: 10   # 10 tokens/second
redis-rate-limiter.burstCapacity: 20   # Max 20 tokens
redis-rate-limiter.requestedTokens: 1  # 1 token per request
```

### Timeline:
```
Time 0s:  User makes request → 20 tokens available → Allow → 19 tokens left
Time 0.1s: User makes request → 19 tokens available → Allow → 18 tokens left
Time 0.2s: User makes request → 18 tokens available → Allow → 17 tokens left
...
Time 1.0s: 10 new tokens added (replenishRate) → 17 + 10 = 27 → Capped at 20
Time 1.1s: User makes request → 20 tokens available → Allow → 19 tokens left
```

### If User Exceeds Rate:
```
Time 2.0s: User makes 25th request in 2 seconds
          → 0 tokens available
          → REJECT → HTTP 429 Too Many Requests
```

## Code Flow

### 1. Request Arrives
```java
// In Spring Cloud Gateway's RequestRateLimiterGatewayFilterFactory
public Mono<Response> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    // Get key from KeyResolver
    String key = keyResolver.resolve(exchange).block();
    // "user123" or "192.168.1.1"
```

### 2. Redis Check (Internal Spring Code)
```java
// Spring Cloud Gateway uses RedisRateLimiter internally
RedisRateLimiter rateLimiter = new RedisRateLimiter(
    replenishRate,  // 10
    burstCapacity,  // 20
    requestedTokens // 1
);

// Lua script executed in Redis:
// 1. Get current tokens
// 2. Calculate time since last refill
// 3. Add replenished tokens (time * replenishRate)
// 4. Cap at burstCapacity
// 5. If tokens >= requestedTokens: allow, decrement
// 6. Else: reject
```

### 3. Response
```java
if (allowed) {
    return chain.filter(exchange);  // Continue to downstream service
} else {
    exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
    return exchange.getResponse().setComplete();  // HTTP 429
}
```

## Testing Rate Limiting

### Test with curl:
```bash
# Make 25 rapid requests
for i in {1..25}; do
  curl -H "Authorization: Bearer YOUR_JWT_TOKEN" \
       http://localhost:8090/api/trading/orders
  echo "Request $i"
done

# First 20 requests: HTTP 200 OK
# Requests 21-25: HTTP 429 Too Many Requests
```

### Check Redis:
```bash
redis-cli
> KEYS request_rate_limiter.*
> HGETALL request_rate_limiter.user123
```

## Configuration Per Route

### Trading Service (Stricter):
```yaml
redis-rate-limiter.replenishRate: 10   # 10 req/sec
redis-rate-limiter.burstCapacity: 20   # Burst: 20
```

### Market Data (More Permissive):
```yaml
redis-rate-limiter.replenishRate: 20   # 20 req/sec
redis-rate-limiter.burstCapacity: 40   # Burst: 40
```

### GraphQL (Most Permissive):
```yaml
redis-rate-limiter.replenishRate: 30   # 30 req/sec
redis-rate-limiter.burstCapacity: 60   # Burst: 60
```

## Why Redis?

1. **Distributed**: Multiple gateway instances share the same rate limit
2. **Fast**: In-memory operations (microseconds)
3. **Atomic**: Lua scripts ensure thread-safe token operations
4. **Persistent**: Can configure Redis persistence if needed
5. **Scalable**: Redis cluster for high throughput

## Key Takeaways

- **No custom code needed**: Spring Cloud Gateway handles Redis operations
- **KeyResolver**: Your code determines WHO to rate limit (user vs IP)
- **Token Bucket**: Algorithm implemented by Spring Cloud Gateway
- **Redis**: Stores token counts per client
- **Configuration**: All in `application.yml` + `RateLimiterConfig.java`
