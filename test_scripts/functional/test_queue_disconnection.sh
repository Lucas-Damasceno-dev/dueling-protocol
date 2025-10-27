#!/usr/bin/env bash

# Ensures the script stops if any command fails
set -e

echo "======================================================="
echo ">>> RUNNING QUEUE DISCONNECTION TEST"
echo "======================================================="

# Get the script's directory to build robust paths
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"
PROJECT_ROOT="$SCRIPT_DIR/../.."
source "$PROJECT_ROOT/test_scripts/common_env.sh"

# Step 1: Build Docker images if needed (skip for now)
echo ">>> [STEP 1/2] Skipping build - assuming images are pre-built"
echo ""

# Step 2: Run Queue Disconnection test
echo ">>> [STEP 2/2] Running Queue Disconnection test..."

# Create env file with common helper
ENV_FILE="$PROJECT_ROOT/.env"
create_env_file "$ENV_FILE" "autobot" "matchmaking_disconnect"

# Start containers and wait for the test to complete
echo "-------------------------------------------------------"
echo ">>> Running test: Queue Disconnection"
echo "-------------------------------------------------------"
docker compose -f "$PROJECT_ROOT/docker/docker-compose.yml" --env-file "$ENV_FILE" up client-1 client-2 -d
sleep 15 # Increase if tests need more time

# Display server logs for analysis
echo ">>> Server Logs for test 'Queue Disconnection':"
docker compose -f "$PROJECT_ROOT/docker/docker-compose.yml" --env-file "$ENV_FILE" logs server-1 server-2

# Clean up environment for the next test
docker compose -f "$PROJECT_ROOT/docker/docker-compose.yml" --env-file "$ENV_FILE" down
# Remove the temporary .env file
rm -f "$ENV_FILE"
echo ">>> Test 'Queue Disconnection' completed."
echo ""

echo "======================================================="
echo ">>> QUEUE DISCONNECTION TEST FINISHED"
echo "======================================================="