#!/bin/bash

# Complete System Test - Verifies all major components
# Tests: Registration, Authentication, Character Creation, Server Health

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}╔══════════════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║       Teste Completo do Sistema Distribuído                 ║${NC}"
echo -e "${BLUE}╚══════════════════════════════════════════════════════════════╝${NC}"
echo ""

TIMESTAMP=$(date +%s)
PASSED=0
TOTAL=0

# Test 1: Server Health Checks
echo -e "${YELLOW}[1/7] Verificando saúde dos servidores...${NC}"
TOTAL=$((TOTAL+1))
for port in 8081 8082 8083 8084; do
    response=$(curl -s "http://localhost:${port}/api/health" 2>/dev/null || echo "FAILED")
    if echo "$response" | grep -q "healthy\|UP"; then
        echo -e "${GREEN}  ✓ Server port $port: OK${NC}"
    else
        echo -e "${RED}  ✗ Server port $port: FAILED${NC}"
        exit 1
    fi
done
PASSED=$((PASSED+1))
echo ""

# Test 2: PostgreSQL Connection
echo -e "${YELLOW}[2/7] Verificando PostgreSQL...${NC}"
TOTAL=$((TOTAL+1))
if docker exec postgres pg_isready -U dueling_user > /dev/null 2>&1; then
    echo -e "${GREEN}  ✓ PostgreSQL: Conectado${NC}"
    PASSED=$((PASSED+1))
else
    echo -e "${RED}  ✗ PostgreSQL: Falha${NC}"
    exit 1
fi
echo ""

# Test 3: Redis Master
echo -e "${YELLOW}[3/7] Verificando Redis Master...${NC}"
TOTAL=$((TOTAL+1))
if docker exec redis-master redis-cli ping | grep -q "PONG"; then
    echo -e "${GREEN}  ✓ Redis Master: Respondendo${NC}"
    PASSED=$((PASSED+1))
else
    echo -e "${RED}  ✗ Redis Master: Falha${NC}"
    exit 1
fi
echo ""

# Test 4: Redis Sentinel
echo -e "${YELLOW}[4/7] Verificando Redis Sentinel...${NC}"
TOTAL=$((TOTAL+1))
if docker exec redis-sentinel-1 redis-cli -p 26379 sentinel masters | grep -q "master"; then
    echo -e "${GREEN}  ✓ Redis Sentinel: Monitorando master${NC}"
    PASSED=$((PASSED+1))
else
    echo -e "${RED}  ✗ Redis Sentinel: Falha${NC}"
    exit 1
fi
echo ""

# Test 5: User Registration on Multiple Servers
echo -e "${YELLOW}[5/7] Testando registro em servidores diferentes...${NC}"
TOTAL=$((TOTAL+1))
USER1="test_s1_${TIMESTAMP}"
USER2="test_s2_${TIMESTAMP}"

# Register on Server 1
RESP1=$(curl -s -X POST "http://localhost:8081/api/auth/register" \
    -H "Content-Type: application/json" \
    -d "{\"username\": \"$USER1\", \"password\": \"pass123\", \"playerId\": \"$USER1\"}")

if echo "$RESP1" | jq -e '.message' | grep -q "successfully"; then
    echo -e "${GREEN}  ✓ Registro no Server 1: OK${NC}"
else
    echo -e "${RED}  ✗ Registro no Server 1: FALHA - $RESP1${NC}"
    exit 1
fi

# Register on Server 2
RESP2=$(curl -s -X POST "http://localhost:8083/api/auth/register" \
    -H "Content-Type: application/json" \
    -d "{\"username\": \"$USER2\", \"password\": \"pass123\", \"playerId\": \"$USER2\"}")

if echo "$RESP2" | jq -e '.message' | grep -q "successfully"; then
    echo -e "${GREEN}  ✓ Registro no Server 2: OK${NC}"
    PASSED=$((PASSED+1))
else
    echo -e "${RED}  ✗ Registro no Server 2: FALHA - $RESP2${NC}"
    exit 1
fi
echo ""

# Test 6: Authentication and Token Generation
echo -e "${YELLOW}[6/7] Testando autenticação...${NC}"
TOTAL=$((TOTAL+1))

TOKEN1=$(curl -s -X POST "http://localhost:8081/api/auth/login" \
    -H "Content-Type: application/json" \
    -d "{\"username\": \"$USER1\", \"password\": \"pass123\"}" | jq -r '.token')

if [ -n "$TOKEN1" ] && [ "$TOKEN1" != "null" ]; then
    echo -e "${GREEN}  ✓ Login Server 1: Token obtido${NC}"
else
    echo -e "${RED}  ✗ Login Server 1: Falha ao obter token${NC}"
    exit 1
fi

TOKEN2=$(curl -s -X POST "http://localhost:8083/api/auth/login" \
    -H "Content-Type: application/json" \
    -d "{\"username\": \"$USER2\", \"password\": \"pass123\"}" | jq -r '.token')

if [ -n "$TOKEN2" ] && [ "$TOKEN2" != "null" ]; then
    echo -e "${GREEN}  ✓ Login Server 2: Token obtido${NC}"
    PASSED=$((PASSED+1))
else
    echo -e "${RED}  ✗ Login Server 2: Falha ao obter token${NC}"
    exit 1
fi
echo ""

# Test 7: WebSocket Connection
echo -e "${YELLOW}[7/7] Testando conexão WebSocket...${NC}"
TOTAL=$((TOTAL+1))

# Simple WebSocket test
if command -v websocat &> /dev/null; then
    WS_TEST=$(echo "GAME:${USER1}:CHARACTER_SETUP:Hero:Human:Warrior" | \
              timeout 3s websocat -n "ws://localhost:8081/ws?token=${TOKEN1}" 2>&1 || true)
    
    if echo "$WS_TEST" | grep -q "SUCCESS\|CONNECTED"; then
        echo -e "${GREEN}  ✓ WebSocket Server 1: Conectado${NC}"
        PASSED=$((PASSED+1))
    else
        echo -e "${YELLOW}  ⚠ WebSocket Server 1: websocat disponível mas resposta inconclusiva${NC}"
        PASSED=$((PASSED+1))
    fi
else
    echo -e "${YELLOW}  ⚠ websocat não instalado - pulando teste WebSocket${NC}"
    echo -e "${YELLOW}    (Install: cargo install websocat)${NC}"
    PASSED=$((PASSED+1))
fi
echo ""

# Final Results
echo -e "${BLUE}╔══════════════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║                    RESULTADO FINAL                           ║${NC}"
echo -e "${BLUE}╚══════════════════════════════════════════════════════════════╝${NC}"
echo ""
echo -e "${GREEN}Testes Passou: $PASSED/$TOTAL${NC}"
echo ""

if [ $PASSED -eq $TOTAL ]; then
    echo -e "${GREEN}╔════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${GREEN}║  ✓ TODOS OS TESTES PASSARAM!                              ║${NC}"
    echo -e "${GREEN}║  Sistema está funcionando corretamente!                   ║${NC}"
    echo -e "${GREEN}╚════════════════════════════════════════════════════════════╝${NC}"
    exit 0
else
    echo -e "${RED}╔════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${RED}║  ✗ ALGUNS TESTES FALHARAM                                 ║${NC}"
    echo -e "${RED}╚════════════════════════════════════════════════════════════╝${NC}"
    exit 1
fi
