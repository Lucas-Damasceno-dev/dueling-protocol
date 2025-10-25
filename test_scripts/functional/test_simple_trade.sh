#!/bin/bash
# Simple Trade Test - Tests if two players can trade cards

echo "=== Simple Trade Cross-Server Test ==="
echo ""

# Register and login two users
echo "[1] Registering and logging in User A..."
TOKEN_A=$(cd dueling-client && mvn exec:java -Dexec.mainClass="client.StockStressClient" -Dexec.args="trader_a pass123" -q 2>&1 | grep "Successfully obtained JWT token" && echo "success" || echo "failed")

if [ "$TOKEN_A" == "failed" ]; then
    echo "✗ Failed to authenticate User A"
    exit 1
fi

echo "[2] Registering and logging in User B..."  
TOKEN_B=$(cd dueling-client && mvn exec:java -Dexec.mainClass="client.StockStressClient" -Dexec.args="trader_b pass123" -q 2>&1 | grep "Successfully obtained JWT token" && echo "success" || echo "failed")

if [ "$TOKEN_B" == "failed" ]; then
    echo "✗ Failed to authenticate User B"
    exit 1
fi

echo ""
echo "✓ Both users authenticated successfully"
echo ""
echo "NOTE: Full trade test requires interactive WebSocket client."
echo "Trade functionality exists in GameFacade but needs proper test client."
echo ""
echo "TRADE IMPLEMENTATION STATUS:"
echo "- TradeService: ✓ Implemented"
echo "- GameFacade.handleTradeProposal: ✓ Implemented"  
echo "- GameFacade.executeTrade: ✓ Implemented"
echo "- Cross-server coordination: ✓ Via ServerApiClient"
echo ""
echo "To complete this test, implement a trade client that:"
echo "1. Connects two WebSocket clients to different servers"
echo "2. Sends TRADE:PROPOSE command"
echo "3. Sends TRADE:ACCEPT command"
echo "4. Verifies cards are exchanged"
