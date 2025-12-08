#!/bin/sh
#
# start-blockchain.sh - Start local blockchain with deployed contracts
#
# Description:
#   Starts Hardhat Network (local Ethereum blockchain) and automatically
#   deploys all smart contracts. This script is used for development and testing.
#
# Usage:
#   ./start-blockchain.sh
#   npm run node  (equivalent npm script)
#
# What it does:
#   1. Starts Hardhat node listening on 0.0.0.0:8545
#   2. Waits 10 seconds for node to be ready
#   3. Deploys all contracts (AssetContract, TradeContract, etc.)
#   4. Saves contract addresses to deployment-info.json
#   5. Keeps node running until terminated (Ctrl+C)
#
# Requirements:
#   - Node.js and npm installed
#   - Dependencies installed (npm install)
#   - No other process using port 8545
#
# Output:
#   - deployment-info.json: Contract addresses for backend integration
#   - Blockchain running on: http://localhost:8545
#
# Troubleshooting:
#   - Port 8545 in use: lsof -ti:8545 | xargs kill -9
#   - Dependencies missing: npm install
#   - Deploy failed: Check logs for Solidity compilation errors
#

echo "🚀 Starting Hardhat Node..."

# Start Hardhat node in background
npx hardhat node --hostname 0.0.0.0 &
HARDHAT_PID=$!

echo "⏳ Waiting for blockchain to be ready..."
sleep 10

echo "📦 Deploying smart contracts..."
npx hardhat run scripts/deploy.js --network localhost

echo "✅ Blockchain ready with deployed contracts!"
echo "📋 Contract addresses saved to deployment-info.json"

# Keep Hardhat node running in foreground
wait $HARDHAT_PID
