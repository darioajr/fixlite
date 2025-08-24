@echo off
REM Build script for FixLite Gateway

setlocal enabledelayedexpansion

echo 🚀 Building FixLite Gateway...

REM Clean and compile
echo 📦 Cleaning and compiling...
call mvn clean compile
if %errorlevel% neq 0 (
    echo ❌ Compilation failed
    exit /b 1
)

REM Run tests
echo 🧪 Running tests...
call mvn test
if %errorlevel% neq 0 (
    echo ❌ Tests failed
    exit /b 1
)

REM Package JAR
echo 📦 Creating JAR package...
call mvn package
if %errorlevel% neq 0 (
    echo ❌ Packaging failed
    exit /b 1
)

REM Build native image (optional)
if "%1"=="native" (
    echo 🏗️ Building native image...
    call mvn package -Pnative -DskipTests
    if %errorlevel% neq 0 (
        echo ❌ Native image build failed
        exit /b 1
    )
    echo ✅ Native image built: target\fixlite-gateway.exe
)

echo ✅ Build completed successfully!
echo.
echo To run the gateway:
echo   Java JAR: java -jar target\fixlite-gateway-1.0.0-SNAPSHOT.jar
if "%1"=="native" (
    echo   Native:   target\fixlite-gateway.exe
)
echo.
echo To run with Docker:
echo   docker-compose up --build
