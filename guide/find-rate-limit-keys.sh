#!/bin/bash

echo "🔍 Finding Rate Limit Keys in Redis"
echo "===================================="
echo ""

# Check all possible key patterns
echo "1. Searching for rate limit keys..."
echo ""

# Pattern 1: Standard pattern
echo "   Pattern: request_rate_limiter.*"
KEYS1=$(redis-cli KEYS "request_rate_limiter.*" 2>/dev/null)
if [ -n "$KEYS1" ]; then
    echo "$KEYS1" | sed 's/^/      ✅ /'
else
    echo "      ⚠️  No keys found"
fi
echo ""

# Pattern 2: Check for IP-based keys
echo "   Pattern: request_rate_limiter.192.*"
KEYS2=$(redis-cli KEYS "request_rate_limiter.192.*" 2>/dev/null)
if [ -n "$KEYS2" ]; then
    echo "$KEYS2" | sed 's/^/      ✅ /'
else
    echo "      ⚠️  No IP-based keys found"
fi
echo ""

# Pattern 3: Check for hash code keys (old behavior)
echo "   Pattern: request_rate_limiter.-*"
KEYS3=$(redis-cli KEYS "request_rate_limiter.-*" 2>/dev/null)
if [ -n "$KEYS3" ]; then
    echo "$KEYS3" | sed 's/^/      ✅ /'
else
    echo "      ⚠️  No hash code keys found"
fi
echo ""

# Pattern 4: Check for UUID keys
echo "   Pattern: request_rate_limiter.*-*-*-*-*"
KEYS4=$(redis-cli KEYS "request_rate_limiter.*-*-*-*-*" 2>/dev/null)
if [ -n "$KEYS4" ]; then
    echo "$KEYS4" | sed 's/^/      ✅ /'
else
    echo "      ⚠️  No UUID keys found"
fi
echo ""

# Show ALL keys to see what's actually there
echo "2. All keys in Redis (first 20):"
ALL_KEYS=$(redis-cli KEYS "*" 2>/dev/null | head -20)
if [ -n "$ALL_KEYS" ]; then
    echo "$ALL_KEYS" | sed 's/^/      /'
else
    echo "      ⚠️  No keys in Redis"
fi
echo ""

# Check if keys contain "rate" or "limiter"
echo "3. Keys containing 'rate' or 'limiter':"
RATE_KEYS=$(redis-cli KEYS "*rate*" 2>/dev/null)
LIMITER_KEYS=$(redis-cli KEYS "*limiter*" 2>/dev/null)
if [ -n "$RATE_KEYS" ] || [ -n "$LIMITER_KEYS" ]; then
    [ -n "$RATE_KEYS" ] && echo "$RATE_KEYS" | sed 's/^/      ✅ /'
    [ -n "$LIMITER_KEYS" ] && echo "$LIMITER_KEYS" | sed 's/^/      ✅ /'
else
    echo "      ⚠️  No rate/limiter keys found"
fi
echo ""

# Check database size
echo "4. Total keys in Redis:"
TOTAL=$(redis-cli DBSIZE 2>/dev/null)
echo "      Total: $TOTAL keys"
echo ""

# If we found keys, show details
if [ -n "$KEYS1" ] || [ -n "$KEYS2" ] || [ -n "$KEYS3" ] || [ -n "$KEYS4" ]; then
    FIRST_KEY=$(echo -e "$KEYS1\n$KEYS2\n$KEYS3\n$KEYS4" | grep -v "^$" | head -1)
    if [ -n "$FIRST_KEY" ]; then
        echo "5. Details of first rate limit key:"
        echo "      Key: $FIRST_KEY"
        echo "      Data:"
        redis-cli HGETALL "$FIRST_KEY" 2>/dev/null | sed 's/^/         /'
    fi
fi

echo ""
echo "💡 Tip: Check gateway logs for 'Extracted user ID from JWT' to see what key is being used"
