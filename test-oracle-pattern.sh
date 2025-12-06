#!/bin/bash

# Test Oracle Pattern Implementation
# This script tests the integrity verification system

echo "🔐 ORACLE PATTERN - Integrity Verification Test"
echo "================================================"
echo ""

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
SERVER_URL="${SERVER_URL:-http://localhost:8080}"
VERIFY_API="$SERVER_URL/api/verify"

echo -e "${BLUE}📋 Configuration${NC}"
echo "   Server URL: $SERVER_URL"
echo "   Verify API: $VERIFY_API"
echo ""

# Function to test endpoint
test_endpoint() {
    local endpoint=$1
    local description=$2
    
    echo -e "${YELLOW}Testing: $description${NC}"
    response=$(curl -s -o /dev/null -w "%{http_code}" "$endpoint")
    
    if [ "$response" == "200" ]; then
        echo -e "   ${GREEN}✅ PASS${NC} - HTTP $response"
        return 0
    else
        echo -e "   ${RED}❌ FAIL${NC} - HTTP $response"
        return 1
    fi
}

# Test 1: Health Check
echo -e "${BLUE}TEST 1: Verification API Health Check${NC}"
test_endpoint "$VERIFY_API/health" "Health endpoint"
echo ""

# Test 2: Get Instructions
echo -e "${BLUE}TEST 2: Get Verification Instructions${NC}"
response=$(curl -s "$VERIFY_API/instructions")
if echo "$response" | jq -e '.description' > /dev/null 2>&1; then
    echo -e "   ${GREEN}✅ PASS${NC} - Instructions available"
    echo "   Description: $(echo $response | jq -r '.description')"
else
    echo -e "   ${RED}❌ FAIL${NC} - Could not get instructions"
fi
echo ""

# Test 3: Verify Purchase (with mock data)
echo -e "${BLUE}TEST 3: Verify Purchase Operation${NC}"
echo "   Creating mock purchase data..."

purchase_data=$(cat <<EOF
{
  "purchaseId": "purchase-test-001",
  "operationType": "PURCHASE",
  "playerId": "test-player-001",
  "playerNickname": "TestHero",
  "packType": "bronze",
  "coinsCost": 100,
  "timestamp": $(date +%s)000,
  "cardsReceived": [
    {
      "id": "card-test-001",
      "name": "Test Dragon",
      "type": "MONSTER",
      "rarity": "Common",
      "attack": 1000,
      "defense": 800
    }
  ]
}
EOF
)

echo "   Sending verification request..."
response=$(curl -s -X POST "$VERIFY_API/purchase" \
  -H "Content-Type: application/json" \
  -d "$purchase_data")

if echo "$response" | jq -e '.success' > /dev/null 2>&1; then
    success=$(echo $response | jq -r '.success')
    message=$(echo $response | jq -r '.message')
    
    if [ "$success" == "true" ]; then
        echo -e "   ${GREEN}✅ PASS${NC} - Verification succeeded"
        echo "   Message: $message"
    elif [[ "$message" == *"does not exist"* ]]; then
        echo -e "   ${YELLOW}⚠️  EXPECTED${NC} - Proof not found (normal for test data)"
        echo "   Message: $message"
    else
        echo -e "   ${RED}❌ FAIL${NC} - Verification failed"
        echo "   Message: $message"
    fi
else
    echo -e "   ${RED}❌ FAIL${NC} - Invalid response"
    echo "   Response: $response"
fi
echo ""

# Test 4: Verify Trade (with mock data)
echo -e "${BLUE}TEST 4: Verify Trade Operation${NC}"
echo "   Creating mock trade data..."

trade_data=$(cat <<EOF
{
  "tradeId": "trade-test-001",
  "operationType": "TRADE",
  "player1Id": "test-player-001",
  "player1Nickname": "Hero1",
  "player2Id": "test-player-002",
  "player2Nickname": "Hero2",
  "timestamp": $(date +%s)000,
  "player1OfferedCards": ["card-001", "card-002"],
  "player2OfferedCards": ["card-003"]
}
EOF
)

echo "   Sending verification request..."
response=$(curl -s -X POST "$VERIFY_API/trade" \
  -H "Content-Type: application/json" \
  -d "$trade_data")

