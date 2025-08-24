#!/bin/bash

# Build script for FixLite Gateway

set -e

echo "🚀 Building FixLite Gateway..."

# Clean and compile
echo "📦 Cleaning and compiling..."
mvn clean compile

# Run tests
echo "🧪 Running tests..."
mvn test

# Package JAR
echo "📦 Creating JAR package..."
mvn package

# Build native image (optional)
if [ "$1" = "native" ]; then
    echo "🏗️ Building native image..."
    mvn package -Pnative -DskipTests
    echo "✅ Native image built: target/fixlite-gateway"
fi

echo "✅ Build completed successfully!"
echo ""
echo "To run the gateway:"
echo "  Java JAR: java -jar target/fixlite-gateway-1.0.0-SNAPSHOT.jar"
if [ "$1" = "native" ]; then
    echo "  Native:   ./target/fixlite-gateway"
fi
echo ""
echo "To run with Docker:"
echo "  docker-compose up --build"
