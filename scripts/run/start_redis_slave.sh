#!/bin/bash

# Script para iniciar apenas o Redis Slave
SCRIPT_DIR=$(cd "$(dirname -- "$0")" && cd .. && pwd)

GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${BLUE}Starting Redis Slave...${NC}"
echo -e "${YELLOW}Note: Redis Master should be running first${NC}"
echo ""

cd "$SCRIPT_DIR/docker"

# Start redis-slave (depends on redis-master)
docker compose up -d redis-slave

echo ""
echo -e "${GREEN}✅ Redis Slave started!${NC}"
echo "Status:"
docker ps --filter "name=redis-slave" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
echo ""
echo "Connection: redis://localhost:6380"
