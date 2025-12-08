#!/bin/bash

# Verify deployed contracts on Etherscan
# Usage: ./verify-contracts.sh [network]
# Example: ./verify-contracts.sh sepolia

NETWORK=${1:-sepolia}

if [ ! -f deployment-info.json ]; then
    echo "❌ Error: deployment-info.json not found"
    echo "Deploy contracts first using: npm run deploy:sepolia"
    exit 1
fi

if [ ! -f .env ]; then
    echo "❌ Error: .env file not found"
    exit 1
fi

source .env

if [ -z "$ETHERSCAN_API_KEY" ]; then
    echo "⚠️  Warning: ETHERSCAN_API_KEY not set"
    echo "Verification will fail without API key"
    echo "Get one from: https://etherscan.io/myapikey"
    exit 1
fi

echo "🔍 Verifying contracts on $NETWORK..."
echo ""

# Extract addresses from deployment-info.json
ASSET_CONTRACT=$(cat deployment-info.json | grep -o '"AssetContract": "[^"]*"' | cut -d'"' -f4)
STORE_CONTRACT=$(cat deployment-info.json | grep -o '"StoreContract": "[^"]*"' | cut -d'"' -f4)
TRADE_CONTRACT=$(cat deployment-info.json | grep -o '"TradeContract": "[^"]*"' | cut -d'"' -f4)
MATCH_CONTRACT=$(cat deployment-info.json | grep -o '"MatchContract": "[^"]*"' | cut -d'"' -f4)
INTEGRITY_CONTRACT=$(cat deployment-info.json | grep -o '"IntegrityContract": "[^"]*"' | cut -d'"' -f4)
ORACLE_REGISTRY=$(cat deployment-info.json | grep -o '"OracleRegistry": "[^"]*"' | cut -d'"' -f4)

echo "📋 Contracts to verify:"
echo "  AssetContract: $ASSET_CONTRACT"
echo "  StoreContract: $STORE_CONTRACT"
echo "  TradeContract: $TRADE_CONTRACT"
echo "  MatchContract: $MATCH_CONTRACT"
echo "  IntegrityContract: $INTEGRITY_CONTRACT"
echo "  OracleRegistry: $ORACLE_REGISTRY"
echo ""

# Verify each contract
echo "🔍 Verifying AssetContract..."
npx hardhat verify --network $NETWORK $ASSET_CONTRACT || echo "⚠️  AssetContract verification failed"

echo "🔍 Verifying StoreContract..."
npx hardhat verify --network $NETWORK $STORE_CONTRACT $ASSET_CONTRACT || echo "⚠️  StoreContract verification failed"

echo "🔍 Verifying TradeContract..."
npx hardhat verify --network $NETWORK $TRADE_CONTRACT $ASSET_CONTRACT || echo "⚠️  TradeContract verification failed"

echo "🔍 Verifying MatchContract..."
npx hardhat verify --network $NETWORK $MATCH_CONTRACT || echo "⚠️  MatchContract verification failed"

echo "🔍 Verifying IntegrityContract..."
npx hardhat verify --network $NETWORK $INTEGRITY_CONTRACT || echo "⚠️  IntegrityContract verification failed"

echo "🔍 Verifying OracleRegistry..."
npx hardhat verify --network $NETWORK $ORACLE_REGISTRY || echo "⚠️  OracleRegistry verification failed"

echo ""
echo "✅ Verification complete!"
echo ""
echo "View verified contracts at:"
if [ "$NETWORK" = "sepolia" ]; then
    echo "  https://sepolia.etherscan.io/address/$ASSET_CONTRACT#code"
else
    echo "  https://etherscan.io/address/$ASSET_CONTRACT#code"
fi
