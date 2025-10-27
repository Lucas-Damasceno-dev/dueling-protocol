#!/usr/bin/env bash

# Get the directory where this script is located
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Source common environment helper
source "$SCRIPT_DIR/../common_env.sh"

# Ensures the script stops if any command fails
set -e

echo "======================================================="
echo ">>> RUNNING MALFORMED INPUTS (MALICIOUS BOT) TEST"
echo "======================================================="

# Step 1: Images should already be built
echo ">>> [STEP 1/2] Using pre-built Docker images..."
echo ">>> (If needed, run: mvn clean package -DskipTests && docker compose build)"
echo ""

# Step 2: Run Malformed Inputs (Malicious Bot) test
echo ">>> [STEP 2/2] Running Malformed Inputs (Malicious Bot) test..."

# Set variables for docker compose
create_env_file .env maliciousbot ""

# Start containers and wait for the test to complete
echo "-------------------------------------------------------"
echo ">>> Running test: Malformed Inputs (Malicious Bot)"
echo "-------------------------------------------------------"
docker compose -f docker/docker-compose.yml --env-file .env up postgres redis-master redis-slave redis-sentinel-1 redis-sentinel-2 redis-sentinel-3 server-1 server-2 nginx-gateway client-1 -d
sleep 15 # Increase if tests need more time

# Display server logs for analysis
echo ">>> Server Logs for test 'Malformed Inputs (Malicious Bot)':"
docker compose -f docker/docker-compose.yml --env-file .env logs server-1 server-2

# Clean up environment for the next test
docker compose -f docker/docker-compose.yml --env-file .env down
# Remove the temporary .env file
rm -f .env
echo ">>> Test 'Malformed Inputs (Malicious Bot)' completed."
echo ""

echo "======================================================="
echo ">>> MALFORMED INPUTS (MALICIOUS BOT) TEST FINISHED"
echo "======================================================="
