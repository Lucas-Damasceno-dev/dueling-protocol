#!/bin/bash

# Start Game Local with Blockchain Integration
# This script starts the game in local mode + blockchain node via Docker

set -e

echo "🚀 Starting Dueling Protocol Local Mode + Blockchain"
echo "====================================================="
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
echo "🏗️  Step 2/4: Starting Infrastructure and Blockchain (Docker)..."
cd docker
docker compose up --build -d dueling-blockchain postgres redis-master redis-sentinel-1 redis-sentinel-2 redis-sentinel-3 redis-slave
echo "Waiting for services to become healthy..."

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
echo "🎮 Step 4/4: Starting Game Server (Java)..."

# Compile if needed
if [ ! -f "dueling-server/target/dueling-server-1.0-SNAPSHOT.jar" ]; then
    echo "Compiling project..."
    mvn clean package -DskipTests
fi

echo "Starting Server on port 8081..."
cd dueling-server
nohup java -DSERVER_PORT=8081 \
     -DSERVER_NAME=local-server \
     -DSERVER_URL=http://localhost:8081 \
     -DPOSTGRES_HOST=localhost \
     -DPOSTGRES_PORT=5432 \
     -DPOSTGRES_DB=dueling_db \
     -DPOSTGRES_USER=user \
     -DPOSTGRES_PASSWORD=password \
     -DREDIS_SENTINEL_MASTER=mymaster \
     -DREDIS_SENTINEL_NODES=localhost:26379,localhost:26380,localhost:26381 \
     -Dspring.profiles.active=local-dev,server,default \
     -jar target/dueling-server-1.0-SNAPSHOT.jar > ../logs/server-8081.log 2>&1 &
echo $! > ../logs/server-8081.pid
cd ..

sleep 10 # Give server time to start

echo ""
echo "✅ Verification:"
echo ""

# Check blockchain
if docker ps | grep -q "dueling-blockchain.*Up.*healthy"; then
    echo "✅ Blockchain Node: Running in Docker on http://localhost:8545"
else
    echo "❌ Blockchain Node: Not running or not healthy"
fi

# Check server
if lsof -i:8081 > /dev/null 2>&1; then
    echo "✅ Game Server: Running on http://localhost:8081"
else
    echo "❌ Game Server: Not responding. Check logs/server-8081.log"
fi

# Check PostgreSQL
if docker ps | grep -q "postgres.*Up.*healthy"; then
    echo "✅ PostgreSQL: Running"
else
    echo "❌ PostgreSQL: Not running or not healthy"
fi

# Check Redis
if docker ps | grep -q "redis-master.*Up.*healthy"; then
    echo "✅ Redis: Running"
else
    echo "❌ Redis: Not running or not healthy"
fi

echo ""
echo "====================================================="
echo "🎉 Local System Started!"
echo "====================================================="
echo ""
echo "📝 Access Points:"
echo "   Game Server:     http://localhost:8081"
echo "   WebSocket:       ws://localhost:8081/ws"
echo "   Blockchain Node: http://localhost:8545"
echo ""
echo "📊 Logs:"
echo "   Server:          tail -f logs/server-8081.log"
echo "   Deployment:      tail -f logs/blockchain-deploy.log"
echo "   Docker Logs:     docker compose -f docker/docker-compose.yml logs -f dueling-blockchain postgres redis-master"
echo ""
echo "🎮 To run client:"
echo "   ./menu.sh → 10"
echo ""
echo "🛑 To stop everything:"
echo "   ./scripts/stop-all-with-blockchain.sh"
echo ""