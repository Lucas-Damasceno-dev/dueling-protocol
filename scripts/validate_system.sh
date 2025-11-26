#!/bin/bash

# Script de validação completa do sistema
SCRIPT_DIR=$(cd "$(dirname -- "$0")" && cd .. && pwd)

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
BOLD='\033[1m'
NC='\033[0m'

echo -e "${CYAN}${BOLD}"
echo "╔════════════════════════════════════════════════════════════════╗"
echo "║    DUELING PROTOCOL - VALIDAÇÃO COMPLETA DE RESILIÊNCIA       ║"
echo "╚════════════════════════════════════════════════════════════════╝"
echo -e "${NC}"
echo ""

TESTS_PASSED=0
TESTS_FAILED=0

# Test 1: System Status
echo -e "${BLUE}═══ Test 1: System Status ═══${NC}"
if docker ps | grep -q "nginx-gateway" && \
   docker ps | grep -q "server-" && \
   docker ps | grep -q "redis-" && \
   docker ps | grep -q "postgres"; then
    echo -e "${GREEN}✅ All core services are running${NC}"
    TESTS_PASSED=$((TESTS_PASSED + 1))
else
    echo -e "${RED}❌ Some core services are not running${NC}"
    TESTS_FAILED=$((TESTS_FAILED + 1))
fi
echo ""

# Test 2: NGINX Configuration
echo -e "${BLUE}═══ Test 2: NGINX Load Balancing Configuration ═══${NC}"
if docker exec nginx-gateway cat /etc/nginx/nginx.conf | grep -q "upstream game_servers"; then
    echo -e "${GREEN}✅ NGINX configured with upstream pool${NC}"
    SERVERS_COUNT=$(docker exec nginx-gateway cat /etc/nginx/nginx.conf | grep "server server-" | wc -l)
    echo "   Configured servers: $SERVERS_COUNT"
    TESTS_PASSED=$((TESTS_PASSED + 1))
else
    echo -e "${RED}❌ NGINX not configured with upstream${NC}"
    TESTS_FAILED=$((TESTS_FAILED + 1))
fi
echo ""

