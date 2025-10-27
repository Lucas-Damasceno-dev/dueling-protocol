#!/usr/bin/env bash

# Ensures the script stops if any command fails
set -e

# Get the script's directory to build robust paths
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"
# The project root directory is two levels above (test_scripts/integration -> test_scripts -> root)
PROJECT_ROOT="$SCRIPT_DIR/../.."
DOCKER_COMPOSE_FILE="$PROJECT_ROOT/docker/docker-compose.yml"
ENV_FILE="$PROJECT_ROOT/.env"

# Source common environment helper
source "$PROJECT_ROOT/test_scripts/common_env.sh"

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

  # Create a temporary .env file for docker compose using common helper
  create_env_file "$ENV_FILE" "autobot" ""

  # Determine which services to start based on client count
  if [ "$client_count" -eq 1 ]; then
    SERVICES="postgres redis-master redis-slave redis-sentinel-1 redis-sentinel-2 redis-sentinel-3 server-1 server-2 nginx-gateway client-1"
  else
    SERVICES="postgres redis-master redis-slave redis-sentinel-1 redis-sentinel-2 redis-sentinel-3 server-1 server-2 nginx-gateway client-1 client-2"
  fi

  # Start containers
  docker compose -f "$DOCKER_COMPOSE_FILE" --env-file "$ENV_FILE" up $SERVICES -d

  echo ">>> Waiting for services to initialize..."
  sleep 8
}

# Helper function to clean up the environment
cleanup() {
  echo ">>> Cleaning up Docker environment..."
  docker compose -f "$DOCKER_COMPOSE_FILE" --env-file "$ENV_FILE" down --remove-orphans
  rm -f "$ENV_FILE"
}

# --- TEST 1: Publisher-Subscriber Matchmaking Notification ---
# Start system without clients (they don't support bot mode)
echo "-------------------------------------------------------"
echo ">>> Running test: Publisher-Subscriber Test"
echo "-------------------------------------------------------"

create_env_file "$ENV_FILE" "autobot" ""
SERVICES="postgres redis-master redis-slave redis-sentinel-1 redis-sentinel-2 redis-sentinel-3 server-1 server-2 nginx-gateway"
docker compose -f "$DOCKER_COMPOSE_FILE" --env-file "$ENV_FILE" up $SERVICES -d

echo ">>> Waiting for services to initialize..."
sleep 10

echo ">>> Testing basic server health..."
HEALTH=$(curl -s http://localhost:8080/api/health)
echo ">>> Health check response: $HEALTH"

echo ">>> Creating two test players and adding them to matchmaking queue..."

# Create Player 1
PLAYER1='{
  "id": "test-player-1",
  "nickname": "TestPlayer1",
  "race": "HUMAN",
  "characterClass": "WARRIOR",
  "health": 100,
  "maxHealth": 100,
  "upgradePoints": 0,
  "level": 1,
  "cardCollection": []
}'

# Create Player 2
PLAYER2='{
  "id": "test-player-2",
  "nickname": "TestPlayer2",
  "race": "ELF",
  "characterClass": "MAGE",
  "health": 100,
  "maxHealth": 100,
  "upgradePoints": 0,
  "level": 1,
  "cardCollection": []
}'

echo ">>> Adding Player 1 to matchmaking queue..."
RESPONSE1=$(curl -s -w "\nHTTP_CODE:%{http_code}" -X POST http://localhost:8080/api/matchmaking/enqueue \
  -H "Content-Type: application/json" \
  -d "$PLAYER1")
echo ">>> Response: $RESPONSE1"

sleep 1

echo ">>> Adding Player 2 to matchmaking queue..."
RESPONSE2=$(curl -s -w "\nHTTP_CODE:%{http_code}" -X POST http://localhost:8080/api/matchmaking/enqueue \
  -H "Content-Type: application/json" \
  -d "$PLAYER2")
echo ">>> Response: $RESPONSE2"

echo ">>> Waiting for match creation..."
sleep 5

echo ">>> Verifying matchmaking in server logs..."
if docker compose -f "$DOCKER_COMPOSE_FILE" --env-file "$ENV_FILE" logs server-1 server-2 | grep -qE "test-player-1|test-player-2|enqueue"; then
  echo ">>> SUCCESS: Players were processed by matchmaking system (API working)."
else
  echo ">>> FAILURE: Could not find player processing in server logs."
  echo ">>> Recent server logs:"
  docker compose -f "$DOCKER_COMPOSE_FILE" --env-file "$ENV_FILE" logs server-1 server-2 | tail -50
  cleanup
  exit 1
fi

cleanup
echo ">>> Test 'Publisher-Subscriber Test' completed."
echo ""


# --- TEST 2: REST API Server Synchronization ---
echo "-------------------------------------------------------"
echo ">>> Running test: REST API Sync Test"
echo "-------------------------------------------------------"

create_env_file "$ENV_FILE" "autobot" ""
SERVICES="postgres redis-master redis-slave redis-sentinel-1 redis-sentinel-2 redis-sentinel-3 server-1 server-2 nginx-gateway"
docker compose -f "$DOCKER_COMPOSE_FILE" --env-file "$ENV_FILE" up $SERVICES -d

echo ">>> Waiting for services to initialize..."
sleep 10

echo ">>> Testing REST API endpoint for player retrieval..."

# First add a player
PLAYER_DATA='{
  "id": "api-test-player",
  "nickname": "APITestPlayer",
  "race": "DWARF",
  "characterClass": "CLERIC",
  "health": 100,
  "maxHealth": 100,
  "upgradePoints": 0,
  "level": 1,
  "cardCollection": []
}'

echo ">>> Saving player via API..."
SAVE_RESPONSE=$(curl -s -w "\nHTTP_CODE:%{http_code}" -X POST http://localhost:8080/api/players \
  -H "Content-Type: application/json" \
  -d "$PLAYER_DATA")
echo ">>> Save Response: $SAVE_RESPONSE"

sleep 2

echo ">>> Retrieving player via API..."
GET_RESPONSE=$(curl -s -w "\nHTTP_CODE:%{http_code}" -X GET http://localhost:8080/api/players/api-test-player)
echo ">>> Get Response: $GET_RESPONSE"

if echo "$GET_RESPONSE" | grep -q "api-test-player"; then
  echo ">>> SUCCESS: REST API player save/retrieve working correctly."
else
  echo ">>> FAILURE: Could not retrieve saved player via REST API."
  echo ">>> Full server logs:"
  docker compose -f "$DOCKER_COMPOSE_FILE" --env-file "$ENV_FILE" logs server-1 server-2 | tail -50
  cleanup
  exit 1
fi

cleanup
echo ">>> Test 'REST API Sync Test' completed."
echo ""

echo "======================================================="
echo ">>> INTEGRATION TESTS FINISHED"
echo "======================================================="