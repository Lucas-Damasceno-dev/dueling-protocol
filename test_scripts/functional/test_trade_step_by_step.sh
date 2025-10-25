#!/bin/bash

# Teste automatizado de TRADE passo a passo com debug

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

SERVER_URL="http://localhost:8083"
WS_URL="ws://localhost:8083/ws"

echo -e "${BLUE}═══════════════════════════════════════════════════${NC}"
echo -e "${BLUE}  Teste de Trade - Passo a Passo com Debug${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════${NC}"
echo ""

# Função para fazer requisição HTTP
make_request() {
    local method=$1
    local endpoint=$2
    local data=$3
    local token=$4
    
    if [ -n "$token" ]; then
        curl -s -X "$method" "${SERVER_URL}${endpoint}" \
            -H "Content-Type: application/json" \
            -H "Authorization: Bearer $token" \
            -d "$data"
    else
        curl -s -X "$method" "${SERVER_URL}${endpoint}" \
            -H "Content-Type: application/json" \
            -d "$data"
    fi
}

echo -e "${YELLOW}[1/8] Registrando trader_a...${NC}"
REGISTER_A=$(make_request POST "/api/auth/register" '{"username":"trader_a","password":"123"}')
echo "$REGISTER_A" | jq '.' 2>/dev/null || echo "$REGISTER_A"

echo -e "${YELLOW}[2/8] Registrando trader_b...${NC}"
REGISTER_B=$(make_request POST "/api/auth/register" '{"username":"trader_b","password":"123"}')
echo "$REGISTER_B" | jq '.' 2>/dev/null || echo "$REGISTER_B"

echo -e "${YELLOW}[3/8] Fazendo login de trader_a...${NC}"
LOGIN_A=$(make_request POST "/api/auth/login" '{"username":"trader_a","password":"123"}')
TOKEN_A=$(echo "$LOGIN_A" | jq -r '.token' 2>/dev/null)
echo "Token A: ${TOKEN_A:0:20}..."

echo -e "${YELLOW}[4/8] Fazendo login de trader_b...${NC}"
LOGIN_B=$(make_request POST "/api/auth/login" '{"username":"trader_b","password":"123"}')
TOKEN_B=$(echo "$LOGIN_B" | jq -r '.token' 2>/dev/null)
echo "Token B: ${TOKEN_B:0:20}..."

if [ "$TOKEN_A" == "null" ] || [ -z "$TOKEN_A" ]; then
    echo -e "${RED}✗ Falha ao obter token A${NC}"
    exit 1
fi

if [ "$TOKEN_B" == "null" ] || [ -z "$TOKEN_B" ]; then
    echo -e "${RED}✗ Falha ao obter token B${NC}"
    exit 1
fi

echo -e "${GREEN}✓ Autenticação bem-sucedida${NC}"
echo ""

echo -e "${BLUE}═══════════════════════════════════════════════════${NC}"
echo -e "${BLUE}  Agora conecte 2 clientes manualmente:${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════${NC}"
echo ""
echo -e "${YELLOW}Terminal 1 (Jogador A):${NC}"
echo "  cd dueling-client"
echo "  java -jar target/dueling-client-1.0-SNAPSHOT.jar"
echo ""
echo "  Ações:"
echo "  1. Login"
echo "  2. Username: ${GREEN}trader_a${NC}"
echo "  3. Password: ${GREEN}123${NC}"
echo "  4. Set up character (opção 1)"
echo "  5. Buy card pack (opção 4 → 1)"
echo "  6. Trade cards (opção 8 → 1)"
echo "     - Target: ${GREEN}trader_b${NC}"
echo "     - Offer: ${GREEN}basic-0${NC}"
echo "     - Request: ${GREEN}basic-1${NC}"
echo ""
echo -e "${YELLOW}Terminal 2 (Jogador B):${NC}"
echo "  cd dueling-client"
echo "  java -jar target/dueling-client-1.0-SNAPSHOT.jar"
echo ""
echo "  Ações:"
echo "  1. Login"
echo "  2. Username: ${GREEN}trader_b${NC}"
echo "  3. Password: ${GREEN}123${NC}"
echo "  4. Set up character (opção 1)"
echo "  5. Buy card pack (opção 4 → 1)"
echo "  6. ${YELLOW}Aguardar notificação de trade${NC}"
echo "  7. Trade cards (opção 8 → 2)"
echo "     - Trade ID: ${GREEN}[copiar da notificação]${NC}"
echo ""
echo -e "${GREEN}═══════════════════════════════════════════════════${NC}"
echo -e "${GREEN}  Usuários criados com sucesso!${NC}"
echo -e "${GREEN}═══════════════════════════════════════════════════${NC}"
