#!/bin/bash
echo ">>> Starting API Gateway..."

# Build the gateway module if not already built
cd ../dueling-gateway
if [ ! -f "target/dueling-gateway-1.0-SNAPSHOT.jar" ]; then
    echo "Building gateway module..."
    mvn clean package -DskipTests
fi

# Run the gateway with correct configuration
echo ">>> Running Gateway on port 8080 connecting to server on port 8083"
java -DGATEWAY_HOST=localhost \
     -DGATEWAY_PORT=8080 \
     -DSERVER_HOST=localhost \
     -DSERVER_PORT=8083 \
     -jar target/dueling-gateway-1.0-SNAPSHOT.jar