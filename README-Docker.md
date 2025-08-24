# FixLite Gateway - Docker Build Guide

Este guia explica como compilar e executar o FixLite Gateway usando Docker com duas abordagens diferentes: JVM padrão e GraalVM Native Image.

## 📋 Pré-requisitos

- Docker 20.10+
- Docker Compose 2.0+
- 4GB+ RAM disponível
- 10GB+ espaço em disco

## 🚀 Opções de Build

### 1. Build JVM Padrão (Recomendado)

**Vantagens:**
- ✅ Build mais rápido (~5-10 minutos)
- ✅ Compatível com qualquer JDK
- ✅ Melhor para desenvolvimento e debugging
- ✅ Suporte completo a reflection e bibliotecas

```bash
# Build apenas JVM
docker build -t fixlite-gateway:jvm .

# Executar com docker-compose
docker-compose -f docker-compose.build.yml up fixlite-jvm kafka kafka-ui kafka-init
```

### 2. Build GraalVM Native Image

**Vantagens:**
- ✅ Startup mais rápido (~2-5 segundos)
- ✅ Menor uso de memória (~50-100MB)
- ✅ Melhor para produção cloud-native
- ⚠️ Build mais lento (~15-30 minutos)

```bash
# Build native image
docker build -f Dockerfile.native -t fixlite-gateway:native .

# Executar com docker-compose
docker-compose -f docker-compose.build.yml up fixlite-native kafka kafka-ui kafka-init
```

### 3. Build Ambos (Comparação)

```bash
# Build e executar ambas as versões simultaneamente
docker-compose -f docker-compose.build.yml up
```

## 🔧 Comandos Úteis

### Build Individual

```bash
# JVM padrão
docker build -t fixlite-gateway:jvm .

# Native image
docker build -f Dockerfile.native -t fixlite-gateway:native .
```

### Executar Apenas o Gateway

```bash
# JVM (porta 9878)
docker run -p 9878:9878 fixlite-gateway:jvm

# Native (porta 9878)
docker run -p 9878:9878 fixlite-gateway:native
```

### Logs e Debugging

```bash
# Ver logs do gateway JVM
docker-compose -f docker-compose.build.yml logs -f fixlite-jvm

# Ver logs do gateway Native
docker-compose -f docker-compose.build.yml logs -f fixlite-native

# Ver logs do Kafka
docker-compose -f docker-compose.build.yml logs -f kafka
```

## 🌐 Endpoints e Portas

| Serviço | Porta | Descrição |
|---------|-------|-----------|
| FixLite JVM | 9878 | FIX Protocol Gateway (JVM) |
| FixLite Native | 9879 | FIX Protocol Gateway (Native) |
| Kafka UI | 8080 | Interface web do Kafka |
| Kafka Broker | 9092 | Kafka Bootstrap Server |
| Metrics JVM | 8081 | Métricas do gateway JVM |
| Metrics Native | 8082 | Métricas do gateway Native |

## 📊 Comparação de Performance

| Métrica | JVM | Native |
|---------|-----|--------|
| **Tempo de Build** | ~5-10 min | ~15-30 min |
| **Tempo de Startup** | ~10-20s | ~2-5s |
| **Uso de Memória** | ~200-500MB | ~50-100MB |
| **Throughput** | Alto | Alto |
| **Debugging** | Completo | Limitado |

## 🛠️ Troubleshooting

### Build Falha - JVM

```bash
# Limpar cache do Docker
docker system prune -a

# Build com logs detalhados
docker build --no-cache --progress=plain -t fixlite-gateway:jvm .
```

### Build Falha - Native

```bash
# Verificar se GraalVM está funcionando
docker run --rm ghcr.io/graalvm/graalvm-ce:ol8-java21-22.3.0 java -version

# Build com mais memória
docker build --memory=8g -f Dockerfile.native -t fixlite-gateway:native .
```

### Problemas de Conectividade

```bash
# Verificar se Kafka está rodando
docker-compose -f docker-compose.build.yml ps

# Testar conectividade com Kafka
docker-compose -f docker-compose.build.yml exec kafka kafka-topics --bootstrap-server localhost:9092 --list
```

## 🔍 Monitoramento

### Kafka UI
- Acesse: http://localhost:8080
- Monitore tópicos, mensagens e performance

### Health Checks
```bash
# Status dos containers
docker-compose -f docker-compose.build.yml ps

# Health check manual
curl http://localhost:9878/health  # JVM
curl http://localhost:9879/health  # Native
```

## 📝 Configuração Avançada

### Variáveis de Ambiente

```yaml
environment:
  # JVM Options (apenas para versão JVM)
  - JAVA_OPTS=-Xmx1g -Xms512m -XX:+UseG1GC
  
  # FixLite Configuration
  - FIXLITE_KAFKA_BOOTSTRAP_SERVERS=kafka:29092
  - FIXLITE_SERVER_GATEWAY_ID=CUSTOM-GW-ID
  - FIXLITE_LOG_LEVEL=INFO
```

### Volumes Personalizados

```yaml
volumes:
  - ./config:/app/config
  - ./logs:/app/logs
  - ./data:/app/data
```

## 🚀 Deploy em Produção

### Recomendações JVM
- Use para ambientes com debugging necessário
- Ideal para desenvolvimento e staging
- Configure heap size adequadamente

### Recomendações Native
- Use para produção cloud-native
- Ideal para containers efêmeros
- Melhor para auto-scaling

### Exemplo Kubernetes
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: fixlite-gateway
spec:
  replicas: 3
  selector:
    matchLabels:
      app: fixlite-gateway
  template:
    metadata:
      labels:
        app: fixlite-gateway
    spec:
      containers:
      - name: fixlite-gateway
        image: fixlite-gateway:native
        ports:
        - containerPort: 9878
        resources:
          requests:
            memory: "128Mi"
            cpu: "100m"
          limits:
            memory: "256Mi"
            cpu: "500m"
```
