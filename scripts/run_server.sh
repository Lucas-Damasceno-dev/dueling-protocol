#!/bin/bash
echo ">>> Starting GameServer in Docker (integrated with full system)..."

# Check if infrastructure is running
if ! docker ps | grep -q "postgres"; then
    echo ""
    echo "⚠️  Infrastructure is not running!"
    echo "Please start the system first with option 5 or 6 from menu"
    echo ""
    read -p "Press Enter to continue..."
    exit 1
fi

# Build the server if needed
cd "$(dirname "$0")/.."
if [ ! -f "dueling-server/target/dueling-server-1.0-SNAPSHOT.jar" ]; then
    echo "Building server module..."
    mvn clean package -pl dueling-server -am -DskipTests
fi

# Get next available port
NEXT_PORT=8091
while docker ps | grep -q ":$NEXT_PORT->"; do
    NEXT_PORT=$((NEXT_PORT + 1))
done

SERVER_NAME="server-debug-$NEXT_PORT"

echo ">>> Starting server: $SERVER_NAME on port $NEXT_PORT"
echo ">>> Connected to blockchain, PostgreSQL, and Redis"
echo ""

# Run server in Docker container
docker run -it --rm \
    --name "$SERVER_NAME" \
    --network docker_dueling-network \
    -p "$NEXT_PORT:8080" \
    -v "$(pwd)/dueling-server/target:/app" \
    -e SERVER_PORT=8080 \
    -e SERVER_NAME="$SERVER_NAME" \
    -e SERVER_URL="http://$SERVER_NAME:8080" \
    -e REDIS_SENTINEL_MASTER=mymaster \
    -e REDIS_SENTINEL_NODES=redis-sentinel-1:26379,redis-sentinel-2:26379,redis-sentinel-3:26379 \
    -e POSTGRES_HOST=postgres \
    -e POSTGRES_PORT=5432 \
    -e POSTGRES_DB=dueling_db \
    -e POSTGRES_USER=user \
    -e POSTGRES_PASSWORD=password \
    -e BLOCKCHAIN_NODE_URL=http://dueling-blockchain:8545 \
    -e ASSET_CONTRACT=0x610178dA211FEF7D417bC0e6FeD39F05609AD788 \
    -e STORE_CONTRACT=0xB7f8BC63BbcaD18155201308C8f3540b07f84F5e \
    -e TRADE_CONTRACT=0x0DCd1Bf9A1b36cE34237eEaFef220932846BCD82 \
    -e MATCH_CONTRACT=0x9A676e781A523b5d0C0e43731313A708CB607508 \
    eclipse-temurin:21-jre \
    java -jar /app/dueling-server-1.0-SNAPSHOT.jar \
        --spring.application.name=GAME-SERVER \
        --spring.profiles.active=server,distributed,distributed-db