#!/bin/bash

# Script para iniciar os Redis Sentinels
SCRIPT_DIR=$(cd "$(dirname -- "$0")" && cd .. && pwd)

GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${BLUE}Starting Redis Sentinels...${NC}"
echo -e "${YELLOW}Note: Redis Master and Slave should be running first${NC}"
echo ""

cd "$SCRIPT_DIR/docker"

# Start all sentinels
docker compose up -d redis-sentinel-1 redis-sentinel-2 redis-sentinel-3

echo ""
echo -e "${GREEN}✅ Redis Sentinels started!${NC}"
echo "Status:"
docker ps --filter "name=redis-sentinel" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
echo ""
echo "Sentinel ports: 26379, 26380, 26381"
echo ""
echo "Check sentinel status:"
echo "  docker exec redis-sentinel-1 redis-cli -p 26379 SENTINEL masters"
