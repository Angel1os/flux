# How to Test Rate Limiting

## Understanding Your Rate Limit Configuration

```yaml
redis-rate-limiter.replenishRate: 10   # 10 tokens per SECOND
redis-rate-limiter.burstCapacity: 20   # Max 20 tokens in bucket
redis-rate-limiter.requestedTokens: 1   # 1 token per request
```

### What This Means:
- **10 requests per second** (sustained rate)
- **20 requests** can be made immediately (burst)
- After burst, tokens replenish at **10 per second**

### In One Minute:
- You can make **up to 600 requests** (10 req/sec × 60 seconds)
- The limit is **per second**, not per minute!

---

## Quick Test Commands

### Test 1: Rapid Burst (Should Rate Limit After 20 Requests)

```bash
# Replace YOUR_JWT_TOKEN with your actual token
JWT_TOKEN="YOUR_JWT_TOKEN"

# Make 25 rapid requests
for i in {1..25}; do
  echo -n "Request $i: "
  curl -s -o /dev/null -w "HTTP %{http_code}\n" \
    -H "Authorization: Bearer $JWT_TOKEN" \
    http://localhost:8090/api/v1/trading/orders
  sleep 0.01  # Tiny delay to avoid connection issues
done
```

**Expected Result:**
- First ~20 requests: `HTTP 200` or `HTTP 401/404` (allowed)
- Requests 21-25: `HTTP 429` (Too Many Requests - rate limited)

---

### Test 2: Sustained Rate (Should All Succeed)

```bash
# Make 50 requests at exactly 10 req/sec
for i in {1..50}; do
  echo -n "Request $i: "
  curl -s -o /dev/null -w "HTTP %{http_code}\n" \
    -H "Authorization: Bearer $JWT_TOKEN" \
    http://localhost:8090/api/v1/trading/orders
  sleep 0.1  # 0.1 seconds = 10 requests per second
done
```

**Expected Result:**
- All 50 requests: `HTTP 200` or `HTTP 401/404` (all allowed)
- No `HTTP 429` responses

---

### Test 3: Exceed Sustained Rate (Should Rate Limit)

```bash
# Make 50 requests at 20 req/sec (exceeds 10 req/sec limit)
for i in {1..50}; do
  echo -n "Request $i: "
  curl -s -o /dev/null -w "HTTP %{http_code}\n" \
    -H "Authorization: Bearer $JWT_TOKEN" \
    http://localhost:8090/api/v1/trading/orders
  sleep 0.05  # 0.05 seconds = 20 requests per second (too fast!)
done
```

**Expected Result:**
- First ~20 requests: `HTTP 200` (burst capacity)
- Remaining requests: Mix of `HTTP 200` and `HTTP 429` (rate limited)

---

## Using the Test Script

```bash
# 1. Edit the script and add your JWT token
nano guide/test-rate-limiting.sh

# 2. Run the test
./guide/test-rate-limiting.sh
```

---

## Check Redis Directly

### View Rate Limit Keys:
```bash
redis-cli
> KEYS request_rate_limiter.*
```

### View Token Count for a User:
```bash
# Replace USER_ID with actual user ID from JWT (sub claim)
redis-cli
> HGETALL request_rate_limiter.USER_ID
```

**Example Output:**
```
1) "tokens"
2) "15"              # Current tokens available
3) "last_refill"     # Timestamp
4) "replenish_rate"
5) "10"
6) "burst_capacity"
7) "20"
```

---

## Troubleshooting

### Rate Limiting Not Working?

1. **Check Redis is running:**
   ```bash
   redis-cli ping
   # Should return: PONG
   ```

2. **Check Gateway logs:**
   ```bash
   # Look for rate limiter errors
   grep -i "rate" api-gateway/logs/application.log
   ```

3. **Verify route matches:**
   - Your route: `/api/v1/trading/orders/**`
   - Make sure you're hitting: `http://localhost:8090/api/v1/trading/orders`

4. **Check user ID extraction:**
   - Gateway should extract user ID from JWT `sub` claim
   - Verify in debugger: `userId` should be UUID, not hash code

5. **Test with same user:**
   - Use the **same JWT token** for all requests
   - Different tokens = different users = different rate limits

---

## Expected Behavior Examples

### Scenario 1: 25 Rapid Requests (0.01s apart)
```
Request 1-20:  ✅ Allowed (burst capacity)
Request 21:    🚫 HTTP 429 (no tokens left)
Request 22:    🚫 HTTP 429
Request 23:    🚫 HTTP 429
Request 24:    🚫 HTTP 429
Request 25:    🚫 HTTP 429
```

### Scenario 2: 50 Requests at 10 req/sec (0.1s apart)
```
Request 1-50:  ✅ All Allowed (within sustained rate)
```

### Scenario 3: 50 Requests at 20 req/sec (0.05s apart)
```
Request 1-20:  ✅ Allowed (burst)
Request 21-30: 🚫 HTTP 429 (exceeded rate, waiting for tokens)
Request 31-40: ✅ Some allowed (tokens replenished)
Request 41-50: 🚫 HTTP 429 (still exceeding rate)
```

---

## Understanding Token Bucket

```
Time 0.0s:  [████████████████████] 20 tokens (full bucket)
Request 1: [███████████████████░] 19 tokens
Request 2: [██████████████████░░] 18 tokens
...
Request 20: [░░░░░░░░░░░░░░░░░░░░] 0 tokens
Request 21: [░░░░░░░░░░░░░░░░░░░░] 0 tokens → REJECT (HTTP 429)

Time 1.0s:  [████████████████████] 20 tokens (replenished)
           ↑ 10 tokens added per second
```

---

## Quick One-Liner Test

```bash
# Make 30 rapid requests and count 429 responses
JWT="YOUR_JWT_TOKEN"
for i in {1..30}; do
  curl -s -o /dev/null -w "%{http_code}\n" \
    -H "Authorization: Bearer $JWT" \
    http://localhost:8090/api/v1/trading/orders
done | grep -c "429"

# Should return: ~10 (30 requests - 20 burst = ~10 rate limited)
```
