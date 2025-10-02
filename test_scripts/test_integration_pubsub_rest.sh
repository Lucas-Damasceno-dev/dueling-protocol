#!/usr/bin/env bash

# Ensures the script stops if any command fails
set -e

# Get the script's directory to build robust paths
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"
# The project root directory is one level above 'test_scripts'
PROJECT_ROOT="$SCRIPT_DIR/.."
DOCKER_COMPOSE_FILE="$PROJECT_ROOT/docker/docker-compose.yml"
ENV_FILE="$PROJECT_ROOT/.env"

echo "======================================================="
echo ">>> STARTING PUBSUB AND REST API INTEGRATION TESTS"
echo "(Assuming Docker images are already built)"
echo "======================================================="

# Helper function to run a specific test
run_test() {
  local test_name=$1
  local client_count=$2

  echo "-------------------------------------------------------"
  echo ">>> Running test: $test_name"
  echo "-------------------------------------------------------"

  # Create a temporary .env file for docker-compose
  echo "BOT_MODE=autobot" > "$ENV_FILE"
  echo "BOT_SCENARIO=" >> "$ENV_FILE"

  # Start containers
  docker-compose -f "$DOCKER_COMPOSE_FILE" --env-file "$ENV_FILE" up --scale client=$client_count --remove-orphans -d

  echo ">>> Waiting for services to initialize..."
  sleep 8
}

# Helper function to clean up the environment
cleanup() {
  echo ">>> Cleaning up Docker environment..."
  docker-compose -f "$DOCKER_COMPOSE_FILE" --env-file "$ENV_FILE" down --remove-orphans
  rm -f "$ENV_FILE"
}

# --- TEST 1: Publisher-Subscriber Matchmaking Notification ---
run_test "Publisher-Subscriber Test" 2

echo ">>> Verifying Pub/Sub matchmaking notifications..."
if docker-compose -f "$DOCKER_COMPOSE_FILE" --env-file "$ENV_FILE" logs server | grep -q "New match created"; then
  echo ">>> SUCCESS: Match creation message found in logs."
else
  echo ">>> FAILURE: Could not find match creation message in server logs."
  docker-compose -f "$DOCKER_COMPOSE_FILE" --env-file "$ENV_FILE" logs server
  cleanup
  exit 1
fi

cleanup
echo ">>> Test 'Publisher-Subscriber Test' completed."
echo ""


# --- TEST 2: REST API Server Synchronization ---
run_test "REST API Sync Test" 1

echo ">>> Testing REST API endpoint for matchmaking..."
curl -s -X POST -H "Content-Type: application/json" \
  -d '{"id":"player-api-123","nickname":"API_Player","health":100,"upgradePoints":0,"cardCollection":[]}' \
  http://localhost:8080/api/sync/matchmaking/enter

sleep 5 # Wait for the server to process the API request

if docker-compose -f "$DOCKER_COMPOSE_FILE" --env-file "$ENV_FILE" logs server | grep -q "Player player-api-123 added to matchmaking queue"; then
  echo ">>> SUCCESS: API player successfully added to matchmaking queue."
else
  echo ">>> FAILURE: Did not find log entry for API player in matchmaking."
  docker-compose -f "$DOCKER_COMPOSE_FILE" --env-file "$ENV_FILE" logs server
  cleanup
  exit 1
fi

cleanup
echo ">>> Test 'REST API Sync Test' completed."
echo ""

echo "======================================================="
echo ">>> INTEGRATION TESTS FINISHED"
echo "======================================================="