#!/bin/bash
echo ">>> Starting GameServer with Docker..."
# Build and run the server module
cd ../dueling-server
mvn clean package -DskipTests
cd ../docker
docker build -t dueling-server .
docker run -p 7777:7777/tcp -p 7778:7778/udp dueling-server
