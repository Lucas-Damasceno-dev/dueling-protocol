#!/bin/bash
echo ">>> Starting GameClient in Docker (integrated with full system)..."

# Check if system is running
if ! docker ps | grep -q "nginx-gateway"; then
    echo ""
    echo "⚠️  System is not running!"
    echo "Please start the complete system first with option 5 or 6 from menu"
    echo ""
    read -p "Press Enter to continue..."
    exit 1
fi

# Build the client if needed
cd "$(dirname "$0")/.."
if [ ! -f "dueling-client/target/dueling-client-1.0-SNAPSHOT.jar" ]; then
    echo "Building client module..."
    mvn clean package -pl dueling-client -am -DskipTests
fi

# Generate unique container name with timestamp
TIMESTAMP=$(date +%s%N | cut -b1-13)
CONTAINER_NAME="dueling-client-$TIMESTAMP"

# Run client in Docker container connected to dueling-network
echo ">>> Running Client in Docker container: $CONTAINER_NAME"
echo ">>> Connecting to gateway at ws://nginx-gateway:8080/ws"
echo ""

docker run -it --rm \
    --name "$CONTAINER_NAME" \
    --network docker_dueling-network \
    -v "$(pwd)/dueling-client/target:/app" \
    -e GATEWAY_HOST=nginx-gateway \
    -e GATEWAY_PORT=8080 \
    -e WEBSOCKET_URL=ws://nginx-gateway:8080/ws \
    eclipse-temurin:21-jre \
    java -jar /app/dueling-client-1.0-SNAPSHOT.jar