#!/bin/bash
echo ">>> Starting GameClient connecting through Gateway..."

# Build the client module from the project root
cd dueling-client
mvn clean package -DskipTests
cd ..

# Run the client connecting to the gateway
echo ">>> Running Client"
java -jar dueling-client/target/dueling-client-1.0-SNAPSHOT.jar