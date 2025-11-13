#!/bin/bash

# Keep Blockchain Node Running
# This script keeps Hardhat node alive even after deploy

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$PROJECT_DIR/dueling-blockchain"

echo "🔗 Starting Blockchain Node (persistent mode)"
echo "=============================================="
echo ""
echo "This node will keep running even after deployments."
echo "Press Ctrl+C to stop."
echo ""

# Check if already running
if lsof -i:8545 > /dev/null 2>&1; then
    echo "⚠️  Port 8545 is already in use!"
    echo ""
    ps aux | grep -E "hardhat|node.*8545" | grep -v grep
    echo ""
    read -p "Kill existing process and restart? (y/N): " restart
    if [[ "$restart" == "y" || "$restart" == "Y" ]]; then
        pkill -f "hardhat node"
        sleep 2
    else
        exit 1
    fi
fi

# Install dependencies if needed
if [ ! -d "node_modules" ]; then
    echo "📦 Installing dependencies..."
    npm install
    echo ""
fi

# Start node
echo "Starting Hardhat node on http://localhost:8545..."
echo ""

# Use exec to replace shell with node process (prevents orphan)
exec npm run node
