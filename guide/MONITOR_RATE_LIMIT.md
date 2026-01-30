# Monitor Rate Limiting Redis Commands

## The Issue
- ✅ Redis is connected (logs show connection success)
- ✅ Rate limiter is executing (logs show `RedisRateLimiter` responses)
- ❌ No `request_rate_limiter.*` keys in Redis

## Solution: Monitor Redis Commands

### Step 1: Monitor Redis Commands in Real-Time

Open a new terminal and run:
```bash
redis-cli MONITOR
```

This will show ALL Redis commands being executed.

### Step 2: Make a Request

In another terminal, make a request:
```bash
curl -H "Authorization: Bearer YOUR_JWT" \
     http://localhost:8090/api/v1/market-data/prices/GOOGL/current
```

### Step 3: Look for Rate Limiter Commands

In the `MONITOR` output, look for:
- `EVAL` or `EVALSHA` commands (Lua scripts used by rate limiter)
- `HGET` or `HSET` commands with `request_rate_limiter` in the key
- `GET` or `SET` commands

### Expected Output:
```
1738204800.123456 [0 127.0.0.1:58162] "EVAL" "local tokens_key = KEYS[1]..." "1" "request_rate_limiter.1923a882-2e82-4bf2-9d0a-32b807ddde90" "10" "20" "1" "1738204800123"
```

---

## Alternative: Check Redis with Pattern Search

### Search for ANY keys that might be rate limit related:
```bash
redis-cli
> KEYS *rate*
> KEYS *limit*
> KEYS *limiter*
```

### Check if keys are being created with different patterns:
```bash
redis-cli
> KEYS *
# Look for any keys that don't start with "OrderReadModel"
```

---

## Why Keys Might Not Appear

### 1. Keys Expire Immediately
Rate limit keys might have a TTL and expire right away. Check:
```bash
redis-cli
> TTL request_rate_limiter.USER_ID
# If returns -2, key doesn't exist
# If returns -1, key exists but no TTL
# If returns positive number, that's seconds until expiration
```

### 2. Different Key Pattern
Spring Cloud Gateway might use a different key pattern. Check gateway source or logs for actual key names.

### 3. Lua Script Execution
Rate limiter uses Lua scripts. If scripts fail silently, keys won't be created. Check for errors in:
- Gateway logs
- Redis logs
- MONITOR output

---

## Debug Steps

1. **Enable Redis command logging:**
   ```bash
   redis-cli MONITOR > redis-commands.log
   ```

2. **Make a request:**
   ```bash
   curl -H "Authorization: Bearer JWT" \
        http://localhost:8090/api/v1/market-data/prices/GOOGL/current
   ```

3. **Stop monitoring (Ctrl+C) and check log:**
   ```bash
   grep -i "rate\|limiter\|eval" redis-commands.log
   ```

4. **Check if keys were created:**
   ```bash
   redis-cli KEYS "*"
   ```

---

## Most Likely Cause

Based on the logs showing `tokensRemaining=-1`, the rate limiter might be:
1. Using in-memory mode (fallback when Redis fails)
2. Not properly configured to use Redis
3. Using a different Redis instance/database

The fact that Redis IS connected but keys aren't appearing suggests the rate limiter might not be using the Redis connection properly.
