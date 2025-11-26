#!/bin/bash

# Script para iniciar apenas o NGINX Load Balancer
SCRIPT_DIR=$(cd "$(dirname -- "$0")" && cd .. && pwd)

GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${BLUE}Starting NGINX Load Balancer...${NC}"
echo -e "${YELLOW}Note: At least one game server should be running${NC}"
echo ""

# Check if any server is running
SERVERS_RUNNING=0
for i in 1 2 3 4; do
    if docker ps | grep -q "server-$i"; then
        SERVERS_RUNNING=$((SERVERS_RUNNING + 1))
    fi
done

if [ $SERVERS_RUNNING -eq 0 ]; then
    echo -e "${RED}⚠️  Warning: No game servers are currently running${NC}"
    echo "NGINX will start but won't be able to route traffic"
    echo ""
    read -p "Continue anyway? (y/N): " confirm
    if [[ ! "$confirm" =~ ^[Yy]$ ]]; then
        echo "Cancelled"
        exit 0
    fi
fi

cd "$SCRIPT_DIR/docker"

# Start nginx-gateway
docker compose up -d nginx-gateway

echo ""
echo -e "${GREEN}✅ NGINX Load Balancer starting...${NC}"
echo "Status:"
docker ps --filter "name=nginx-gateway" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
echo ""
echo "Gateway URL: http://localhost:8080"
echo "Health check: http://localhost:8080/nginx-health"
echo ""
echo "Available servers: $SERVERS_RUNNING"
