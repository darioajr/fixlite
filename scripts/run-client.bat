@echo off
REM Run script for FixLite Client

setlocal enabledelayedexpansion

echo 🔌 Starting FixLite Client...

REM Check if gateway is running
echo 🔍 Checking if gateway is running on port 9878...
netstat -an | findstr ":9878" >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ Gateway not running on port 9878
    echo Please start the gateway first:
    echo   java -jar target\fixlite-gateway-1.0.0-SNAPSHOT.jar
    echo   or
    echo   docker-compose up
    exit /b 1
)

echo ✅ Gateway is running
echo 🚀 Starting client...

REM Run the client
java -cp target\fixlite-gateway-1.0.0-SNAPSHOT.jar com.fixlite.client.FixClient
