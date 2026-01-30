#!/bin/bash

# Rate Limiting Test Script
# Tests the API Gateway rate limiting functionality

GATEWAY_URL="http://localhost:8090"
JWT_TOKEN="YOUR_JWT_TOKEN_HERE"  # Replace with your actual JWT token

echo "🧪 Testing Rate Limiting on API Gateway"
echo "========================================"
echo ""
echo "Configuration:"
echo "  - Replenish Rate: 10 requests/second"
echo "  - Burst Capacity: 20 requests"
echo "  - Requested Tokens: 1 per request"
echo ""
echo "This means:"
echo "  - You can make 10 requests per second (sustained)"
echo "  - You can burst up to 20 requests immediately"
echo "  - After burst, you need to wait for tokens to replenish"
echo ""

# Check if Redis is running
echo "📡 Checking Redis connection..."
if ! redis-cli -h localhost -p 6379 ping > /dev/null 2>&1; then
    echo "❌ ERROR: Redis is not running or not accessible!"
    echo "   Start Redis: redis-server"
    exit 1
fi
echo "✅ Redis is running"
echo ""

# Test 1: Make 25 rapid requests (should allow first 20, then rate limit)
echo "Test 1: Making 25 rapid requests (should allow ~20, then rate limit)"
echo "-------------------------------------------------------------------"
SUCCESS=0
RATE_LIMITED=0

for i in {1..25}; do
    RESPONSE=$(curl -s -w "\n%{http_code}" \
        -H "Authorization: Bearer $JWT_TOKEN" \
        "$GATEWAY_URL/api/trading/orders" 2>/dev/null)
    
    HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
    BODY=$(echo "$RESPONSE" | sed '$d')
    
    if [ "$HTTP_CODE" = "200" ] || [ "$HTTP_CODE" = "401" ] || [ "$HTTP_CODE" = "404" ]; then
        SUCCESS=$((SUCCESS + 1))
        echo -n "✅ Request $i: HTTP $HTTP_CODE | "
    elif [ "$HTTP_CODE" = "429" ]; then
        RATE_LIMITED=$((RATE_LIMITED + 1))
        echo -n "🚫 Request $i: HTTP 429 (Rate Limited) | "
    else
        echo -n "⚠️  Request $i: HTTP $HTTP_CODE | "
    fi
    
    # Show progress every 5 requests
    if [ $((i % 5)) -eq 0 ]; then
        echo ""
    fi
done

echo ""
echo ""
echo "Results:"
echo "  ✅ Successful requests: $SUCCESS"
echo "  🚫 Rate limited requests: $RATE_LIMITED"
echo ""

# Test 2: Check Redis keys
echo "Test 2: Checking Redis for rate limit keys"
echo "------------------------------------------"
echo "Redis keys for rate limiting:"
redis-cli -h localhost -p 6379 KEYS "request_rate_limiter.*" 2>/dev/null | head -5

if [ -n "$(redis-cli -h localhost -p 6379 KEYS 'request_rate_limiter.*' 2>/dev/null)" ]; then
    echo ""
    echo "Sample rate limit data (first key):"
    FIRST_KEY=$(redis-cli -h localhost -p 6379 KEYS "request_rate_limiter.*" 2>/dev/null | head -1)
    if [ -n "$FIRST_KEY" ]; then
        redis-cli -h localhost -p 6379 HGETALL "$FIRST_KEY" 2>/dev/null
    fi
fi
echo ""

# Test 3: Sustained rate test (10 requests per second)
echo "Test 3: Sustained rate test (10 requests/second for 5 seconds)"
echo "--------------------------------------------------------------"
echo "Making 50 requests at 10 req/sec (should all succeed)"
SUCCESS_SUSTAINED=0
RATE_LIMITED_SUSTAINED=0

for i in {1..50}; do
    RESPONSE=$(curl -s -w "\n%{http_code}" \
        -H "Authorization: Bearer $JWT_TOKEN" \
        "$GATEWAY_URL/api/trading/orders" 2>/dev/null)
    
    HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
    
    if [ "$HTTP_CODE" = "200" ] || [ "$HTTP_CODE" = "401" ] || [ "$HTTP_CODE" = "404" ]; then
        SUCCESS_SUSTAINED=$((SUCCESS_SUSTAINED + 1))
    elif [ "$HTTP_CODE" = "429" ]; then
        RATE_LIMITED_SUSTAINED=$((RATE_LIMITED_SUSTAINED + 1))
    fi
    
    # Wait 0.1 seconds between requests (10 req/sec)
    sleep 0.1
    
    if [ $((i % 10)) -eq 0 ]; then
        echo "  Processed $i requests..."
    fi
done

echo ""
echo "Results:"
echo "  ✅ Successful requests: $SUCCESS_SUSTAINED"
echo "  🚫 Rate limited requests: $RATE_LIMITED_SUSTAINED"
echo ""

echo "📊 Summary"
echo "=========="
echo "If rate limiting is working correctly:"
echo "  - Test 1: Should allow ~20 requests, then rate limit the rest"
echo "  - Test 3: Should allow all 50 requests (10 req/sec sustained)"
echo ""
