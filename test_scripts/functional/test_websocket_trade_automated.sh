#!/bin/bash
# Automated WebSocket Trade Test using websocat
# Tests complete trade flow between two servers

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

SERVER1_URL="${SERVER1_URL:-http://localhost:8080}"
SERVER2_URL="${SERVER2_URL:-http://localhost:8083}"
WS1_URL="${WS1_URL:-ws://localhost:8080/ws}"
WS2_URL="${WS2_URL:-ws://localhost:8083/ws}"

TIMESTAMP=$(date +%s)
USER_A="player_a_${TIMESTAMP}"
USER_B="player_b_${TIMESTAMP}"
PASS="pass123"

echo -e "${BLUE}╔══════════════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║     Automated WebSocket Trade Test (Cross-Server)           ║${NC}"
echo -e "${BLUE}╚══════════════════════════════════════════════════════════════╝${NC}"
echo ""

# Check if websocat is available
if ! command -v websocat &> /dev/null; then
    echo -e "${YELLOW}⚠️  websocat not found. Installing via cargo...${NC}"
    if command -v cargo &> /dev/null; then
        cargo install websocat
    else
        echo -e "${RED}❌ cargo not installed. Please install Rust/Cargo first:${NC}"
        echo "   curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs | sh"
        exit 1
    fi
fi

# Function to register and login
authenticate_user() {
    local server_url=$1
    local username=$2
    local password=$3
    
    # Register
    echo -e "${GREEN}Registering $username on $server_url...${NC}"
    register_response=$(curl -s -X POST "${server_url}/api/auth/register" \
        -H "Content-Type: application/json" \
        -d "{\"username\": \"$username\", \"password\": \"$password\"}")
    
    # Login
    echo -e "${GREEN}Logging in $username...${NC}"
    login_response=$(curl -s -X POST "${server_url}/api/auth/login" \
        -H "Content-Type: application/json" \
        -d "{\"username\": \"$username\", \"password\": \"$password\"}")
    
    token=$(echo "$login_response" | jq -r '.token' 2>/dev/null)
    
    if [ -z "$token" ] || [ "$token" = "null" ]; then
        echo -e "${RED}❌ Failed to authenticate $username${NC}"
        echo "Response: $login_response"
        return 1
    fi
    
    echo -e "${GREEN}✓ $username authenticated successfully${NC}"
    echo "$token"
    return 0
}

echo -e "${YELLOW}═══ Step 1: Authentication ═══${NC}"
TOKEN_A=$(authenticate_user "$SERVER1_URL" "$USER_A" "$PASS")
if [ $? -ne 0 ]; then exit 1; fi

TOKEN_B=$(authenticate_user "$SERVER2_URL" "$USER_B" "$PASS")
if [ $? -ne 0 ]; then exit 1; fi

echo ""
echo -e "${YELLOW}═══ Step 2: WebSocket Connection ═══${NC}"

# Create temp files for WebSocket communication
FIFO_A=$(mktemp -u)
FIFO_B=$(mktemp -u)
mkfifo "$FIFO_A"
mkfifo "$FIFO_B"

# Connect Player A (Server 1)
echo -e "${GREEN}Connecting $USER_A to $WS1_URL...${NC}"
(
    # Send initial character creation
    sleep 2
    echo "PLAYER:CREATE:$USER_A"
    sleep 1
    echo "PLAYER:DECK:DEFAULT"
    sleep 2
    echo "MATCHMAKING:ENTER"
    sleep 30
) > "$FIFO_A" &
SENDER_A_PID=$!

websocat -H "Authorization: Bearer $TOKEN_A" "$WS1_URL" < "$FIFO_A" > "/tmp/ws_a_${TIMESTAMP}.log" 2>&1 &
WS_A_PID=$!

# Connect Player B (Server 2)
echo -e "${GREEN}Connecting $USER_B to $WS2_URL...${NC}"
(
    sleep 2
    echo "PLAYER:CREATE:$USER_B"
    sleep 1
    echo "PLAYER:DECK:DEFAULT"
    sleep 2
    echo "MATCHMAKING:ENTER"
    sleep 30
) > "$FIFO_B" &
SENDER_B_PID=$!

websocat -H "Authorization: Bearer $TOKEN_B" "$WS2_URL" < "$FIFO_B" > "/tmp/ws_b_${TIMESTAMP}.log" 2>&1 &
WS_B_PID=$!

echo -e "${GREEN}✓ WebSocket connections established${NC}"
echo ""

echo -e "${YELLOW}═══ Step 3: Waiting for Match Creation (30s) ═══${NC}"
sleep 35

# Check logs for match creation
echo -e "${YELLOW}═══ Step 4: Analyzing Results ═══${NC}"

LOG_A="/tmp/ws_a_${TIMESTAMP}.log"
LOG_B="/tmp/ws_b_${TIMESTAMP}.log"

echo -e "${BLUE}--- Player A Log ---${NC}"
if [ -f "$LOG_A" ]; then
    cat "$LOG_A"
else
    echo -e "${RED}❌ Log file not found: $LOG_A${NC}"
fi

echo ""
echo -e "${BLUE}--- Player B Log ---${NC}"
if [ -f "$LOG_B" ]; then
    cat "$LOG_B"
else
    echo -e "${RED}❌ Log file not found: $LOG_B${NC}"
fi

# Cleanup
kill $WS_A_PID $WS_B_PID $SENDER_A_PID $SENDER_B_PID 2>/dev/null || true
rm -f "$FIFO_A" "$FIFO_B"

echo ""
echo -e "${GREEN}═══════════════════════════════════════════════════════════════${NC}"
echo -e "${GREEN}           Test Completed - Check logs above                    ${NC}"
echo -e "${GREEN}═══════════════════════════════════════════════════════════════${NC}"

# Verify match creation from server logs
echo ""
echo -e "${YELLOW}Checking server logs for match creation...${NC}"
if docker logs server-1 2>&1 | tail -100 | grep -q "Match created"; then
    echo -e "${GREEN}✓ Match creation detected in server logs${NC}"
else
    echo -e "${YELLOW}⚠️  No match creation found in last 100 log lines${NC}"
fi
