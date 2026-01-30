#!/bin/bash

echo "🔍 Quick Rate Limit Diagnostic"
echo "=============================="
echo ""

# Check Redis
echo "1. Testing Redis connection..."
if redis-cli ping > /dev/null 2>&1; then
    echo "   ✅ Redis is running"
else
    echo "   ❌ Redis is NOT running - start it with: redis-server"
    exit 1
fi

# Check current keys
echo ""
echo "2. Current rate limit keys:"
KEYS=$(redis-cli KEYS "request_rate_limiter.*" 2>/dev/null)
if [ -z "$KEYS" ]; then
    echo "   ⚠️  No rate limit keys found"
else
    echo "   Found keys:"
    echo "$KEYS" | sed 's/^/      /'
fi

# Check all keys
echo ""
echo "3. Total keys in Redis:"
TOTAL=$(redis-cli DBSIZE 2>/dev/null)
echo "   Total: $TOTAL keys"

# Show sample keys
echo ""
echo "4. Sample keys (first 10):"
redis-cli KEYS "*" 2>/dev/null | head -10 | sed 's/^/      /'

echo ""
echo "📝 Next steps:"
echo "   1. Make a request: curl -H 'Authorization: Bearer YOUR_JWT' http://localhost:8090/api/v1/trading/orders"
echo "   2. Run this script again to check if keys appear"
echo "   3. Check gateway logs for errors"