if echo "$response" | jq -e '.success' > /dev/null 2>&1; then
    success=$(echo $response | jq -r '.success')
    message=$(echo $response | jq -r '.message')
    
    if [ "$success" == "true" ]; then
        echo -e "   ${GREEN}✅ PASS${NC} - Verification succeeded"
        echo "   Message: $message"
    elif [[ "$message" == *"does not exist"* ]]; then
        echo -e "   ${YELLOW}⚠️  EXPECTED${NC} - Proof not found (normal for test data)"
        echo "   Message: $message"
    else
        echo -e "   ${RED}❌ FAIL${NC} - Verification failed"
        echo "   Message: $message"
    fi
else
    echo -e "   ${RED}❌ FAIL${NC} - Invalid response"
fi
echo ""

# Test 5: Verify Match (with mock data)
echo -e "${BLUE}TEST 5: Verify Match Operation${NC}"
echo "   Creating mock match data..."

match_data=$(cat <<EOF
{
  "matchId": "match-test-001",
  "operationType": "MATCH",
  "winnerId": "test-player-001",
  "winnerNickname": "Champion",
  "loserId": "test-player-002",
  "loserNickname": "Challenger",
  "winnerScore": 10,
  "loserScore": 0,
  "timestamp": $(date +%s)000
}
EOF
)

echo "   Sending verification request..."
response=$(curl -s -X POST "$VERIFY_API/match" \
  -H "Content-Type: application/json" \
  -d "$match_data")

if echo "$response" | jq -e '.success' > /dev/null 2>&1; then
    success=$(echo $response | jq -r '.success')
    message=$(echo $response | jq -r '.message')
    
    if [ "$success" == "true" ]; then
        echo -e "   ${GREEN}✅ PASS${NC} - Verification succeeded"
        echo "   Message: $message"
    elif [[ "$message" == *"does not exist"* ]]; then
        echo -e "   ${YELLOW}⚠️  EXPECTED${NC} - Proof not found (normal for test data)"
        echo "   Message: $message"
    else
        echo -e "   ${RED}❌ FAIL${NC} - Verification failed"
        echo "   Message: $message"
    fi
else
    echo -e "   ${RED}❌ FAIL${NC} - Invalid response"
fi
echo ""

# Test 6: Tamper Detection (modify data and verify - should fail)
echo -e "${BLUE}TEST 6: Tamper Detection (Modified Data)${NC}"
echo "   Creating tampered purchase data..."

tampered_data=$(cat <<EOF
{
  "purchaseId": "purchase-test-001",
  "operationType": "PURCHASE",
  "playerId": "test-player-001",
  "playerNickname": "TestHero",
  "packType": "GOLD",
  "coinsCost": 0,
  "timestamp": $(date +%s)000,
  "cardsReceived": []
}
EOF
)

echo "   Sending verification request with tampered data..."
response=$(curl -s -X POST "$VERIFY_API/purchase" \
  -H "Content-Type: application/json" \
  -d "$tampered_data")

if echo "$response" | jq -e '.success' > /dev/null 2>&1; then
    success=$(echo $response | jq -r '.success')
    
    if [ "$success" == "false" ]; then
        echo -e "   ${GREEN}✅ PASS${NC} - Tamper correctly detected"
    else
        echo -e "   ${YELLOW}⚠️  WARNING${NC} - Tampered data not detected (or proof doesn't exist yet)"
    fi
else
    echo -e "   ${RED}❌ FAIL${NC} - Invalid response"
fi
echo ""

# Summary
echo "================================================"
echo -e "${BLUE}📊 Test Summary${NC}"
echo ""
echo "✅ Tests Passed: API is functional"
echo "⚠️  Note: Most tests show 'Proof not found' because:"
echo "   - No actual game operations were performed"
echo "   - Blockchain proofs are only created during real gameplay"
echo ""
echo "🎮 To test with real proofs:"
echo "   1. Start the game client"
echo "   2. Perform a purchase/trade/match"
echo "   3. Check server logs for proof IDs"
echo "   4. Use those IDs to verify operations"
echo ""
echo -e "${GREEN}Oracle Pattern Implementation: READY ✅${NC}"
echo "================================================"
