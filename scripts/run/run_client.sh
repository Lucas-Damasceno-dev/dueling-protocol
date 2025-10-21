#!/bin/bash
echo ">>> Starting GameClient connecting through Gateway..."

# Build the client module from the project root if not already built
cd dueling-client
if [ ! -f "target/dueling-client-1.0-SNAPSHOT.jar" ]; then
    echo "Building client module..."
    cd ..
    mvn clean package -DskipTests
    cd dueling-client
fi

# Run the client connecting to the gateway
echo ">>> Running Client connecting to gateway at ws://localhost:8080/ws"
java -DGATEWAY_HOST=localhost \
     -DGATEWAY_PORT=8080 \
     -DWEBSOCKET_URL=ws://localhost:8080/ws \
     -jar target/dueling-client-1.0-SNAPSHOT.jar