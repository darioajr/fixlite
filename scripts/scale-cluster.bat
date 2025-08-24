@echo off
REM FixLite Cluster Scaling Script

setlocal enabledelayedexpansion

REM Get script directory
set SCRIPT_DIR=%~dp0
set PROJECT_DIR=%SCRIPT_DIR%..

REM Default values
set INSTANCES=3
set KAFKA_BROKERS=3
set ENVIRONMENT=docker

REM Parse command line arguments
:parse_args
if "%1"=="" goto end_parse
if "%1"=="-i" (
    set INSTANCES=%2
    shift
    shift
    goto parse_args
)
if "%1"=="--instances" (
    set INSTANCES=%2
    shift
    shift
    goto parse_args
)
if "%1"=="-k" (
    set KAFKA_BROKERS=%2
    shift
    shift
    goto parse_args
)
if "%1"=="--kafka-brokers" (
    set KAFKA_BROKERS=%2
    shift
    shift
    goto parse_args
)
if "%1"=="-e" (
    set ENVIRONMENT=%2
    shift
    shift
    goto parse_args
)
if "%1"=="--environment" (
    set ENVIRONMENT=%2
    shift
    shift
    goto parse_args
)
if "%1"=="-h" goto show_help
if "%1"=="--help" goto show_help
echo Unknown option %1
exit /b 1

:show_help
echo Usage: %0 [OPTIONS]
echo Options:
echo   -i, --instances NUM      Number of FixLite gateway instances (default: 3)
echo   -k, --kafka-brokers NUM  Number of Kafka brokers (default: 3)
echo   -e, --environment ENV    Environment: docker^|kubernetes (default: docker)
echo   -h, --help              Show this help message
exit /b 0

:end_parse

echo 🚀 Scaling FixLite Cluster
echo Environment: %ENVIRONMENT%
echo Gateway Instances: %INSTANCES%
echo Kafka Brokers: %KAFKA_BROKERS%
echo.

cd /d "%PROJECT_DIR%"

if "%ENVIRONMENT%"=="docker" goto docker_env
if "%ENVIRONMENT%"=="kubernetes" goto kubernetes_env
echo ❌ Unknown environment: %ENVIRONMENT%
echo Supported environments: docker, kubernetes
exit /b 1

:docker_env
echo 📦 Building Docker images...
docker-compose -f docker-compose-cluster.yml build
if %errorlevel% neq 0 (
    echo ❌ Docker build failed
    exit /b 1
)

echo 🔧 Scaling services...
docker-compose -f docker-compose-cluster.yml up -d --scale fixlite-gw-1=1 --scale fixlite-gw-2=1 --scale fixlite-gw-3=1
if %errorlevel% neq 0 (
    echo ❌ Docker scaling failed
    exit /b 1
)

REM Wait for services to be ready
echo ⏳ Waiting for services to be ready...
timeout /t 30 /nobreak >nul

echo ✅ Cluster scaled successfully!
echo.
echo Services:
echo   Load Balancer: http://localhost:9878 (FIX)
echo   Kafka UI: http://localhost:8080
echo   Metrics: http://localhost:8081
echo.
echo Direct Gateway Access:
echo   Gateway 1: localhost:9871
echo   Gateway 2: localhost:9872
echo   Gateway 3: localhost:9873
goto show_status

:kubernetes_env
echo ☸️ Deploying to Kubernetes...

REM Apply Kubernetes manifests
kubectl apply -f kubernetes\
if %errorlevel% neq 0 (
    echo ❌ Kubernetes apply failed
    exit /b 1
)

REM Scale deployment
kubectl scale deployment fixlite-gateway --replicas=%INSTANCES%
if %errorlevel% neq 0 (
    echo ❌ Kubernetes scaling failed
    exit /b 1
)

echo ⏳ Waiting for pods to be ready...
kubectl wait --for=condition=ready pod -l app=fixlite-gateway --timeout=300s
if %errorlevel% neq 0 (
    echo ❌ Pods failed to become ready
    exit /b 1
)

echo ✅ Kubernetes deployment scaled successfully!
echo.
echo Services:
kubectl get services
echo.
echo Pods:
kubectl get pods -l app=fixlite-gateway
goto show_status

:show_status
echo.
echo 🔍 Cluster Status:
if "%ENVIRONMENT%"=="docker" (
    docker-compose -f docker-compose-cluster.yml ps
) else if "%ENVIRONMENT%"=="kubernetes" (
    kubectl get pods,services -l app=fixlite-gateway
)
