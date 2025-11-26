#!/bin/bash

# Script para iniciar apenas o Redis Master
SCRIPT_DIR=$(cd "$(dirname -- "$0")" && cd .. && pwd)

GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}Starting Redis Master...${NC}"
cd "$SCRIPT_DIR/docker"

# Start only redis-master
docker compose up -d redis-master

echo ""
echo -e "${GREEN}✅ Redis Master started!${NC}"
echo "Status:"
docker ps --filter "name=redis-master" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
echo ""
echo "Connection: redis://localhost:6379"
