#!/bin/bash

# Start Server 2 on port 8083 with Redis Pub/Sub (local-distributed mode)
echo "Starting Server 2 on port 8083..."
echo "Using Redis Pub/Sub for cross-server communication"

SERVER_PORT=8083 SERVER_NAME=server-2 java \
  -DPOSTGRES_HOST=localhost \
  -DPOSTGRES_PORT=5432 \
  -DPOSTGRES_DB=dueling_db \
  -DPOSTGRES_USER=dueling_user \
  -DPOSTGRES_PASSWORD=dueling_password \
  -DREDIS_HOST=localhost \
  -DREDIS_PORT=6379 \
  -Dspring.profiles.active=server,distributed,local-distributed \
  -jar dueling-server/target/dueling-server-1.0-SNAPSHOT.jar
