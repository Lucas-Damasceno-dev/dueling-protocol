#!/usr/bin/env bash

# Ensures the script stops if any command fails
set -e

echo "======================================================="
echo ">>> RUNNING MALFORMED INPUTS (MALICIOUS BOT) TEST"
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

# Step 2: Run Malformed Inputs (Malicious Bot) test
echo ">>> [STEP 2/2] Running Malformed Inputs (Malicious Bot) test..."

# Set variables for docker compose (using env-file approach to avoid warnings)
echo "BOT_MODE=maliciousbot" > .env
echo "BOT_SCENARIO=" >> .env

# Start containers and wait for the test to complete
echo "-------------------------------------------------------"
echo ">>> Running test: Malformed Inputs (Malicious Bot)"
echo "-------------------------------------------------------"
docker compose -f docker/docker-compose.yml --env-file .env up --scale client=1 --remove-orphans -d
sleep 15 # Increase if tests need more time

# Display server logs for analysis
echo ">>> Server Logs for test 'Malformed Inputs (Malicious Bot)':"
docker compose -f docker/docker-compose.yml --env-file .env logs server

# Clean up environment for the next test
docker compose -f docker/docker-compose.yml --env-file .env down
# Remove the temporary .env file
rm -f .env
echo ">>> Test 'Malformed Inputs (Malicious Bot)' completed."
echo ""

echo "======================================================="
echo ">>> MALFORMED INPUTS (MALICIOUS BOT) TEST FINISHED"
echo "======================================================="