# Test 3: NGINX Health
echo -e "${BLUE}═══ Test 3: NGINX Gateway Health ═══${NC}"
NGINX_HEALTH=$(curl -sf http://localhost:8080/nginx-health)
if [ "$NGINX_HEALTH" = "healthy" ]; then
    echo -e "${GREEN}✅ NGINX gateway is healthy${NC}"
    TESTS_PASSED=$((TESTS_PASSED + 1))
else
    echo -e "${RED}❌ NGINX gateway health check failed${NC}"
    TESTS_FAILED=$((TESTS_FAILED + 1))
fi
echo ""

# Test 4: Backend API Access
echo -e "${BLUE}═══ Test 4: Backend API Accessibility ═══${NC}"
API_RESPONSE=$(curl -sf http://localhost:8080/actuator/health)
if echo "$API_RESPONSE" | grep -q "UP"; then
    echo -e "${GREEN}✅ Backend API is accessible through NGINX${NC}"
    echo "   Response: $API_RESPONSE"
    TESTS_PASSED=$((TESTS_PASSED + 1))
else
    echo -e "${RED}❌ Backend API not accessible${NC}"
    TESTS_FAILED=$((TESTS_FAILED + 1))
fi
echo ""

# Test 5: Redis Sentinel Configuration
echo -e "${BLUE}═══ Test 5: Redis Sentinel Cluster ═══${NC}"
SENTINEL_COUNT=$(docker ps --filter "name=redis-sentinel" | grep -c "redis-sentinel")
if [ "$SENTINEL_COUNT" -ge 3 ]; then
    echo -e "${GREEN}✅ Redis Sentinel cluster operational ($SENTINEL_COUNT sentinels)${NC}"
    MASTER_INFO=$(docker exec redis-sentinel-1 redis-cli -p 26379 SENTINEL get-master-addr-by-name mymaster 2>/dev/null)
    echo "   Current master: $MASTER_INFO"
    TESTS_PASSED=$((TESTS_PASSED + 1))
else
    echo -e "${RED}❌ Redis Sentinel cluster incomplete${NC}"
    TESTS_FAILED=$((TESTS_FAILED + 1))
fi
echo ""

# Test 6: Game Servers Count
echo -e "${BLUE}═══ Test 6: Game Servers Availability ═══${NC}"
SERVERS_UP=$(docker ps --filter "name=server-" --filter "health=healthy" | grep -c "server-")
echo "   Healthy servers: $SERVERS_UP/4"
if [ "$SERVERS_UP" -ge 2 ]; then
    echo -e "${GREEN}✅ Sufficient game servers available${NC}"
    TESTS_PASSED=$((TESTS_PASSED + 1))
else
    echo -e "${RED}❌ Insufficient game servers${NC}"
    TESTS_FAILED=$((TESTS_FAILED + 1))
fi
echo ""

# Test 7: Database Connectivity
echo -e "${BLUE}═══ Test 7: PostgreSQL Connectivity ═══${NC}"
if docker exec postgres pg_isready -U user -d dueling_db > /dev/null 2>&1; then
    echo -e "${GREEN}✅ PostgreSQL is ready and accepting connections${NC}"
    TESTS_PASSED=$((TESTS_PASSED + 1))
else
    echo -e "${RED}❌ PostgreSQL not ready${NC}"
    TESTS_FAILED=$((TESTS_FAILED + 1))
fi
echo ""

# Test 8: Blockchain Node
echo -e "${BLUE}═══ Test 8: Blockchain Node Availability ═══${NC}"
if curl -sf http://localhost:8545 -X POST -H "Content-Type: application/json" \
   --data '{"jsonrpc":"2.0","method":"eth_blockNumber","params":[],"id":1}' > /dev/null 2>&1; then
    echo -e "${GREEN}✅ Blockchain node is responding${NC}"
    TESTS_PASSED=$((TESTS_PASSED + 1))
else
    echo -e "${YELLOW}⚠️  Blockchain node not responding (may be starting)${NC}"
    TESTS_FAILED=$((TESTS_FAILED + 1))
fi
echo ""

# Summary
echo -e "${CYAN}${BOLD}"
echo "╔════════════════════════════════════════════════════════════════╗"
echo "║                        TEST SUMMARY                            ║"
echo "╚════════════════════════════════════════════════════════════════╝"
echo -e "${NC}"
echo ""
echo -e "${GREEN}Tests Passed: $TESTS_PASSED${NC}"
echo -e "${RED}Tests Failed: $TESTS_FAILED${NC}"
echo ""

TOTAL_TESTS=$((TESTS_PASSED + TESTS_FAILED))
PASS_RATE=$((TESTS_PASSED * 100 / TOTAL_TESTS))

if [ $TESTS_FAILED -eq 0 ]; then
    echo -e "${GREEN}${BOLD}╔════════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${GREEN}${BOLD}║              ✅ ALL TESTS PASSED (100%)                       ║${NC}"
    echo -e "${GREEN}${BOLD}║          System is ready for production use!                  ║${NC}"
    echo -e "${GREEN}${BOLD}╚════════════════════════════════════════════════════════════════╝${NC}"
    exit 0
elif [ $PASS_RATE -ge 75 ]; then
    echo -e "${YELLOW}${BOLD}╔════════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${YELLOW}${BOLD}║          ⚠️  MOST TESTS PASSED ($PASS_RATE%)                        ║${NC}"
    echo -e "${YELLOW}${BOLD}║          System operational with minor issues                 ║${NC}"
    echo -e "${YELLOW}${BOLD}╚════════════════════════════════════════════════════════════════╝${NC}"
    exit 1
else
    echo -e "${RED}${BOLD}╔════════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${RED}${BOLD}║              ❌ CRITICAL FAILURES DETECTED                     ║${NC}"
    echo -e "${RED}${BOLD}║          System requires immediate attention!                  ║${NC}"
    echo -e "${RED}${BOLD}╚════════════════════════════════════════════════════════════════╝${NC}"
    exit 2
fi
