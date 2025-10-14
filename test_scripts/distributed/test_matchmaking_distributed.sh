#!/bin/bash

# Script para testar matchmaking distribuído entre servidores
echo "=== Teste de Matchmaking Distribuído ==="

# Verificar se o ambiente está rodando
if ! docker compose -f docker/docker-compose.yml ps | grep -q "server-1.*healthy"; then
    echo "Iniciando ambiente com 4 servidores..."
    docker compose -f docker/docker-compose.yml up postgres redis server-1 server-2 server-3 server-4 server-gateway --detach
    sleep 30
fi

echo "Servidores ativos:"
docker compose -f docker/docker-compose.yml ps | grep -E "server-[1-4]|gateway"

# Teste de matchmaking distribuído
echo "Iniciando teste de matchmaking distribuído..."
echo "1. Verificando comunicação entre servidores"
docker compose -f docker/docker-compose.yml logs server-1 | grep -i "register\|elect\|leader" | tail -10

echo "2. Verificando logs de matchmaking"
docker compose -f docker/docker-compose.yml logs server-1 | grep -i "match\|queue\|partner" | tail -10

# Verificar se servidores conseguem se comunicar entre si para matchmaking
echo "3. Verificando se server-1 pode encontrar parceiros remotos"
docker compose -f docker/docker-compose.yml logs server-1 | grep -i "remote\|find\|partner\|match" | tail -5

echo "Teste de matchmaking distribuído concluído."
