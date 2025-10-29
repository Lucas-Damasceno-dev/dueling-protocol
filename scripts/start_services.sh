#!/bin/bash

# Start Dueling Protocol Services
# This script ensures services are properly started and ready

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}════════════════════════════════════════${NC}"
echo -e "${BLUE}  Dueling Protocol - Starting Services  ${NC}"
echo -e "${BLUE}════════════════════════════════════════${NC}"
echo

# Get project root
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

cd "$PROJECT_DIR"

# Check if JARs are built
if [ ! -f "dueling-gateway/target/dueling-gateway-1.0-SNAPSHOT.jar" ] || \
   [ ! -f "dueling-server/target/dueling-server-1.0-SNAPSHOT.jar" ]; then
    echo -e "${YELLOW}Building project first...${NC}"
    mvn clean package -DskipTests
    echo -e "${GREEN}✓ Build complete${NC}"
    echo
fi

# Navigate to docker directory
cd docker

# Check if .env exists
if [ ! -f .env ]; then
    echo -e "${YELLOW}Creating .env file from .env.example...${NC}"
    cp .env.example .env
fi

# Stop any existing services
echo -e "${YELLOW}Stopping existing services...${NC}"
docker compose down --remove-orphans 2>/dev/null || true
echo

# Start services
echo -e "${GREEN}Starting Docker services...${NC}"
docker compose up -d

# Wait for services to be healthy
echo -e "${YELLOW}Waiting for services to be ready...${NC}"
echo -e "${YELLOW}This may take 30-60 seconds...${NC}"

MAX_WAIT=120
ELAPSED=0

while [ $ELAPSED -lt $MAX_WAIT ]; do
    sleep 5
    ELAPSED=$((ELAPSED + 5))
    
    # Check if key services are running
    POSTGRES=$(docker compose ps postgres | grep -c "Up" || echo "0")
    REDIS=$(docker compose ps redis-master | grep -c "Up" || echo "0")
    GATEWAY=$(docker compose ps nginx-gateway | grep -c "Up" || echo "0")
    
    if [ "$POSTGRES" -gt 0 ] && [ "$REDIS" -gt 0 ] && [ "$GATEWAY" -gt 0 ]; then
        echo -e "${GREEN}✓ Core services are running${NC}"
        break
    fi
    
    echo -n "."
done

echo
echo

# Display service status
echo -e "${BLUE}════════════════════════════════════════${NC}"
echo -e "${GREEN}Services Status:${NC}"
echo -e "${BLUE}════════════════════════════════════════${NC}"
docker compose ps
echo

# Display connection information
echo -e "${BLUE}════════════════════════════════════════${NC}"
echo -e "${GREEN}Connection Information:${NC}"
echo -e "${BLUE}════════════════════════════════════════${NC}"
echo -e "${YELLOW}Local Connection:${NC}"
echo "  HTTP API: http://localhost:8080"
echo "  WebSocket: ws://localhost:8080/ws"
echo
echo -e "${YELLOW}Multi-PC Network Connection:${NC}"
echo "  1. Get your local IP address:"
echo "     ip addr show | grep 'inet ' | grep -v '127.0.0.1'"
echo "  2. Ensure firewall allows port 8080"
echo "  3. Clients connect to: http://YOUR_IP:8080"
echo
echo -e "${YELLOW}Available Endpoints:${NC}"
echo "  POST /api/auth/register - Register new user"
echo "  POST /api/auth/login - Login"
echo "  WebSocket /ws - Game connection (requires token)"
echo
echo -e "${BLUE}════════════════════════════════════════${NC}"
echo -e "${GREEN}✓ Services Started Successfully!${NC}"
echo -e "${BLUE}════════════════════════════════════════${NC}"
echo
echo -e "${YELLOW}To stop services: ./scripts/deploy/stop_all_services.sh${NC}"
echo -e "${YELLOW}To view logs: docker compose -f docker/docker-compose.yml logs -f${NC}"
echo
