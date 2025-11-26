#!/bin/bash

# Script para testar reconexão automática ao Redis após failover
SCRIPT_DIR=$(cd "$(dirname -- "$0")" && cd .. && pwd)

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
BOLD='\033[1m'
NC='\033[0m'

echo -e "${CYAN}${BOLD}"
echo "╔════════════════════════════════════════════════════════════════╗"
echo "║    TESTE: REDIS SENTINEL FAILOVER COM RECONEXÃO AUTOMÁTICA    ║"
echo "╚════════════════════════════════════════════════════════════════╝"
echo -e "${NC}"
echo ""

# Step 1: Verificar sistema
echo -e "${YELLOW}═══ STEP 1: Verificando Sistema ═══${NC}"
if ! docker ps | grep -q "server-"; then
    echo -e "${RED}❌ Servidores não estão rodando${NC}"
    exit 1
fi

echo "✅ Servidores ativos"
echo ""

# Step 2: Identificar master atual
echo -e "${YELLOW}═══ STEP 2: Identificando Redis Master ═══${NC}"
CURRENT_MASTER=$(docker exec redis-sentinel-1 redis-cli -p 26379 SENTINEL get-master-addr-by-name mymaster 2>/dev/null | tr '\n' ':' | sed 's/:$//')
echo "Master atual: $CURRENT_MASTER"

# Identificar qual container é o master
if docker exec redis-master redis-cli ROLE 2>/dev/null | grep -q "master"; then
    MASTER_CONTAINER="redis-master"
    echo "Container: redis-master"
elif docker exec redis-slave redis-cli ROLE 2>/dev/null | grep -q "master"; then
    MASTER_CONTAINER="redis-slave"
    echo "Container: redis-slave"
else
    echo -e "${RED}❌ Não foi possível identificar o master${NC}"
    exit 1
fi
echo ""

# Step 3: Testar API antes da falha
echo -e "${YELLOW}═══ STEP 3: Testando API (antes da falha) ═══${NC}"
API_TEST=$(curl -sf http://localhost:8080/actuator/health 2>/dev/null)
if echo "$API_TEST" | grep -q "UP"; then
    echo "✅ API respondendo normalmente"
else
    echo -e "${RED}❌ API não está respondendo${NC}"
    exit 1
fi
echo ""

# Step 4: Simular falha do master
echo -e "${YELLOW}═══ STEP 4: Simulando Falha do Redis Master ═══${NC}"
echo "Parando container: $MASTER_CONTAINER"
docker stop $MASTER_CONTAINER > /dev/null 2>&1
echo "⚠️  Master parado"
echo ""

# Step 5: Aguardar failover
echo -e "${YELLOW}═══ STEP 5: Aguardando Failover do Sentinel (~40s) ═══${NC}"
for i in {40..1}; do
    echo -ne "  Aguardando: $i segundos...\r"
    sleep 1
done
echo ""
echo ""

# Step 6: Verificar novo master
echo -e "${YELLOW}═══ STEP 6: Verificando Novo Master ═══${NC}"
NEW_MASTER=$(docker exec redis-sentinel-1 redis-cli -p 26379 SENTINEL get-master-addr-by-name mymaster 2>/dev/null | tr '\n' ':' | sed 's/:$//')
echo "Novo master: $NEW_MASTER"

if [ "$NEW_MASTER" != "$CURRENT_MASTER" ]; then
    echo -e "${GREEN}✅ Failover executado com sucesso!${NC}"
else
    echo -e "${RED}❌ Failover não ocorreu${NC}"
    exit 1
fi
echo ""

# Step 7: Aguardar reconexão dos servidores
echo -e "${YELLOW}═══ STEP 7: Aguardando Reconexão dos Servidores (20s) ═══${NC}"
for i in {20..1}; do
    echo -ne "  Aguardando: $i segundos...\r"
    sleep 1
done
echo ""
echo ""

# Step 8: Testar API após failover
echo -e "${YELLOW}═══ STEP 8: Testando API (após failover) ═══${NC}"
SUCCESSES=0
FAILURES=0

for i in {1..5}; do
    API_TEST=$(curl -sf http://localhost:8080/actuator/health 2>/dev/null)
    if echo "$API_TEST" | grep -q "UP"; then
        echo -e "  ${GREEN}✅ Teste $i: API respondendo (reconexão OK)${NC}"
        SUCCESSES=$((SUCCESSES + 1))
    else
        echo -e "  ${RED}❌ Teste $i: API não respondendo${NC}"
        FAILURES=$((FAILURES + 1))
    fi
    sleep 1
done
echo ""

# Step 9: Testar funcionalidade (simulação de compra)
echo -e "${YELLOW}═══ STEP 9: Testando Funcionalidade (Redis) ═══${NC}"
echo "Verificando se Redis está funcionando..."

# Tentar fazer uma operação que usa Redis
TEST_RESULT=$(docker exec server-1 curl -sf http://localhost:8080/actuator/health 2>/dev/null)
if echo "$TEST_RESULT" | grep -q "UP"; then
    echo -e "${GREEN}✅ Servidor conseguiu acessar Redis${NC}"
else
    echo -e "${RED}❌ Servidor não conseguiu acessar Redis${NC}"
fi
echo ""

# Step 10: Análise final
echo -e "${YELLOW}═══ STEP 10: Análise de Resultados ═══${NC}"
echo "Testes de API bem-sucedidos: $SUCCESSES/5"
echo "Testes de API com falha: $FAILURES/5"
echo ""

if [ $SUCCESSES -eq 5 ]; then
    echo -e "${GREEN}${BOLD}╔════════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${GREEN}${BOLD}║           ✅ RECONEXÃO AUTOMÁTICA FUNCIONANDO!                ║${NC}"
    echo -e "${GREEN}${BOLD}╚════════════════════════════════════════════════════════════════╝${NC}"
    echo ""
    echo "Comportamento:"
    echo "  • Redis Sentinel promoveu novo master"
    echo "  • Servidores se reconectaram automaticamente"
    echo "  • API continuou operacional após breve período"
    echo "  • Sistema resiliente a falhas do Redis"
elif [ $SUCCESSES -ge 3 ]; then
    echo -e "${YELLOW}${BOLD}╔════════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${YELLOW}${BOLD}║         ⚠️  RECONEXÃO PARCIAL (melhorias necessárias)        ║${NC}"
    echo -e "${YELLOW}${BOLD}╚════════════════════════════════════════════════════════════════╝${NC}"
    echo ""
    echo "A maioria dos testes passou, mas há instabilidade"
else
    echo -e "${RED}${BOLD}╔════════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${RED}${BOLD}║              ❌ RECONEXÃO AUTOMÁTICA FALHOU                   ║${NC}"
    echo -e "${RED}${BOLD}╚════════════════════════════════════════════════════════════════╝${NC}"
    echo ""
    echo "Servidores não conseguiram se reconectar ao novo master"
fi

echo ""
echo -e "${BLUE}═══ Restaurando Configuração Original ═══${NC}"
docker start $MASTER_CONTAINER > /dev/null 2>&1
echo "✅ Container $MASTER_CONTAINER reiniciado"
echo ""
echo "Aguardando Sentinel reconfigurar (20 segundos)..."
sleep 20
echo "Teste concluído!"
