#!/bin/bash

# Script para testar failover do Redis Sentinel
SCRIPT_DIR=$(cd "$(dirname -- "$0")" && cd .. && pwd)

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}╔════════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║       Redis Sentinel Failover Test                    ║${NC}"
echo -e "${BLUE}╚════════════════════════════════════════════════════════╝${NC}"
echo ""

# Check if Redis containers are running
if ! docker ps | grep -q "redis-master"; then
    echo -e "${RED}❌ Redis master is not running!${NC}"
    echo "Start system first with menu option 50"
    exit 1
fi

echo -e "${YELLOW}Step 1/5: Checking current Redis master status${NC}"
CURRENT_MASTER=$(docker exec redis-sentinel-1 redis-cli -p 26379 SENTINEL get-master-addr-by-name mymaster 2>/dev/null)
echo "Current master: $CURRENT_MASTER"
echo ""

echo -e "${YELLOW}Step 2/5: Checking Sentinel configuration${NC}"
docker exec redis-sentinel-1 redis-cli -p 26379 SENTINEL masters | head -20
echo ""

echo -e "${YELLOW}Step 3/5: Stopping redis-master to trigger failover${NC}"
docker stop redis-master
echo -e "${GREEN}✓ Redis master stopped${NC}"
echo ""

echo -e "${YELLOW}Step 4/5: Waiting for Sentinel failover (40 seconds)${NC}"
for i in {40..1}; do
    echo -ne "Waiting: $i seconds remaining...\r"
    sleep 1
done
echo ""
echo ""

echo -e "${YELLOW}Step 5/5: Checking new master${NC}"
NEW_MASTER=$(docker exec redis-sentinel-1 redis-cli -p 26379 SENTINEL get-master-addr-by-name mymaster 2>/dev/null)
echo "New master: $NEW_MASTER"
echo ""

# Check if failover happened
if [ "$NEW_MASTER" != "$CURRENT_MASTER" ]; then
    echo -e "${GREEN}╔════════════════════════════════════════════════════════╗${NC}"
    echo -e "${GREEN}║  ✅ FAILOVER SUCCESSFUL!                              ║${NC}"
    echo -e "${GREEN}╚════════════════════════════════════════════════════════╝${NC}"
    echo ""
    echo "Previous master: $CURRENT_MASTER"
    echo "New master:      $NEW_MASTER"
    echo ""
    echo -e "${YELLOW}Sentinel logs:${NC}"
    docker logs redis-sentinel-1 --tail 30
else
    echo -e "${RED}╔════════════════════════════════════════════════════════╗${NC}"
    echo -e "${RED}║  ❌ FAILOVER DID NOT OCCUR                            ║${NC}"
    echo -e "${RED}╚════════════════════════════════════════════════════════╝${NC}"
    echo ""
    echo "Master is still: $NEW_MASTER"
    echo ""
    echo -e "${YELLOW}Checking sentinel logs for errors:${NC}"
    docker logs redis-sentinel-1 --tail 50
fi

echo ""
echo -e "${BLUE}To restart redis-master:${NC}"
echo "  docker start redis-master"
