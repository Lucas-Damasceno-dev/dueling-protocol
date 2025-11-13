#!/bin/bash

# Auto-deploy contracts when blockchain starts
# This script should run AFTER blockchain is ready

echo "⏳ Waiting for blockchain to be ready..."
sleep 5

echo "📦 Deploying smart contracts..."
cd /app
npx hardhat run scripts/deploy.js --network localhost

echo "✅ Deployment complete!"
echo "📋 Contract addresses saved to deployment-info.json"
