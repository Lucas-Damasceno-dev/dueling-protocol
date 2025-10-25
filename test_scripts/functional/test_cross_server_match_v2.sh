#!/bin/bash

# Cross-Server Match Test - WITH AUTHENTICATION
# Properly registers users, logs in, and tests cross-server matchmaking

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}╔══════════════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║       Teste Cross-Server Match (com autenticação)           ║${NC}"
echo -e "${BLUE}╚══════════════════════════════════════════════════════════════╝${NC}"
echo ""

# Check websocat
if ! command -v websocat &> /dev/null; then
    echo -e "${RED}ERROR: websocat not installed!${NC}"
    echo "Install with: cargo install websocat"
    exit 1
fi

# Unique IDs
TIMESTAMP=$(date +%s)
PLAYER_A="match_a_${TIMESTAMP}"
PLAYER_B="match_b_${TIMESTAMP}"
PASSWORD="pass123"

SERVER1="http://localhost:8081"
SERVER2="http://localhost:8083"
WS1="ws://localhost:8081/ws"
WS2="ws://localhost:8083/ws"

echo -e "${YELLOW}Configuração:${NC}"
echo "  Server 1: $SERVER1 (Player A)"
echo "  Server 2: $SERVER2 (Player B)"
echo ""

# Step 1: Register and login Player A on Server 1
echo -e "${BLUE}═══ Step 1: Register Player A on Server 1 ═══${NC}"
RESP_A=$(curl -s -X POST "${SERVER1}/api/auth/register" \
    -H "Content-Type: application/json" \
    -d "{\"username\": \"$PLAYER_A\", \"password\": \"$PASSWORD\", \"playerId\": \"$PLAYER_A\"}")

if echo "$RESP_A" | jq -e '.error != null and .error != ""' > /dev/null 2>&1; then
    echo -e "${RED}✗ Failed to register Player A: $RESP_A${NC}"
    exit 1
fi
echo -e "${GREEN}✓ Player A registered${NC}"

TOKEN_A=$(curl -s -X POST "${SERVER1}/api/auth/login" \
    -H "Content-Type: application/json" \
    -d "{\"username\": \"$PLAYER_A\", \"password\": \"$PASSWORD\"}" | jq -r '.token')

if [ -z "$TOKEN_A" ] || [ "$TOKEN_A" = "null" ]; then
    echo -e "${RED}✗ Failed to login Player A${NC}"
    exit 1
fi
echo -e "${GREEN}✓ Player A logged in${NC}"

# Step 2: Register and login Player B on Server 2
echo -e "${BLUE}═══ Step 2: Register Player B on Server 2 ═══${NC}"
RESP_B=$(curl -s -X POST "${SERVER2}/api/auth/register" \
    -H "Content-Type: application/json" \
    -d "{\"username\": \"$PLAYER_B\", \"password\": \"$PASSWORD\", \"playerId\": \"$PLAYER_B\"}")

if echo "$RESP_B" | jq -e '.error != null and .error != ""' > /dev/null 2>&1; then
    echo -e "${RED}✗ Failed to register Player B: $RESP_B${NC}"
    exit 1
fi
echo -e "${GREEN}✓ Player B registered${NC}"

TOKEN_B=$(curl -s -X POST "${SERVER2}/api/auth/login" \
    -H "Content-Type: application/json" \
    -d "{\"username\": \"$PLAYER_B\", \"password\": \"$PASSWORD\"}" | jq -r '.token')

if [ -z "$TOKEN_B" ] || [ "$TOKEN_B" = "null" ]; then
    echo -e "${RED}✗ Failed to login Player B${NC}"
    exit 1
fi
echo -e "${GREEN}✓ Player B logged in${NC}"

# Step 3: Create characters
echo -e "${BLUE}═══ Step 3: Create Characters ═══${NC}"

echo "GAME:${PLAYER_A}:CHARACTER_SETUP:HeroA:Human:Warrior" | websocat -n "${WS1}?token=${TOKEN_A}" 2>&1 > /dev/null
sleep 2
echo -e "${GREEN}✓ Player A character created${NC}"

