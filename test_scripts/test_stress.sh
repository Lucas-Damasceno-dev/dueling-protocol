#!/usr/bin/env bash

# Ensures the script stops if any command fails
set -e

echo "======================================================="
echo ">>> RUNNING STRESS TEST"
echo "======================================================="

CLIENT_COUNT=10
TEST_DURATION=30 # in seconds

echo ">>> [STEP 1/3] Ensuring the project is compiled..."
./scripts/build.sh

echo ""
echo ">>> [STEP 2/3] Building Docker images..."
# Set environment variables to avoid warnings
echo "BOT_MODE=autobot" > .env
echo "BOT_SCENARIO=" >> .env
docker compose -f docker/docker-compose.yml --env-file .env build

echo ""
echo ">>> [STEP 3/3] Starting stress test with 1 server and $CLIENT_COUNT clients..."
echo ">>> The test will run for $TEST_DURATION seconds."
docker compose -f docker/docker-compose.yml --env-file .env up --scale client=$CLIENT_COUNT --remove-orphans &

# Saves the docker compose process PID
COMPOSE_PID=$!

# Waits for the test duration
sleep $TEST_DURATION

# Terminates docker compose
kill $COMPOSE_PID

echo ""
echo ">>> Test finished. Cleaning up containers..."
docker compose -f docker/docker-compose.yml --env-file .env down
# Clean up the temporary .env file
rm -f .env

echo "======================================================="
echo ">>> STRESS TEST FINISHED"
echo "======================================================="