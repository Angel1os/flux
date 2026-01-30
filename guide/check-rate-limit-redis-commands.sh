#!/bin/bash

echo "🔍 Monitoring Redis Commands for Rate Limiting"
echo "=============================================="
echo ""
echo "This will monitor Redis commands in real-time."
echo "Make a request to the gateway while this is running."
echo ""
echo "Press Ctrl+C to stop monitoring"
echo ""

# Monitor Redis commands using MONITOR (be careful - this is verbose!)
redis-cli MONITOR 2>/dev/null | grep -i "rate\|limiter\|eval\|script" --line-buffered
