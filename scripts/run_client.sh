#!/bin/bash
echo ">>> Starting GameClient connecting through Gateway..."

# Build the client module
cd ../dueling-client
mvn clean package -DskipTests

# Run the client connecting to the gateway
echo ">>> Running Client - connecting to gateway at localhost:8080"
java -jar target/dueling-client-1.0-SNAPSHOT.jar