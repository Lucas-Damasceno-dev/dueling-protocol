#!/bin/bash

# Script para iniciar tudo necessário para teste de troca cross-server

set -e

BLUE='\033[0;34m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${BLUE}╔══════════════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║    Iniciando Ambiente para Teste Cross-Server Trade         ║${NC}"
echo -e "${BLUE}╚══════════════════════════════════════════════════════════════╝${NC}"
echo ""

# Parar serviços anteriores
echo -e "${YELLOW}1. Parando serviços anteriores...${NC}"
./scripts/deploy/stop_all_services.sh 2>/dev/null || true
pkill -f "dueling-server" 2>/dev/null || true

# Iniciar infraestrutura
echo -e "${YELLOW}2. Iniciando PostgreSQL e Redis...${NC}"
cd docker
docker compose up postgres redis-master -d
cd ..

# Aguardar inicialização
echo -e "${YELLOW}3. Aguardando inicialização da infraestrutura (10s)...${NC}"
sleep 10

echo -e "${GREEN}✓ Infraestrutura pronta!${NC}"
echo ""
echo -e "${BLUE}═══ Próximos passos ═══${NC}"
echo ""
echo -e "${YELLOW}Abra 2 novos terminais e execute:${NC}"
echo ""
echo -e "${GREEN}Terminal 1:${NC}"
echo "  cd $(pwd)"
echo "  ./start_server1.sh"
echo ""
echo -e "${GREEN}Terminal 2:${NC}"
echo "  cd $(pwd)"
echo "  ./start_server2.sh"
echo ""
echo -e "${YELLOW}Aguarde os servidores iniciarem (~30s) e então execute:${NC}"
echo ""
echo -e "${GREEN}Terminal 3:${NC}"
echo "  cd $(pwd)"
echo "  ./test_cross_server_trade.sh"
echo ""
echo -e "${BLUE}═══ Monitorar logs (opcional) ═══${NC}"
echo ""
echo -e "${GREEN}Terminal 4:${NC}"
echo "  tail -f dueling-server/logs/application.log | grep --color=always \"\\[TRADE\""
echo ""
