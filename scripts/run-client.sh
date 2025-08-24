#!/bin/bash

# Run script for FixLite Client

set -e

echo "🔌 Starting FixLite Client..."

# Check if gateway is running
echo "🔍 Checking if gateway is running on port 9878..."
if ! nc -z localhost 9878 2>/dev/null; then
    echo "❌ Gateway not running on port 9878"
    echo "Please start the gateway first:"
    echo "  java -jar target/fixlite-gateway-1.0.0-SNAPSHOT.jar"
    echo "  or"
    echo "  docker-compose up"
    exit 1
fi

echo "✅ Gateway is running"
echo "🚀 Starting client..."

# Run the client
java -cp target/fixlite-gateway-1.0.0-SNAPSHOT.jar com.fixlite.client.FixClient