echo "GAME:${PLAYER_B}:CHARACTER_SETUP:HeroB:Elf:Mage" | websocat -n "${WS2}?token=${TOKEN_B}" 2>&1 > /dev/null
sleep 2
echo -e "${GREEN}✓ Player B character created${NC}"

# Step 4: Start listeners
echo -e "${BLUE}═══ Step 4: Start WebSocket Listeners ═══${NC}"

TMP_DIR="/tmp/match_test_${TIMESTAMP}"
mkdir -p "$TMP_DIR"
LOG_A="${TMP_DIR}/player_a.log"
LOG_B="${TMP_DIR}/player_b.log"

timeout 60s websocat "${WS1}?token=${TOKEN_A}" > "$LOG_A" 2>&1 &
PID_A=$!
timeout 60s websocat "${WS2}?token=${TOKEN_B}" > "$LOG_B" 2>&1 &
PID_B=$!
sleep 3
echo -e "${GREEN}✓ Listeners started (PIDs: $PID_A, $PID_B)${NC}"

# Step 5: Enter matchmaking
echo -e "${BLUE}═══ Step 5: Enter Matchmaking ═══${NC}"

echo "GAME:${PLAYER_A}:MATCHMAKING:ENTER" | websocat -n "${WS1}?token=${TOKEN_A}" 2>&1 > /dev/null &
sleep 3
echo -e "${GREEN}✓ Player A entered matchmaking${NC}"

echo "GAME:${PLAYER_B}:MATCHMAKING:ENTER" | websocat -n "${WS2}?token=${TOKEN_B}" 2>&1 > /dev/null &
sleep 15
echo -e "${GREEN}✓ Player B entered matchmaking${NC}"

# Step 6: Check for match
echo -e "${BLUE}═══ Step 6: Verify Match Creation ═══${NC}"
echo ""

# Kill listeners
kill $PID_A $PID_B 2>/dev/null || true
wait $PID_A $PID_B 2>/dev/null || true

# Check logs
MATCH_A=$(grep -c "MATCH_FOUND\|UPDATE:MATCH\|GAME:MATCHED" "$LOG_A" 2>/dev/null || echo "0")
MATCH_B=$(grep -c "MATCH_FOUND\|UPDATE:MATCH\|GAME:MATCHED" "$LOG_B" 2>/dev/null || echo "0")

MATCH_A=$(echo "$MATCH_A" | tr -d '\n' | tr -d ' ')
MATCH_B=$(echo "$MATCH_B" | tr -d '\n' | tr -d ' ')

if [ "$MATCH_A" -gt 0 ] || [ "$MATCH_B" -gt 0 ]; then
    echo -e "${GREEN}╔════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${GREEN}║  ✓ MATCH CRIADO COM SUCESSO!                              ║${NC}"
    echo -e "${GREEN}╚════════════════════════════════════════════════════════════╝${NC}"
    echo ""
    echo -e "${YELLOW}Matches encontrados:${NC}"
    echo "  Player A: $MATCH_A"
    echo "  Player B: $MATCH_B"
    echo ""
    echo -e "${YELLOW}Log de Player A:${NC}"
    cat "$LOG_A"
    echo ""
    echo -e "${YELLOW}Log de Player B:${NC}"
    cat "$LOG_B"
    exit 0
else
    echo -e "${RED}╔════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${RED}║  ✗ MATCH NÃO FOI CRIADO                                   ║${NC}"
    echo -e "${RED}╚════════════════════════════════════════════════════════════╝${NC}"
    echo ""
    echo -e "${YELLOW}Log de Player A:${NC}"
    cat "$LOG_A"
    echo ""
    echo -e "${YELLOW}Log de Player B:${NC}"
    cat "$LOG_B"
    echo ""
    echo -e "${YELLOW}Logs salvos em: $TMP_DIR${NC}"
    exit 1
fi
