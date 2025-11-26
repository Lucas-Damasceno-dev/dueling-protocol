#!/bin/bash

# Cenário de Teste: Reproduz o problema original e demonstra a correção
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
echo "║         CENÁRIO DE TESTE: ALTA DISPONIBILIDADE                ║"
echo "║   Reproduz o problema original e valida a correção             ║"
echo "╚════════════════════════════════════════════════════════════════╝"
echo -e "${NC}"
echo ""

echo -e "${BLUE}Cenário Original (PROBLEMA):${NC}"
echo "1. Sistema iniciado (todos os serviços)"
echo "2. Um servidor adicional foi iniciado"
echo "3. Dois clientes foram iniciados"
echo "4. Servidor líder era server-4"
echo "5. Servidor adicional foi parado → Clientes OK ✅"
echo "6. Server-1 foi parado → ${RED}Clientes pararam de funcionar ❌${NC}"
echo ""
echo -e "${GREEN}Solução Implementada:${NC}"
echo "- NGINX com upstream pool (load balancing)"
echo "- Failover automático para servidores remanescentes"
echo "- Health checks passivos com retry"
echo ""
read -p "Pressione Enter para iniciar o teste..."
echo ""

# Step 1: Check system
echo -e "${YELLOW}═══ STEP 1: Verificando Sistema ═══${NC}"
SERVERS_UP=$(docker ps --filter "name=server-" --filter "status=running" | grep -c "server-")
echo "Servidores ativos: $SERVERS_UP"

if [ "$SERVERS_UP" -lt 2 ]; then
    echo -e "${RED}❌ Servidores insuficientes. Execute: ./menu.sh → 50${NC}"
    exit 1
fi

for i in 1 2 3 4; do
    if docker ps | grep -q "server-$i"; then
        STATUS=$(docker exec nginx-gateway curl -sf http://server-$i:8080/actuator/health 2>/dev/null | grep -o "UP" || echo "DOWN")
        if [ "$STATUS" = "UP" ]; then
            echo "  ✅ server-$i: HEALTHY"
        else
            echo "  ⚠️  server-$i: NOT READY"
        fi
    fi
done
echo ""

# Step 2: Test API before any failure
echo -e "${YELLOW}═══ STEP 2: Testando API (Estado Inicial) ═══${NC}"
for i in {1..3}; do
    RESPONSE=$(curl -sf http://localhost:8080/actuator/health 2>/dev/null)
    if echo "$RESPONSE" | grep -q "UP"; then
        echo "  ✅ Requisição $i: Sucesso"
    else
        echo "  ❌ Requisição $i: Falha"
    fi
    sleep 0.5
done
echo ""

# Step 3: Stop server-1 (simulating the original problem)
echo -e "${YELLOW}═══ STEP 3: Parando server-1 (Reproduzindo Problema) ═══${NC}"
echo "No sistema antigo, isso causaria falha total dos clientes..."
docker stop server-1 > /dev/null 2>&1
echo "  ⚠️  server-1 PARADO"
echo ""

# Wait for NGINX to detect failure
echo "Aguardando NGINX detectar falha (10 segundos)..."
for i in {10..1}; do
    echo -ne "  Aguardando: $i segundos...\r"
    sleep 1
done
echo ""
echo ""

# Step 4: Test API after server-1 failure
echo -e "${YELLOW}═══ STEP 4: Testando API após falha do server-1 ═══${NC}"
echo "Com a correção, NGINX deve rotear para servers 2-4..."
echo ""

SUCCESSES=0
FAILURES=0

for i in {1..10}; do
    RESPONSE=$(curl -sf http://localhost:8080/actuator/health 2>/dev/null)
    if echo "$RESPONSE" | grep -q "UP"; then
        echo -e "  ${GREEN}✅ Requisição $i: Sucesso (roteada para servidor ativo)${NC}"
        SUCCESSES=$((SUCCESSES + 1))
    else
        echo -e "  ${RED}❌ Requisição $i: Falha${NC}"
        FAILURES=$((FAILURES + 1))
    fi
    sleep 0.3
done
echo ""

# Step 5: Analysis
echo -e "${YELLOW}═══ STEP 5: Análise de Resultados ═══${NC}"
echo "Requisições bem-sucedidas: $SUCCESSES/10"
echo "Requisições com falha: $FAILURES/10"
echo ""

if [ $SUCCESSES -eq 10 ]; then
    echo -e "${GREEN}${BOLD}╔════════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${GREEN}${BOLD}║                  ✅ CORREÇÃO VALIDADA!                        ║${NC}"
    echo -e "${GREEN}${BOLD}╚════════════════════════════════════════════════════════════════╝${NC}"
    echo ""
    echo -e "${GREEN}O sistema NGINX está funcionando corretamente!${NC}"
    echo ""
    echo "Comportamento:"
    echo "  • NGINX detectou falha do server-1"
    echo "  • Roteou automaticamente para servers 2-4"
    echo "  • Clientes continuam operando sem interrupção"
    echo "  • Failover foi transparente para os usuários"
    echo ""
    echo -e "${CYAN}Logs recentes do NGINX:${NC}"
    docker logs nginx-gateway --tail 10 2>&1 | grep -E "error|upstream"
elif [ $SUCCESSES -ge 8 ]; then
    echo -e "${YELLOW}${BOLD}╔════════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${YELLOW}${BOLD}║              ⚠️  CORREÇÃO PARCIALMENTE VALIDADA               ║${NC}"
    echo -e "${YELLOW}${BOLD}╚════════════════════════════════════════════════════════════════╝${NC}"
    echo ""
    echo "A maioria das requisições foi bem-sucedida ($SUCCESSES/10)"
    echo "Pode haver um breve período de transição durante failover"
else
    echo -e "${RED}${BOLD}╔════════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${RED}${BOLD}║                  ❌ FALHA NA VALIDAÇÃO                        ║${NC}"
    echo -e "${RED}${BOLD}╚════════════════════════════════════════════════════════════════╝${NC}"
    echo ""
    echo "O failover não está funcionando corretamente"
    echo "Verifique os logs do NGINX para detalhes"
fi

echo ""
echo -e "${BLUE}═══ Restaurando server-1 ═══${NC}"
docker start server-1 > /dev/null 2>&1
echo "  ✅ server-1 reiniciado"
echo ""
echo "Teste concluído!"
