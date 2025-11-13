#!/bin/bash

# Script to update blockchain contract addresses after deployment
# Reads from dueling-blockchain/deployment-info.json

cd "$(dirname "$0")/.."

echo "=================================================="
echo "  UPDATING BLOCKCHAIN CONTRACT ADDRESSES"
echo "=================================================="
echo ""

# Check if deployment-info.json exists
if [ ! -f "dueling-blockchain/deployment-info.json" ]; then
    echo "❌ deployment-info.json not found!"
    echo "   Please deploy contracts first:"
    echo "   cd dueling-blockchain && npx hardhat run scripts/deploy.js --network localhost"
    exit 1
fi

# Extract addresses from deployment-info.json
ASSET=$(cat dueling-blockchain/deployment-info.json | python3 -c "import json, sys; print(json.load(sys.stdin)['contracts']['AssetContract'])")
STORE=$(cat dueling-blockchain/deployment-info.json | python3 -c "import json, sys; print(json.load(sys.stdin)['contracts']['StoreContract'])")
TRADE=$(cat dueling-blockchain/deployment-info.json | python3 -c "import json, sys; print(json.load(sys.stdin)['contracts']['TradeContract'])")
MATCH=$(cat dueling-blockchain/deployment-info.json | python3 -c "import json, sys; print(json.load(sys.stdin)['contracts']['MatchContract'])")

echo "📋 New Contract Addresses:"
echo "   AssetContract : $ASSET"
echo "   StoreContract : $STORE"
echo "   TradeContract : $TRADE"
echo "   MatchContract : $MATCH"
echo ""

# Update docker/docker-compose.yml
echo "📝 Updating docker/docker-compose.yml..."
sed -i "s/ASSET_CONTRACT: \".*\"/ASSET_CONTRACT: \"$ASSET\"/g" docker/docker-compose.yml
sed -i "s/STORE_CONTRACT: \".*\"/STORE_CONTRACT: \"$STORE\"/g" docker/docker-compose.yml
sed -i "s/TRADE_CONTRACT: \".*\"/TRADE_CONTRACT: \"$TRADE\"/g" docker/docker-compose.yml
sed -i "s/MATCH_CONTRACT: \".*\"/MATCH_CONTRACT: \"$MATCH\"/g" docker/docker-compose.yml

# Update scripts/run_server.sh
echo "📝 Updating scripts/run_server.sh..."
sed -i "s/-e ASSET_CONTRACT=.*/-e ASSET_CONTRACT=$ASSET \\\\/g" scripts/run_server.sh
sed -i "s/-e STORE_CONTRACT=.*/-e STORE_CONTRACT=$STORE \\\\/g" scripts/run_server.sh
sed -i "s/-e TRADE_CONTRACT=.*/-e TRADE_CONTRACT=$TRADE \\\\/g" scripts/run_server.sh
sed -i "s/-e MATCH_CONTRACT=.*/-e MATCH_CONTRACT=$MATCH \\\\/g" scripts/run_server.sh

# Update dueling-blockchain/scripts/verify-ledger.js
echo "📝 Updating dueling-blockchain/scripts/verify-ledger.js..."
sed -i "s/const assetAddress = \".*\";/const assetAddress = \"$ASSET\";/g" dueling-blockchain/scripts/verify-ledger.js
sed -i "s/const matchAddress = \".*\";/const matchAddress = \"$MATCH\";/g" dueling-blockchain/scripts/verify-ledger.js

echo ""
echo "✅ All files updated successfully!"
echo ""
echo "⚠️  IMPORTANT: You must restart the servers for changes to take effect:"
echo "   ./menu.sh → option 7 (Stop All Services)"
echo "   ./menu.sh → option 5 (Start Complete System)"
echo ""
