#!/bin/bash

# FixLite Cluster Scaling Script

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

# Default values
INSTANCES=3
KAFKA_BROKERS=3
ENVIRONMENT="docker"

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -i|--instances)
            INSTANCES="$2"
            shift 2
            ;;
        -k|--kafka-brokers)
            KAFKA_BROKERS="$2"
            shift 2
            ;;
        -e|--environment)
            ENVIRONMENT="$2"
            shift 2
            ;;
        -h|--help)
            echo "Usage: $0 [OPTIONS]"
            echo "Options:"
            echo "  -i, --instances NUM      Number of FixLite gateway instances (default: 3)"
            echo "  -k, --kafka-brokers NUM  Number of Kafka brokers (default: 3)"
            echo "  -e, --environment ENV    Environment: docker|kubernetes (default: docker)"
            echo "  -h, --help              Show this help message"
            exit 0
            ;;
        *)
            echo "Unknown option $1"
            exit 1
            ;;
    esac
done

echo "🚀 Scaling FixLite Cluster"
echo "Environment: $ENVIRONMENT"
echo "Gateway Instances: $INSTANCES"
echo "Kafka Brokers: $KAFKA_BROKERS"
echo ""

cd "$PROJECT_DIR"

case $ENVIRONMENT in
    docker)
        echo "📦 Building Docker images..."
        docker-compose -f docker-compose-cluster.yml build
        
        echo "🔧 Scaling services..."
        docker-compose -f docker-compose-cluster.yml up -d --scale fixlite-gw-1=1 --scale fixlite-gw-2=1 --scale fixlite-gw-3=1
        
        # Wait for services to be ready
        echo "⏳ Waiting for services to be ready..."
        sleep 30
        
        echo "✅ Cluster scaled successfully!"
        echo ""
        echo "Services:"
        echo "  Load Balancer: http://localhost:9878 (FIX)"
        echo "  Kafka UI: http://localhost:8080"
        echo "  Metrics: http://localhost:8081"
        echo ""
        echo "Direct Gateway Access:"
        echo "  Gateway 1: localhost:9871"
        echo "  Gateway 2: localhost:9872" 
        echo "  Gateway 3: localhost:9873"
        ;;
        
    kubernetes)
        echo "☸️ Deploying to Kubernetes..."
        
        # Apply Kubernetes manifests
        kubectl apply -f kubernetes/
        
        # Scale deployment
        kubectl scale deployment fixlite-gateway --replicas=$INSTANCES
        
        echo "⏳ Waiting for pods to be ready..."
        kubectl wait --for=condition=ready pod -l app=fixlite-gateway --timeout=300s
        
        echo "✅ Kubernetes deployment scaled successfully!"
        echo ""
        echo "Services:"
        kubectl get services
        echo ""
        echo "Pods:"
        kubectl get pods -l app=fixlite-gateway
        ;;
        
    *)
        echo "❌ Unknown environment: $ENVIRONMENT"
        echo "Supported environments: docker, kubernetes"
        exit 1
        ;;
esac

echo ""
echo "🔍 Cluster Status:"
case $ENVIRONMENT in
    docker)
        docker-compose -f docker-compose-cluster.yml ps
        ;;
    kubernetes)
        kubectl get pods,services -l app=fixlite-gateway
        ;;
esac
