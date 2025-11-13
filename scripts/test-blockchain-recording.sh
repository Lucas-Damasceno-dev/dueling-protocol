#!/bin/bash

# Test Match and Trade Recording on Blockchain
# This script verifies that matches and trades are properly recorded

echo "╔════════════════════════════════════════════════════════════════╗"
echo "║   TEST: Blockchain Match & Trade Recording Verification        ║"
echo "╚════════════════════════════════════════════════════════════════╝"
echo

# Check if system is running
echo "Step 1/3: Checking if system is running..."
if ! docker ps | grep -q dueling-blockchain; then
    echo "❌ Blockchain is not running. Please start the system first (option 5)"
    exit 1
fi
echo "✅ System is running"
echo

# Check current blockchain state BEFORE
echo "Step 2/3: Checking current blockchain state..."
cd dueling-blockchain

TOTAL_MATCHES_BEFORE=$(npx hardhat run --network localhost scripts/check-match.js 2>/dev/null | grep "Current total matches:" | awk '{print $4}')
echo "   Matches recorded: $TOTAL_MATCHES_BEFORE"

echo
echo "════════════════════════════════════════════════════════════════"
echo "  NOW: Play a complete match using option 10 (two clients)"
echo "  - Register and login two players"
echo "  - Buy card packs"
echo "  - Propose and accept a trade  
echo "  - Enter matchmaking and complete a match"
echo "════════════════════════════════════════════════════════════════"
echo
echo "Press Enter when you've finished playing the match..."
read

echo
echo "Step 3/3: Verifying blockchain updates..."
cd dueling-blockchain

# Check matches
echo "📊 Checking matches..."
TOTAL_MATCHES_AFTER=$(npx hardhat run --network localhost scripts/check-match.js 2>/dev/null | grep "Current total matches:" | awk '{print $4}')
echo "   Matches after: $TOTAL_MATCHES_AFTER"

if [ "$TOTAL_MATCHES_AFTER" -gt "$TOTAL_MATCHES_BEFORE" ]; then
    echo "   ✅ Match WAS recorded! (+$(($TOTAL_MATCHES_AFTER - $TOTAL_MATCHES_BEFORE)) matches)"
else
    echo "   ❌ Match NOT recorded"
fi

echo
echo "🔍 Full verification (option 54)..."
cd ..
bash scripts/verify_blockchain_ledger.sh

echo
echo "════════════════════════════════════════════════════════════════"
echo "Test complete!"
echo "════════════════════════════════════════════════════════════════"
