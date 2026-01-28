#!/bin/bash
# Helper script to load .env file and start Market Data Service

# Load .env file if it exists
if [ -f .env ]; then
    export $(cat .env | grep -v '^#' | xargs)
    echo "✅ Loaded environment variables from .env file"
else
    echo "⚠️  No .env file found. Using defaults from application.yml"
fi

# Start Market Data Service
cd market-data-service
mvn spring-boot:run
