#!/bin/sh

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
