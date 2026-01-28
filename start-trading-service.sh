#!/bin/bash
# Helper script to load .env file and start Trading Service

# Load .env file if it exists
if [ -f .env ]; then
    export $(cat .env | grep -v '^#' | xargs)
    echo "✅ Loaded environment variables from .env file"
else
    echo "⚠️  No .env file found. Using defaults from application.yml"
fi

# Start Trading Service
cd trading-service
mvn spring-boot:run
