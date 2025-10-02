#!/usr/bin/env bash

# Ensures the script stops if any command fails
set -e

echo "======================================================="
echo ">>> RUNNING SIMULTANEOUS PLAY TEST"
echo "======================================================="

# Step 1: Compile and build Docker images (only needs to be done once)
echo ">>> [STEP 1/2] Building Docker images..."
./scripts/build.sh
# Create a temporary .env file with default values for docker-compose build
echo "BOT_MODE=autobot" > .env
echo "BOT_SCENARIO=" >> .env
docker-compose -f docker/docker-compose.yml --env-file .env build
rm -f .env
echo ">>> Images built successfully."
echo ""

# Step 2: Run Simultaneous Play test
echo ">>> [STEP 2/2] Running Simultaneous Play test..."

# Set variables for docker-compose (using env-file approach to avoid warnings)
echo "BOT_MODE=autobot" > .env
echo "BOT_SCENARIO=simultaneous_play" >> .env

# Start containers and wait for the test to complete
echo "-------------------------------------------------------"
echo ">>> Running test: Simultaneous Play"
echo "-------------------------------------------------------"
docker-compose -f docker/docker-compose.yml --env-file .env up --scale client=2 --remove-orphans -d
sleep 15 # Increase if tests need more time

# Display server logs for analysis
echo ">>> Server Logs for test 'Simultaneous Play':"
docker-compose -f docker/docker-compose.yml --env-file .env logs server

# Clean up environment for the next test
docker-compose -f docker/docker-compose.yml --env-file .env down
# Remove the temporary .env file
rm -f .env
echo ">>> Test 'Simultaneous Play' completed."
echo ""

echo "======================================================="
echo ">>> SIMULTANEOUS PLAY TEST FINISHED"
echo "======================================================="