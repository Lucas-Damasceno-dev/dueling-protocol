#!/bin/bash

# Script para parar todos os serviços do Protocolo de Duelo
echo "Parando todos os serviços do Protocolo de Duelo..."

# Parar processos Java do gateway e servidor
GATEWAY_PID=$(pgrep -f "dueling-gateway-1.0-SNAPSHOT.jar")
if [ -n "$GATEWAY_PID" ]; then
    echo "Parando Gateway (PID: $GATEWAY_PID)..."
    kill -9 $GATEWAY_PID
fi

SERVER_PID=$(pgrep -f "dueling-server-1.0-SNAPSHOT.jar")
if [ -n "$SERVER_PID" ]; then
    echo "Parando Servidor (PID: $SERVER_PID)..."
    kill -9 $SERVER_PID
fi

# Parar e remover containers Docker completamente
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$(dirname "$SCRIPT_DIR")")"
cd "$PROJECT_DIR/docker"
echo "Parando containers Docker..."
docker compose down --remove-orphans -v --timeout 10 2>/dev/null || true

# Remover quaisquer containers do projeto que possam ter ficado
echo "Removendo containers órfãos do projeto..."
# Parar containers específicos
docker stop $(docker ps -aq --filter "name=redis" --filter "name=postgres" --filter "name=server" --filter "name=nginx" --filter "name=grafana" --filter "name=prometheus" --filter "name=client" 2>/dev/null) 2>/dev/null || true

# Remover containers específicos
docker rm $(docker ps -aq --filter "name=redis" --filter "name=postgres" --filter "name=server" --filter "name=nginx" --filter "name=grafana" --filter "name=prometheus" --filter "name=client" 2>/dev/null) 2>/dev/null || true

echo "Todos os serviços foram parados e containers removidos."