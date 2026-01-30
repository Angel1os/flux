# Viewing Rate Limiting Data in Redis Insight

## What to Look For

### Key Pattern
```
request_rate_limiter.{USER_ID}
```

### Examples:
```
request_rate_limiter.1923a882-2e82-4bf2-9d0a-32b807ddde90
request_rate_limiter.192.168.1.1
request_rate_limiter.-1756226236  (if using hash code - old behavior)
```

---

## How to Find It in Redis Insight

### Step 1: Connect to Redis
1. Open Redis Insight
2. Add connection:
   - **Host**: `localhost`
   - **Port**: `6379`
   - **Username**: `default` (or your Redis username)
   - **Password**: `redis123` (or your Redis password)

### Step 2: Search for Rate Limit Keys
1. Go to **Browser** tab
2. In the search box, type: `request_rate_limiter.*`
3. Click **Search** or press Enter

### Step 3: View Key Details
Click on any key to see its data structure.

---

## Data Structure

Each rate limit key is a **Hash** with the following fields:

```json
{
  "tokens": "15",                    // Current tokens available
  "last_refill": "1738204800000",    // Timestamp of last token refill (milliseconds)
  "replenish_rate": "10",            // Tokens added per second
  "burst_capacity": "20"             // Maximum tokens in bucket
}
```

---

## Visual Guide

### In Redis Insight Browser:

```
📁 Browser
  ├── 📄 request_rate_limiter.1923a882-2e82-4bf2-9d0a-32b807ddde90
  │   ├── Type: Hash
  │   ├── TTL: -1 (no expiration)
  │   └── Fields:
  │       ├── tokens: "15"
  │       ├── last_refill: "1738204800000"
  │       ├── replenish_rate: "10"
  │       └── burst_capacity: "20"
  │
  ├── 📄 request_rate_limiter.192.168.1.1
  │   └── (same structure)
  │
  └── 📄 request_rate_limiter.unknown
      └── (same structure)
```

---

## Understanding the Values

### `tokens`
- **Current tokens available** for this user
- Decrements by 1 for each request
- Replenishes at `replenish_rate` per second
- Cannot exceed `burst_capacity`

### `last_refill`
- **Unix timestamp** (milliseconds) of last token refill
- Used to calculate how many tokens to add
- Formula: `tokens_to_add = (current_time - last_refill) / 1000 * replenish_rate`

### `replenish_rate`
- **Tokens per second** (from your config: `10`)
- How fast tokens are added back

### `burst_capacity`
- **Maximum tokens** (from your config: `20`)
- Initial capacity and maximum limit

---

## Real-Time Monitoring

### Watch Tokens Decrease:
1. Make a request to the gateway
2. Refresh Redis Insight
3. Watch `tokens` value decrease by 1

### Watch Tokens Replenish:
1. Wait 1 second
2. Refresh Redis Insight
3. Watch `tokens` value increase by 10 (up to `burst_capacity`)

---

## Example Scenarios

### Scenario 1: Fresh User (No Previous Requests)
```
Key: request_rate_limiter.user123
{
  "tokens": "20",              // Full bucket
  "last_refill": "1738204800000",
  "replenish_rate": "10",
  "burst_capacity": "20"
}
```

### Scenario 2: After 5 Requests
```
Key: request_rate_limiter.user123
{
  "tokens": "15",              // 20 - 5 = 15
  "last_refill": "1738204805000",  // Updated timestamp
  "replenish_rate": "10",
  "burst_capacity": "20"
}
```

### Scenario 3: After 20 Requests (Bucket Empty)
```
Key: request_rate_limiter.user123
{
  "tokens": "0",               // Empty bucket
  "last_refill": "1738204820000",
  "replenish_rate": "10",
  "burst_capacity": "20"
}
```

### Scenario 4: After 1 Second Wait (Tokens Replenished)
```
Key: request_rate_limiter.user123
{
  "tokens": "10",              // Replenished: 0 + 10 = 10
  "last_refill": "1738204821000",  // New timestamp
  "replenish_rate": "10",
  "burst_capacity": "20"
}
```

---

## Filtering in Redis Insight

### Find All Rate Limit Keys:
```
Pattern: request_rate_limiter.*
```

### Find Specific User:
```
Pattern: request_rate_limiter.1923a882-2e82-4bf2-9d0a-32b807ddde90
```

### Find IP-Based Limits:
```
Pattern: request_rate_limiter.192.168.*
```

---

## Troubleshooting

### No Keys Found?
1. **Check if Redis is connected** in Redis Insight
2. **Make a request** to the gateway first (keys are created on first request)
3. **Check key pattern** - make sure you're searching for `request_rate_limiter.*`

### Keys Not Updating?
1. **Refresh** Redis Insight (F5 or refresh button)
2. **Check gateway logs** for rate limiter errors
3. **Verify Redis connection** in gateway `application.yml`

### Wrong User ID in Key?
- Check `JwtExtractionFilter` - it should extract `sub` claim from JWT
- Should see UUID like `1923a882-2e82-4bf2-9d0a-32b807ddde90`
- If you see hash codes like `-1756226236`, the JWT extraction is broken

---

## Quick Test in Redis Insight

1. **Before making requests:**
   - Search: `request_rate_limiter.*`
   - Should see: No keys (or old keys)

2. **Make 5 rapid requests:**
   ```bash
   for i in {1..5}; do
     curl -H "Authorization: Bearer YOUR_JWT" \
          http://localhost:8090/api/v1/trading/orders
   done
   ```

3. **Refresh Redis Insight:**
   - Search: `request_rate_limiter.*`
   - Should see: New key with your user ID
   - Click on it → See `tokens: "15"` (20 - 5 = 15)

4. **Wait 2 seconds, refresh:**
   - Should see: `tokens: "20"` (replenished to max)

---

## Pro Tips

### 1. Monitor Multiple Users
- Each user gets their own key
- Compare token counts across users
- See which users are hitting rate limits

### 2. Check TTL
- Rate limit keys typically have **no expiration** (TTL: -1)
- They persist until Redis is cleared or key is deleted

### 3. Export Data
- Redis Insight allows exporting key data
- Useful for analyzing rate limit patterns

### 4. Real-Time Updates
- Enable **auto-refresh** in Redis Insight
- Watch tokens change in real-time as requests come in

---

## Summary

✅ **Yes, you can see rate limiting data in Redis Insight!**

**Key Pattern:** `request_rate_limiter.{USER_ID}`

**Data Type:** Hash with fields:
- `tokens` - Current available tokens
- `last_refill` - Last replenishment timestamp
- `replenish_rate` - Tokens per second
- `burst_capacity` - Maximum tokens

**How to Find:**
1. Connect to Redis in Redis Insight
2. Search: `request_rate_limiter.*`
3. Click on any key to view details
