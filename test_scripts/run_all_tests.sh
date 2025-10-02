#!/usr/bin/env bash

# Ensures the script stops if any command fails
set -e

echo "======================================================="
echo ">>> STARTING COMPLETE TEST SUITE"
echo "======================================================="

# Step 1: Compile and build Docker images (only needs to be done once)
echo ">>> [STEP 1/4] Building Docker images..."
./scripts/build.sh
# Create a temporary .env file with default values for docker-compose build
echo "BOT_MODE=autobot" > .env
echo "BOT_SCENARIO=" >> .env
docker-compose -f docker/docker-compose.yml --env-file .env build
rm -f .env
echo ">>> Images built successfully."
echo ""

# -----------------------------------------------------------------------------

# Step 2: Running scenario and robustness tests
echo ">>> [STEP 2/4] Running individual scenario tests..."

# Helper function to run a specific test
run_test() {
  local bot_mode=$1
  local bot_scenario=$2
  local test_name=$3
  local client_count=$4

  echo "-------------------------------------------------------"
  echo ">>> Running test: $test_name"
  echo "-------------------------------------------------------"

  # Set variables for docker-compose
  echo "BOT_MODE=$bot_mode" > .env
  echo "BOT_SCENARIO=$bot_scenario" >> .env

  # Start containers and wait for the test to complete
  docker-compose -f docker/docker-compose.yml --env-file .env up --scale client=$client_count --remove-orphans -d
  sleep 15 # Time for the test to run

  # Display server logs for analysis
  echo ">>> Server Logs for test '$test_name':"
  docker-compose -f docker/docker-compose.yml --env-file .env logs server

  # Clean up environment
  docker-compose -f docker/docker-compose.yml down
  rm -f .env
  echo ">>> Test '$test_name' completed."
  echo ""
}

# Abrupt Disconnection Scenarios
run_test "autobot" "matchmaking_disconnect" "Queue Disconnection" 2
run_test "autobot" "mid_game_disconnect" "Mid-Game Disconnection" 2

# Concurrency Scenarios
run_test "autobot" "simultaneous_play" "Simultaneous Play" 2
run_test "autobot" "race_condition" "Persistence Race Condition" 1

# Robustness Test with Malformed Inputs
run_test "maliciousbot" "" "Malformed Inputs (Malicious Bot)" 1

echo ">>> Individual scenario tests completed."
echo ""

# -----------------------------------------------------------------------------

# Step 3: Running new integration tests for Pub/Sub and REST API
echo ">>> [STEP 3/4] Running integration tests for new features..."
./test_scripts/test_integration_pubsub_rest.sh
echo ">>> Integration tests completed."
echo ""

# -----------------------------------------------------------------------------

# Step 4: Running the final stress test
echo ">>> [STEP 4/4] Running stress test with 10 clients..."
./test_scripts/test_stress.sh

echo "======================================================="
echo ">>> COMPLETE TEST SUITE FINISHED"
echo "======================================================="