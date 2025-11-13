#!/bin/bash

# Script to verify blockchain distributed ledger
# Checks all transactions: card mints, trades, and matches

cd "$(dirname "$0")/.."

echo ""
echo "=================================================="
echo "  BLOCKCHAIN DISTRIBUTED LEDGER VERIFICATION"
echo "=================================================="
echo ""

# Check if blockchain is running
if ! docker ps | grep -q "dueling-blockchain"; then
    echo "❌ Blockchain node is not running!"
    echo "   Please start with: ./menu.sh → option 5 or 6"
    exit 1
fi

echo "✅ Blockchain node is running"
echo ""

# Run verification script
cd dueling-blockchain
npx hardhat run --network localhost scripts/verify-ledger.js

echo ""
echo "=================================================="
echo "  VERIFICATION COMPLETE"
echo "=================================================="
echo ""
