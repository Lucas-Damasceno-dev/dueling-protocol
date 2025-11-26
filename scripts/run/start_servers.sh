#!/bin/bash

# Script para iniciar servidores de jogo específicos
SCRIPT_DIR=$(cd "$(dirname -- "$0")" && cd .. && pwd)

GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${BLUE}Starting Game Servers...${NC}"
echo ""

# Check dependencies
echo -e "${YELLOW}Checking dependencies...${NC}"
DEPS_OK=true

if ! docker ps | grep -q "postgres"; then
    echo -e "${RED}❌ PostgreSQL is not running${NC}"
    DEPS_OK=false
fi

if ! docker ps | grep -q "redis-master"; then
    echo -e "${RED}❌ Redis Master is not running${NC}"
    DEPS_OK=false
fi

if ! docker ps | grep -q "redis-sentinel"; then
    echo -e "${RED}❌ Redis Sentinels are not running${NC}"
    DEPS_OK=false
fi

if ! docker ps | grep -q "dueling-blockchain"; then
    echo -e "${RED}❌ Blockchain is not running${NC}"
    DEPS_OK=false
fi

if [ "$DEPS_OK" = false ]; then
    echo ""
    echo -e "${RED}⚠️  Some dependencies are missing${NC}"
    read -p "Continue anyway? (y/N): " confirm
    if [[ ! "$confirm" =~ ^[Yy]$ ]]; then
        echo "Cancelled. Start dependencies first or use menu option 50 for full system"
        exit 0
    fi
fi

echo ""
echo "Which servers do you want to start?"
echo "1. Start ALL servers (server-1, server-2, server-3, server-4)"
echo "2. Start server-1 only"
echo "3. Start server-2 only"
echo "4. Start server-3 only"
echo "5. Start server-4 only"
echo "6. Start specific servers (choose multiple)"
echo ""
read -p "Choose option (1-6): " choice

cd "$SCRIPT_DIR/docker"

case $choice in
    1)
        echo -e "${BLUE}Starting all servers...${NC}"
        docker compose up -d server-1 server-2 server-3 server-4
        ;;
    2)
        echo -e "${BLUE}Starting server-1...${NC}"
        docker compose up -d server-1
        ;;
    3)
        echo -e "${BLUE}Starting server-2...${NC}"
        docker compose up -d server-2
        ;;
    4)
        echo -e "${BLUE}Starting server-3...${NC}"
        docker compose up -d server-3
        ;;
    5)
        echo -e "${BLUE}Starting server-4...${NC}"
        docker compose up -d server-4
        ;;
    6)
        echo "Enter server numbers separated by space (e.g., 1 3 4):"
        read -p "Servers: " servers
        SERVICES=""
        for num in $servers; do
            SERVICES="$SERVICES server-$num"
        done
        echo -e "${BLUE}Starting servers: $SERVICES${NC}"
        docker compose up -d $SERVICES
        ;;
    *)
        echo -e "${RED}Invalid option${NC}"
        exit 1
        ;;
esac

echo ""
echo -e "${GREEN}✅ Game servers starting...${NC}"
echo "Waiting for servers to initialize (15 seconds)..."
sleep 15

echo ""
echo "Status:"
docker ps --filter "name=server-" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
echo ""
echo "Server ports: 8081-8084"
