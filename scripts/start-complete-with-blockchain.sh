#!/bin/bash

# Start Complete System with Blockchain Integration
# This script starts the game system + blockchain node via Docker

set -e

echo "🚀 Starting Dueling Protocol Complete System"
echo "============================================="
echo ""

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$PROJECT_DIR"

# Create logs directory if it doesn't exist
mkdir -p logs

echo "📋 Step 1/4: Stopping old services..."
bash ./scripts/deploy/stop_all_services.sh 2>/dev/null || true
cd "$PROJECT_DIR/docker"
docker-compose down -v --remove-orphans 2>/dev/null || true
cd "$PROJECT_DIR"
sleep 3

echo ""
echo "🏗️  Step 2/4: Starting Game System and Blockchain (Docker)..."
cd docker

if [ -f .env ]; then
    export $(cat .env | sed 's/#.*//g' | xargs)
fi

docker compose up --build -d

echo "Waiting for services to become healthy (this may take 2-3 minutes)..."
# Wait for blockchain to be healthy
MAX_RETRIES=30
RETRY=0
while [ $RETRY -lt $MAX_RETRIES ]; do
    if docker ps | grep -q "dueling-blockchain.*Up.*healthy"; then
        echo "✅ Blockchain container is healthy!"
        break
    fi
    RETRY=$((RETRY + 1))
    if [ $RETRY -eq $MAX_RETRIES ]; then
        echo "❌ Blockchain container failed to become healthy."
        docker logs dueling-blockchain
        exit 1
    fi
    echo "Waiting for blockchain container... ($RETRY/$MAX_RETRIES)"
    sleep 5
done

# Wait for nginx gateway to be healthy
MAX_RETRIES=60
RETRY=0
while [ $RETRY -lt $MAX_RETRIES ]; do
    if docker ps | grep -q "nginx-gateway.*Up.*healthy"; then
        echo "✅ NGINX Gateway is healthy!"
        break
    fi
    RETRY=$((RETRY + 1))
    if [ $RETRY -eq $MAX_RETRIES ]; then
        echo "❌ NGINX Gateway failed to become healthy."
        docker logs nginx-gateway
        exit 1
    fi
    echo "Waiting for NGINX gateway... ($RETRY/$MAX_RETRIES)"
    sleep 5
done

cd ..

echo ""
echo "🔨 Step 3/4: Deploying Smart Contracts..."
echo "Deploying contracts to the 'dueling-blockchain' container..."
docker compose -f docker/docker-compose.yml exec dueling-blockchain npx hardhat --network localhost run scripts/deploy.js 2>&1 | tee logs/blockchain-deploy.log

# Verify deployment succeeded
if [ $? -ne 0 ]; then
    echo "❌ Contract deployment failed!"
    echo "Check logs/blockchain-deploy.log for details"
    exit 1
fi
echo "✅ Smart Contracts deployed successfully."


echo ""
echo "✅ Step 4/4: Verification..."
echo ""

# Check blockchain
if docker ps | grep -q "dueling-blockchain.*Up.*healthy"; then
    echo "✅ Blockchain Node: Running in Docker on http://localhost:8545"
else
    echo "❌ Blockchain Node: Not running or not healthy"
fi

# Check Docker services
if docker ps | grep -q "nginx-gateway.*Up.*healthy"; then
    echo "✅ NGINX Gateway: Running on http://localhost:8080"
else
    echo "❌ NGINX Gateway: Not healthy"
fi

SERVER_COUNT=$(docker ps | grep "server-.*Up.*healthy" | wc -l)
if [ "$SERVER_COUNT" -gt 0 ]; then
    echo "✅ Game Servers: $SERVER_COUNT instances healthy"
else
    echo "❌ Game Servers: No healthy instances found"
fi

if docker ps | grep -q "postgres.*Up.*healthy"; then
    echo "✅ PostgreSQL: Running"
else
    echo "❌ PostgreSQL: Not running or not healthy"
fi

if docker ps | grep redis-master | grep -q "Up.*healthy"; then
    echo "✅ Redis: Running"
else
    echo "❌ Redis: Not running or not healthy"
fi

echo ""
echo "============================================="
echo "🎉 System Started!"
echo "============================================="
echo ""
echo "📝 Access Points:"
echo "   Game Gateway:    http://localhost:8080"
echo "   WebSocket:       ws://localhost:8080/ws"
echo "   Blockchain Node: http://localhost:8545"
echo ""
echo "📊 Logs:"
echo "   Deployment:      tail -f logs/blockchain-deploy.log"
echo "   Docker Logs:     docker compose -f docker/docker-compose.yml logs -f"
echo "   (specific):      docker logs -f <container_name>"
echo ""
echo "🔗 Blockchain Verification:"
echo "   ./menu.sh → 46 (Complete Verification)"
echo ""
echo "🛑 To stop everything:"
echo "   ./scripts/stop-all-with-blockchain.sh"
echo ""