#!/usr/bin/env bash

# Ensures the script stops if any command fails
set -e

echo "======================================================="
echo ">>> RUNNING PERSISTENCE RACE CONDITION TEST"
echo "======================================================="

# Step 1: Compile and build Docker images (only needs to be done once)
echo ">>> [STEP 1/2] Building Docker images..."
./scripts/build.sh
# Create a temporary .env file with default values for docker compose build
echo "BOT_MODE=autobot" > .env
echo "BOT_SCENARIO=" >> .env
docker compose -f docker/docker-compose.yml --env-file .env build
rm -f .env
echo ">>> Images built successfully."
echo ""

# Step 2: Run Persistence Race Condition test
echo ">>> [STEP 2/2] Running Persistence Race Condition test..."

# Set variables for docker compose (using env-file approach to avoid warnings)
echo "BOT_MODE=autobot" > .env
echo "BOT_SCENARIO=race_condition" >> .env

# Start containers and wait for the test to complete
echo "-------------------------------------------------------"
echo ">>> Running test: Persistence Race Condition"
echo "-------------------------------------------------------"
docker compose -f docker/docker-compose.yml --env-file .env up --scale client=1 --remove-orphans -d
sleep 15 # Increase if tests need more time

# Display server logs for analysis
echo ">>> Server Logs for test 'Persistence Race Condition':"
docker compose -f docker/docker-compose.yml --env-file .env logs server

# Clean up environment for the next test
docker compose -f docker/docker-compose.yml --env-file .env down
# Remove the temporary .env file
rm -f .env
echo ">>> Test 'Persistence Race Condition' completed."
echo ""

echo "======================================================="
echo ">>> PERSISTENCE RACE CONDITION TEST FINISHED"
echo "======================================================="