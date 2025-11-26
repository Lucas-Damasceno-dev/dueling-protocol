#!/bin/bash

# Script para testar failover do NGINX
SCRIPT_DIR=$(cd "$(dirname -- "$0")" && cd .. && pwd)

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}╔════════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║       NGINX Load Balancer Failover Test               ║${NC}"
echo -e "${BLUE}╚════════════════════════════════════════════════════════╝${NC}"
echo ""

# Check if NGINX is running
if ! docker ps | grep -q "nginx-gateway"; then
    echo -e "${RED}❌ NGINX gateway is not running!${NC}"
    echo "Start system first with menu option 50"
    exit 1
fi

echo -e "${YELLOW}Step 1/6: Checking which servers are running${NC}"
for server in server-1 server-2 server-3 server-4; do
    if docker ps | grep -q "$server"; then
        STATUS=$(docker exec nginx-gateway curl -sf http://$server:8080/actuator/health 2>/dev/null && echo "✅ HEALTHY" || echo "❌ DOWN")
        echo "  $server: $STATUS"
    else
        echo "  $server: ⛔ NOT RUNNING"
    fi
done
echo ""

echo -e "${YELLOW}Step 2/6: Testing NGINX health endpoint${NC}"
curl -sf http://localhost:8080/nginx-health && echo -e "${GREEN}✅ NGINX is healthy${NC}" || echo -e "${RED}❌ NGINX is down${NC}"
echo ""

echo -e "${YELLOW}Step 3/6: Testing API endpoint through NGINX${NC}"
RESPONSE=$(curl -sf http://localhost:8080/actuator/health 2>/dev/null)
if [ $? -eq 0 ]; then
    echo -e "${GREEN}✅ API accessible through NGINX${NC}"
    echo "Response: $RESPONSE"
else
    echo -e "${RED}❌ API not accessible${NC}"
fi
echo ""

echo -e "${YELLOW}Step 4/6: Stopping server-1 to test failover${NC}"
docker stop server-1
echo -e "${GREEN}✓ Server-1 stopped${NC}"
echo ""

echo -e "${YELLOW}Step 5/6: Waiting for NGINX to detect failure (10 seconds)${NC}"
for i in {10..1}; do
    echo -ne "Waiting: $i seconds remaining...\r"
    sleep 1
done
echo ""
echo ""

echo -e "${YELLOW}Step 6/6: Testing if NGINX routes to other servers${NC}"
for i in {1..5}; do
    RESPONSE=$(curl -sf http://localhost:8080/actuator/health 2>/dev/null)
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✅ Attempt $i: API still accessible (failover working!)${NC}"
    else
        echo -e "${RED}❌ Attempt $i: API not accessible${NC}"
    fi
    sleep 1
done
echo ""

# Check NGINX logs for upstream errors
echo -e "${YELLOW}NGINX logs (last 20 lines):${NC}"
docker logs nginx-gateway --tail 20
echo ""

echo -e "${BLUE}To restart server-1:${NC}"
echo "  docker start server-1"
