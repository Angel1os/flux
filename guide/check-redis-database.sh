#!/bin/bash

echo "🔍 Checking Redis Databases for Rate Limit Keys"
echo "==============================================="
echo ""

# Redis has 16 databases (0-15), check each one
for db in {0..15}; do
    echo "Checking database $db:"
    KEYS=$(redis-cli -n $db KEYS "request_rate_limiter.*" 2>/dev/null)
    if [ -n "$KEYS" ]; then
        echo "   ✅ Found keys in database $db:"
        echo "$KEYS" | sed 's/^/      /'
        echo ""
        echo "   Sample key data:"
        FIRST_KEY=$(echo "$KEYS" | head -1)
        redis-cli -n $db HGETALL "$FIRST_KEY" 2>/dev/null | sed 's/^/      /'
        echo ""
    else
        echo "   ⚠️  No rate limit keys in database $db"
    fi
done

echo ""
echo "💡 Default database is 0. Gateway might be using a different database."
