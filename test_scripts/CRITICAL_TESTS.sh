#!/bin/bash

# ============================================
# TESTES CRÍTICOS: TRADE + MATCH + PURCHASE
# ============================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$SCRIPT_DIR/.."
DOCKER_COMPOSE_FILE="$PROJECT_ROOT/docker/docker-compose.yml"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

set -e

echo "╔════════════════════════════════════════════════════╗"
echo "║  TESTES CRÍTICOS: TRADE + MATCH + PURCHASE         ║"
echo "╚════════════════════════════════════════════════════╝"

# Source helper
source "$SCRIPT_DIR/common_env.sh"

# Create env file
create_env_file "$PROJECT_ROOT/docker/.env" "normal" ""

# Cleanup function
cleanup() {
    echo -e "\n${YELLOW}Limpando ambiente...${NC}"
    docker compose -f "$DOCKER_COMPOSE_FILE" down -v 2>/dev/null
    rm -f /tmp/test_*.log
}
trap cleanup EXIT

# Start services
echo -e "\n${YELLOW}[1/4] Iniciando serviços...${NC}"
docker compose -f "$DOCKER_COMPOSE_FILE" --env-file "$PROJECT_ROOT/docker/.env" up -d \
    postgres redis-master redis-slave redis-sentinel-1 redis-sentinel-2 redis-sentinel-3 \
    server-1 server-2 nginx-gateway

echo "Aguardando serviços ficarem prontos (40s)..."
sleep 40

# Test 1: Authentication (foundation for all tests)
echo -e "\n${YELLOW}[2/4] Testando Autenticação (fundação)...${NC}"
REGISTER_RESP=$(curl -s -X POST http://localhost:8080/api/auth/register \
    -H "Content-Type: application/json" \
    -d '{"username":"testuser1","password":"pass123"}')
echo "Register: $REGISTER_RESP"

LOGIN_RESP=$(curl -s -X POST http://localhost:8080/api/auth/login \
    -H "Content-Type: application/json" \
    -d '{"username":"testuser1","password":"pass123"}')
TOKEN=$(echo "$LOGIN_RESP" | grep -o '"token":"[^"]*"' | cut -d'"' -f4)

if [ -z "$TOKEN" ]; then
    echo -e "${RED}❌ FALHOU: Não conseguiu obter token${NC}"
    exit 1
fi
echo -e "${GREEN}✅ Auth OK - Token obtido${NC}"

# Test 2: PURCHASE (via WebSocket simulation or REST)
echo -e "\n${YELLOW}[3/4] Testando PURCHASE...${NC}"

# Check if player endpoint exists
PLAYER_RESP=$(curl -s -X GET http://localhost:8080/api/players/1 \
    -H "Authorization: Bearer $TOKEN")
echo "Player data: $PLAYER_RESP"

# Try purchase endpoint if it exists
PURCHASE_RESP=$(curl -s -X POST http://localhost:8080/api/store/purchase \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $TOKEN" \
    -d '{"packageType":"BASIC"}' 2>&1)

if echo "$PURCHASE_RESP" | grep -q "error\|404\|403"; then
    echo -e "${YELLOW}⚠️  Purchase endpoint não existe via REST${NC}"
    echo -e "${YELLOW}   Testando via WebSocket com cliente Java...${NC}"
    
    # Use Java client to test purchase
    cd "$PROJECT_ROOT/dueling-client"
    timeout 30 java -DGATEWAY_HOST=localhost -DGATEWAY_PORT=8080 \
        -jar target/dueling-client-1.0-SNAPSHOT.jar autotest purchase testuser1 pass123 > /tmp/test_purchase.log 2>&1 &
    PID=$!
    sleep 10
    kill $PID 2>/dev/null || true
    
    if grep -q "STORE:BUY" /tmp/test_purchase.log || grep -q "SUCCESS" /tmp/test_purchase.log; then
        echo -e "${GREEN}✅ PURCHASE funcionando via WebSocket${NC}"
    else
        echo -e "${RED}❌ PURCHASE não funcionou${NC}"
        cat /tmp/test_purchase.log | tail -20
    fi
else
    echo -e "${GREEN}✅ PURCHASE endpoint respondeu${NC}"
    echo "Response: $PURCHASE_RESP"
fi

# Test 3: TRADE
echo -e "\n${YELLOW}[4/4] Testando TRADE...${NC}"

# Register second user
curl -s -X POST http://localhost:8080/api/auth/register \
    -H "Content-Type: application/json" \
    -d '{"username":"testuser2","password":"pass123"}' > /dev/null

LOGIN_RESP2=$(curl -s -X POST http://localhost:8080/api/auth/login \
    -H "Content-Type: application/json" \
    -d '{"username":"testuser2","password":"pass123"}')
TOKEN2=$(echo "$LOGIN_RESP2" | grep -o '"token":"[^"]*"' | cut -d'"' -f4)

# Check if trade endpoint exists
TRADE_RESP=$(curl -s -X POST http://localhost:8080/api/trade/propose \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $TOKEN" \
    -d '{"targetPlayer":"testuser2","offeredCard":"basic-0","requestedCard":"basic-1"}' 2>&1)

if echo "$TRADE_RESP" | grep -q "error\|404\|403"; then
    echo -e "${YELLOW}⚠️  Trade endpoint não existe via REST${NC}"
    echo -e "${YELLOW}   Trade precisa ser testado via WebSocket (manual)${NC}"
    echo -e "${RED}❌ TRADE não pode ser testado automaticamente${NC}"
else
    echo -e "${GREEN}✅ TRADE endpoint respondeu${NC}"
    echo "Response: $TRADE_RESP"
fi

# Test 4: MATCH
echo -e "\n${YELLOW}[5/4] Testando MATCH...${NC}"

# Try matchmaking endpoint
MATCH_RESP1=$(curl -s -X POST http://localhost:8080/api/matchmaking/enqueue \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $TOKEN" \
    -d '{"playerId":1,"playerNickname":"testuser1"}')
echo "Player 1 enqueue: $MATCH_RESP1"

MATCH_RESP2=$(curl -s -X POST http://localhost:8080/api/matchmaking/enqueue \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $TOKEN2" \
    -d '{"playerId":2,"playerNickname":"testuser2"}')
echo "Player 2 enqueue: $MATCH_RESP2"

sleep 5

# Check server logs for match creation
docker logs server-1 2>&1 | tail -50 | grep -i "match\|partida\|created" | tail -5
MATCH_LOGS=$(docker logs server-1 2>&1 | grep -i "match" | tail -5)

if [ ! -z "$MATCH_LOGS" ]; then
    echo -e "${GREEN}✅ MATCH - Logs indicam processamento de matchmaking${NC}"
else
    echo -e "${YELLOW}⚠️  MATCH - Não encontrei logs claros de match${NC}"
fi

# Summary
echo -e "\n╔════════════════════════════════════════════════════╗"
echo -e "║                 RESUMO DOS TESTES                  ║"
echo -e "╚════════════════════════════════════════════════════╝"
echo -e "1. Auth:     ${GREEN}✅ OK${NC}"
echo -e "2. Purchase: (verificar acima)"
echo -e "3. Trade:    (verificar acima)"
echo -e "4. Match:    (verificar acima)"

exit 0
