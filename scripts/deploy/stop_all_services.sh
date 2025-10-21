#!/bin/bash

# Script para parar todos os serviços do Protocolo de Duelo
echo "Parando todos os serviços do Protocolo de Duelo..."

# Parar processos Java específicos do projeto
GATEWAY_PID=$(pgrep -f "dueling-gateway-1.0-SNAPSHOT.jar")
if [ -n "$GATEWAY_PID" ]; then
    echo "Parando Gateway (PID: $GATEWAY_PID)..."
    kill -TERM $GATEWAY_PID 2>/dev/null
    # Esperar um pouco antes de forçar a parada
    sleep 3
    # Verificar se o processo ainda está rodando e matar com -9 se necessário
    if kill -0 $GATEWAY_PID 2>/dev/null; then
        echo "Gateway ainda rodando, forçando parada..."
        kill -9 $GATEWAY_PID 2>/dev/null
    fi
fi

SERVER_PID=$(pgrep -f "dueling-server-1.0-SNAPSHOT.jar")
if [ -n "$SERVER_PID" ]; then
    echo "Parando Servidor (PID: $SERVER_PID)..."
    kill -TERM $SERVER_PID 2>/dev/null
    # Esperar um pouco antes de forçar a parada
    sleep 3
    # Verificar se o processo ainda está rodando e matar com -9 se necessário
    if kill -0 $SERVER_PID 2>/dev/null; then
        echo "Servidor ainda rodando, forçando parada..."
        kill -9 $SERVER_PID 2>/dev/null
    fi
fi

CLIENT_PID=$(pgrep -f "dueling-client-1.0-SNAPSHOT.jar")
if [ -n "$CLIENT_PID" ]; then
    echo "Parando Cliente (PID: $CLIENT_PID)..."
    kill -TERM $CLIENT_PID 2>/dev/null
    # Esperar um pouco antes de forçar a parada
    sleep 3
    # Verificar se o processo ainda está rodando e matar com -9 se necessário
    if kill -0 $CLIENT_PID 2>/dev/null; then
        echo "Cliente ainda rodando, forçando parada..."
        kill -9 $CLIENT_PID 2>/dev/null
    fi
fi

# Parar e remover containers Docker completamente
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$(dirname "$SCRIPT_DIR")")"
cd "$PROJECT_DIR/docker"
echo "Parando containers Docker..."
docker compose down --remove-orphans -v --timeout 10 2>/dev/null || true

# Remover quaisquer containers do projeto que possam ter ficado
echo "Removendo containers órfãos do projeto..."
# Parar containers específicos relacionados ao projeto
docker stop $(docker ps -aq --filter "name=nginx-gateway" --filter "name=server-[0-9]" --filter "name=client-[0-9]" --filter "name=redis-master" --filter "name=redis-slave" --filter "name=redis-sentinel-[0-9]" --filter "name=postgres" --filter "name=prometheus" --filter "name=grafana" 2>/dev/null) 2>/dev/null || true

# Remover containers específicos
docker rm $(docker ps -aq --filter "name=nginx-gateway" --filter "name=server-[0-9]" --filter "name=client-[0-9]" --filter "name=redis-master" --filter "name=redis-slave" --filter "name=redis-sentinel-[0-9]" --filter "name=postgres" --filter "name=prometheus" --filter "name=grafana" 2>/dev/null) 2>/dev/null || true

# Esperar um momento para garantir que todas as mudanças tenham efeito
sleep 2

# Limpar redes e volumes específicos do projeto, se necessário
docker network prune -f 2>/dev/null || true
docker volume prune -f 2>/dev/null || true

echo "Todos os serviços do Protocolo de Duelo foram parados e containers removidos."