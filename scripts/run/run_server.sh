#!/usr/bin/env bash

# Run the server
echo "Make sure PostgreSQL and Redis are running before starting the server!"
echo "You can start them with: cd docker && docker compose up postgres redis-master redis-sentinel-1 redis-sentinel-2 redis-sentinel-3 redis-slave -d"
./scripts/run_server.sh