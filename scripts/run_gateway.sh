#!/bin/bash
echo ">>> Starting API Gateway..."

# Build the gateway module
cd ../dueling-gateway
mvn clean package -DskipTests

# Run the gateway
echo ">>> Running Gateway on port 8080"
java -jar target/dueling-gateway-1.0-SNAPSHOT.jar