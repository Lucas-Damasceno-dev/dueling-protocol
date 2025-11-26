#!/bin/bash

# Script para iniciar apenas o Blockchain
SCRIPT_DIR=$(cd "$(dirname -- "$0")" && cd .. && pwd)

GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${BLUE}Starting Blockchain Node...${NC}"
echo -e "${YELLOW}This will start Hardhat node and auto-deploy contracts${NC}"
echo ""

cd "$SCRIPT_DIR/docker"

# Start blockchain
docker compose up -d dueling-blockchain

echo ""
echo -e "${GREEN}✅ Blockchain node starting...${NC}"
echo "Waiting for deployment (this may take 30-45 seconds)..."
sleep 10

echo ""
echo "Status:"
docker ps --filter "name=dueling-blockchain" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
echo ""
echo "RPC URL: http://localhost:8545"
echo ""
echo "To check deployment progress:"
echo "  docker logs -f dueling-blockchain"
