#!/bin/bash

# Deploy Dueling Protocol to Sepolia Testnet
# Make sure you have configured .env file with:
# - SEPOLIA_RPC_URL
# - PRIVATE_KEY
# - ETHERSCAN_API_KEY (optional, for verification)

set -e

echo "🚀 Deploying Dueling Protocol to Sepolia Testnet..."
echo ""

# Check if .env exists
if [ ! -f .env ]; then
    echo "❌ Error: .env file not found"
    echo "Please create .env file with:"
    echo "  SEPOLIA_RPC_URL=https://sepolia.infura.io/v3/YOUR_KEY"
    echo "  PRIVATE_KEY=0x..."
    echo "  ETHERSCAN_API_KEY=YOUR_KEY (optional)"
    exit 1
fi

# Load environment variables
source .env

# Check if RPC URL is configured
if [ -z "$SEPOLIA_RPC_URL" ]; then
    echo "❌ Error: SEPOLIA_RPC_URL not configured in .env"
    exit 1
fi

# Check if private key is configured
if [ -z "$PRIVATE_KEY" ]; then
    echo "❌ Error: PRIVATE_KEY not configured in .env"
    exit 1
fi

echo "✅ Environment configured"
echo "📡 RPC: $SEPOLIA_RPC_URL"
echo ""

# Compile contracts
echo "📝 Compiling contracts..."
npx hardhat compile
echo "✅ Compilation complete"
echo ""

# Deploy to Sepolia
echo "🌐 Deploying to Sepolia..."
npx hardhat run scripts/deploy.js --network sepolia

echo ""
echo "✅ Deployment complete!"
echo ""
echo "📋 Next steps:"
echo "1. Save the contract addresses from deployment-info.json"
echo "2. Update server configuration with new addresses"
echo "3. (Optional) Verify contracts on Etherscan:"
echo "   npx hardhat verify --network sepolia CONTRACT_ADDRESS"
echo ""
echo "4. Get testnet ETH from faucet:"
echo "   https://sepoliafaucet.com"
echo ""
echo "5. View your contracts on Etherscan:"
echo "   https://sepolia.etherscan.io/address/YOUR_ADDRESS"
