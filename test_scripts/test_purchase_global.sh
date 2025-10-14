#!/bin/bash

# Script para testar compra de cartas com coordenação global
echo "=== Teste de Compra de Cartas com Coordenação Global ==="

# Verificar se o ambiente está rodando
if ! docker compose -f docker/docker-compose.yml ps | grep -q "server-1.*healthy"; then
    echo "ERRO: Ambiente não está rodando. Inicie os servidores primeiro."
    exit 1
fi

echo "Servidores ativos:"
docker compose -f docker/docker-compose.yml ps | grep -E "server-[1-4]|gateway"

# Teste de compra de cartas com recursos globais
echo "Iniciando teste de compra de cartas com coordenação global..."
echo "1. Verificando locks distribuídos no Redis"
docker compose -f docker/docker-compose.yml exec redis redis-cli keys "*lock*"

echo "2. Verificando logs de compra nos servidores"
docker compose -f docker/docker-compose.yml logs server-1 | grep -i "purchase\|buy\|lock.*acquired\|lock.*released" | tail -10

docker compose -f docker/docker-compose.yml logs server-2 | grep -i "purchase\|buy\|lock.*acquired\|lock.*released" | tail -10

docker compose -f docker/docker-compose.yml logs server-3 | grep -i "purchase\|buy\|lock.*acquired\|lock.*released" | tail -10

docker compose -f docker/docker-compose.yml logs server-4 | grep -i "purchase\|buy\|lock.*acquired\|lock.*released" | tail -10

echo "3. Verificando se há concorrência controlada nas compras"
docker compose -f docker/docker-compose.yml logs server-1 | grep -i "SERVER_BUSY\|INSUFFICIENT_FUNDS\|OUT_OF_STOCK" | tail -5

echo "Teste de compra de cartas com coordenação global concluído."