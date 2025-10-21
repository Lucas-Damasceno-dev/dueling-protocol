#!/bin/bash

# Script para testar visualização de logs do server-1
# Este script automatiza o processo que você estava fazendo manualmente

echo "=== Teste de Logs do Server-1 ==="
echo

# Cores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Verificar se o server-1 está rodando
if ! docker ps | grep -q "server-1"; then
    echo -e "${RED}Erro: server-1 não está rodando${NC}"
    echo "Por favor, execute primeiro: ./menu.sh -> opção 1"
    exit 1
fi

echo -e "${GREEN}Server-1 está rodando${NC}"
echo

# Mostrar informações sobre o container
echo -e "${BLUE}Informações do container server-1:${NC}"
docker ps --filter "name=server-1" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
echo

# Verificar o driver de logging do Docker
echo -e "${BLUE}Driver de logging do container:${NC}"
docker inspect server-1 --format='{{.HostConfig.LogConfig.Type}}'
echo

# Mostrar os últimos logs
echo -e "${BLUE}Últimos 50 logs do server-1:${NC}"
docker logs --tail 50 server-1
echo

echo -e "${YELLOW}Agora você pode:${NC}"
echo "1. Abrir outro terminal e executar: docker logs -f server-1"
echo "2. Executar: ./menu.sh -> opção 10 (Run Client)"
echo "3. Ver os logs em tempo real no outro terminal"
echo
echo -e "${YELLOW}Se ainda não ver os logs da aplicação:${NC}"
echo "1. Reconstrua a imagem: docker compose -f docker/docker-compose.yml build server-1"
echo "2. Reinicie o container: docker restart server-1"
echo "3. Verifique novamente os logs: docker logs -f server-1"
