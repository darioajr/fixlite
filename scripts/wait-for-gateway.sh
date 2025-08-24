#!/bin/bash

# Script para aguardar o FixLite Gateway estar pronto antes de iniciar clientes de teste

GATEWAY_HOST=${GATEWAY_HOST:-localhost}
GATEWAY_PORT=${GATEWAY_PORT:-9878}
MAX_ATTEMPTS=30
ATTEMPT=0

echo "Aguardando FixLite Gateway em $GATEWAY_HOST:$GATEWAY_PORT..."

while [ $ATTEMPT -lt $MAX_ATTEMPTS ]; do
    if nc -z $GATEWAY_HOST $GATEWAY_PORT; then
        echo "Gateway está pronto! Iniciando cliente..."
        break
    fi
    
    ATTEMPT=$((ATTEMPT + 1))
    echo "Tentativa $ATTEMPT/$MAX_ATTEMPTS - Gateway não está pronto ainda..."
    sleep 2
done

if [ $ATTEMPT -eq $MAX_ATTEMPTS ]; then
    echo "ERRO: Gateway não ficou pronto após $MAX_ATTEMPTS tentativas"
    exit 1
fi

# Executar comando passado como argumento
exec "$@"
