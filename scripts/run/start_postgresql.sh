#!/bin/bash

# Script para iniciar apenas o PostgreSQL
SCRIPT_DIR=$(cd "$(dirname -- "$0")" && cd .. && pwd)

GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}Starting PostgreSQL...${NC}"
cd "$SCRIPT_DIR/docker"

# Start only postgres
docker compose up -d postgres

echo ""
echo -e "${GREEN}✅ PostgreSQL started!${NC}"
echo "Status:"
docker ps --filter "name=postgres" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
echo ""
echo "Connection: postgresql://user:password@localhost:5432/dueling_db"
