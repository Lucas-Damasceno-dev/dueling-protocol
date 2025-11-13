#!/bin/bash

# Automated test script for blockchain ledger verification

echo "╔════════════════════════════════════════════════════════════════╗"
echo "║          🧪 AUTOMATED LEDGER TEST - Full Workflow              ║"
echo "╚════════════════════════════════════════════════════════════════╝"
echo ""

# Check if blockchain is running
if ! docker ps | grep -q dueling-blockchain; then
    echo "❌ Blockchain container not running! Start the system first."
    exit 1
fi

# Check if servers are ready
echo "⏳ Waiting for servers to be ready..."
sleep 5

MAX_RETRIES=10
RETRY_COUNT=0
while [ $RETRY_COUNT -lt $MAX_RETRIES ]; do
    if docker logs server-1 2>&1 | grep -q "Contract addresses loaded"; then
        echo "✅ Server ready!"
        break
    fi
    RETRY_COUNT=$((RETRY_COUNT + 1))
    if [ $RETRY_COUNT -eq $MAX_RETRIES ]; then
        echo "❌ Timeout waiting for server"
        exit 1
    fi
    sleep 3
done

echo ""
echo "📊 Current blockchain state BEFORE test:"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

# Get current state
cd /home/lucas/Documentos/dev/projects/dueling-protocol/dueling-blockchain
BEFORE_CARDS=$(npx hardhat run scripts/verify-ledger.js --network localhost 2>/dev/null | grep "Total Cards Minted:" | awk '{print $4}')
BEFORE_TRANSFERS=$(npx hardhat run scripts/verify-ledger.js --network localhost 2>/dev/null | grep "Total Card Transfers:" | awk '{print $4}')
BEFORE_MATCHES=$(npx hardhat run scripts/verify-ledger.js --network localhost 2>/dev/null | grep "Total Matches Recorded:" | awk '{print $4}')

echo "  Cards: $BEFORE_CARDS"
echo "  Transfers: $BEFORE_TRANSFERS"
echo "  Matches: $BEFORE_MATCHES"
echo ""

# Perform a test match using simulation
echo "🎮 Simulating a test match..."
cd /home/lucas/Documentos/dev/projects/dueling-protocol/dueling-blockchain
npm run simulate > /dev/null 2>&1

echo ""
echo "📊 Blockchain state AFTER simulation:"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

AFTER_CARDS=$(npx hardhat run scripts/verify-ledger.js --network localhost 2>/dev/null | grep "Total Cards Minted:" | awk '{print $4}')
AFTER_TRANSFERS=$(npx hardhat run scripts/verify-ledger.js --network localhost 2>/dev/null | grep "Total Card Transfers:" | awk '{print $4}')
AFTER_MATCHES=$(npx hardhat run scripts/verify-ledger.js --network localhost 2>/dev/null | grep "Total Matches Recorded:" | awk '{print $4}')

echo "  Cards: $AFTER_CARDS (delta: +$((AFTER_CARDS - BEFORE_CARDS)))"
echo "  Transfers: $AFTER_TRANSFERS (delta: +$((AFTER_TRANSFERS - BEFORE_TRANSFERS)))"
echo "  Matches: $AFTER_MATCHES (delta: +$((AFTER_MATCHES - BEFORE_MATCHES)))"
echo ""

# Verdict
echo "🔍 VERDICT:"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

if [ $((AFTER_CARDS - BEFORE_CARDS)) -eq 10 ]; then
    echo "  ✅ Card purchases: WORKING"
else
    echo "  ❌ Card purchases: FAILED"
fi

if [ $((AFTER_TRANSFERS - BEFORE_TRANSFERS)) -ge 1 ]; then
    echo "  ✅ Card trades: WORKING"
else
    echo "  ⚠️  Card trades: NOT VERIFIED"
fi

if [ $((AFTER_MATCHES - BEFORE_MATCHES)) -ge 1 ]; then
    echo "  ✅ Match recording: WORKING"
else
    echo "  ❌ Match recording: FAILED"
fi

echo ""
echo "📋 To see full details, run: ./menu.sh → option 54"
echo ""
