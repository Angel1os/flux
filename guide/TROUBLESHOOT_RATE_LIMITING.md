# Troubleshooting: Rate Limiting Keys Not Appearing in Redis

## Problem: `KEYS request_rate_limiter.*` returns empty

If you've made requests but don't see rate limit keys in Redis, check these:

---

## Step 1: Verify Redis Connection

### Check if Gateway can connect to Redis:

```bash
# Test Redis connection from terminal
redis-cli -h localhost -p 6379 ping
# Should return: PONG
```

### Check Gateway logs for Redis connection errors:

Look for errors like:
- `Unable to connect to Redis`
- `RedisConnectionException`
- `Connection refused`

---

## Step 2: Verify Route Matches Your Request

### Your route configuration:
```yaml
- Path=/api/v1/trading/orders/**
```

### Make sure you're calling:
```
http://localhost:8090/api/v1/trading/orders
```

**NOT:**
- ❌ `http://localhost:8090/api/trading/orders` (missing `/v1`)
- ❌ `http://localhost:8090/trading/orders` (wrong path)

---

## Step 3: Check if Rate Limiter Filter is Applied

### Verify in Gateway logs:
Look for messages like:
- `RouteDefinition trading-service-orders applying filter RequestRateLimiter`
- Any errors about `RequestRateLimiter`

### Test with a simple request:
```bash
curl -v -H "Authorization: Bearer YOUR_JWT" \
     http://localhost:8090/api/v1/trading/orders
```

Check the response headers - you should see rate limit headers if working.

---

## Step 4: Verify User ID Extraction

### Check Gateway logs for:
```
Extracted user ID from JWT: 1923a882-2e82-4bf2-9d0a-32b807ddde90
```

If you see hash codes like `-1756226236`, the JWT extraction is broken.

### Test JWT extraction:
1. Add logging to `JwtExtractionFilter`
2. Make a request
3. Check logs for extracted user ID

---

## Step 5: Check Redis Configuration

### Verify in `application.yml`:
```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      username: ${REDIS_USERNAME:default}
      password: ${REDIS_PASSWORD:redis123}
```

### Test Redis connection from Gateway:
Add this to a test endpoint or check startup logs:
- Gateway should log Redis connection on startup
- Look for: `RedisConnectionFactory` or `LettuceConnectionFactory`

---

## Step 6: Verify Rate Limiter Dependency

### Check if Redis rate limiter is available:
The gateway needs `spring-boot-starter-data-redis-reactive` for rate limiting.

### Verify in `pom.xml`:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis-reactive</artifactId>
</dependency>
```

---

## Step 7: Check for Errors in Gateway Logs

### Common errors:

1. **Redis Connection Failed:**
   ```
   Unable to connect to Redis at localhost:6379
   ```
   **Fix:** Start Redis or check connection settings

2. **KeyResolver Not Found:**
   ```
   No qualifying bean of type 'KeyResolver' available
   ```
   **Fix:** Check `RateLimiterConfig.java` - `userKeyResolver` should be `@Primary`

3. **Route Not Matching:**
   ```
   No route found for path: /api/trading/orders
   ```
   **Fix:** Check route path matches your request URL

---

## Step 8: Manual Test

### 1. Make a request:
```bash
curl -H "Authorization: Bearer YOUR_JWT" \
     http://localhost:8090/api/v1/trading/orders
```

### 2. Immediately check Redis:
```bash
redis-cli
> KEYS request_rate_limiter.*
```

### 3. If still empty, check all keys:
```bash
redis-cli
> KEYS *
```

Maybe keys are being created with a different pattern?

---

## Step 9: Enable Debug Logging

### Add to `application.yml`:
```yaml
logging:
  level:
    org.springframework.cloud.gateway: DEBUG
    org.springframework.data.redis: DEBUG
    com.angellos.api.gateway: DEBUG
```

### Restart gateway and make a request:
You should see detailed logs about:
- Route matching
- Filter execution
- Redis operations
- KeyResolver calls

---

## Step 10: Verify Request Actually Hits Gateway

### Test if request reaches gateway:
```bash
# Add logging to see if request arrives
curl -v http://localhost:8090/api/v1/trading/orders
```

Check gateway logs for:
- Request received
- Route matched
- Filter applied

---

## Quick Diagnostic Script

```bash
#!/bin/bash

echo "🔍 Rate Limiting Diagnostic"
echo "=========================="
echo ""

# 1. Check Redis
echo "1. Checking Redis connection..."
if redis-cli ping > /dev/null 2>&1; then
    echo "   ✅ Redis is running"
else
    echo "   ❌ Redis is NOT running"
    exit 1
fi

# 2. Check Redis keys
echo ""
echo "2. Checking for rate limit keys..."
KEYS=$(redis-cli KEYS "request_rate_limiter.*" 2>/dev/null | wc -l)
echo "   Found $KEYS rate limit keys"

# 3. Check all keys
echo ""
echo "3. Total Redis keys:"
TOTAL=$(redis-cli DBSIZE 2>/dev/null)
echo "   Total keys in database: $TOTAL"

# 4. List all keys (first 10)
echo ""
echo "4. Sample keys in Redis:"
redis-cli KEYS "*" 2>/dev/null | head -10

echo ""
echo "✅ Diagnostic complete"
```

---

## Common Solutions

### Solution 1: Route Path Mismatch
**Problem:** Request path doesn't match route predicate
**Fix:** Update route or request URL to match

### Solution 2: Redis Not Connected
**Problem:** Gateway can't connect to Redis
**Fix:** 
- Start Redis: `redis-server`
- Check connection settings in `application.yml`
- Verify Redis credentials

### Solution 3: User ID Not Extracted
**Problem:** `JwtExtractionFilter` not extracting user ID
**Fix:** 
- Check JWT token is valid
- Verify `JwtExtractionFilter` is working
- Check logs for extracted user ID

### Solution 4: Rate Limiter Not Applied
**Problem:** Filter not being executed
**Fix:**
- Verify route matches
- Check filter configuration
- Enable debug logging

---

## Expected Behavior

### When Working Correctly:

1. **Make request:**
   ```bash
   curl -H "Authorization: Bearer JWT" \
        http://localhost:8090/api/v1/trading/orders
   ```

2. **Check Redis immediately:**
   ```bash
   redis-cli
   > KEYS request_rate_limiter.*
   1) "request_rate_limiter.1923a882-2e82-4bf2-9d0a-32b807ddde90"
   ```

3. **View key data:**
   ```bash
   > HGETALL request_rate_limiter.1923a882-2e82-4bf2-9d0a-32b807ddde90
   1) "tokens"
   2) "19"
   3) "last_refill"
   4) "1738204800000"
   5) "replenish_rate"
   6) "10"
   7) "burst_capacity"
   8) "20"
   ```

---

## Still Not Working?

1. **Check gateway is actually running** on port 8090
2. **Verify request is hitting gateway** (not going directly to service)
3. **Check gateway logs** for any errors
4. **Test with a simple route** without rate limiting first
5. **Verify Redis is the same instance** gateway is connecting to
