#!/usr/bin/env bash

# Define the log file name with current date and time to keep them organized
if [ "$SAVE_LOGS" = "true" ]; then
    LOG_FILE="test_results_$(date +'%Y-%m-%d_%H-%M-%S').log"
    echo "======================================================="
    echo ">>> RUNNING ALL TESTS AND SAVING OUTPUT"
    echo ">>> The output will be displayed here and also saved to file: $LOG_FILE"
    echo "======================================================="
    exec &> >(tee "$LOG_FILE")
fi

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
docker compose -f docker/docker-compose.yml --env-file .env build
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
  docker compose -f docker/docker-compose.yml --env-file .env up --scale client=$client_count --remove-orphans -d
  sleep 15 # Time for the test to run

  # Display server logs for analysis
  echo ">>> Server Logs for test '$test_name':"
  docker compose -f docker/docker-compose.yml --env-file .env logs server

  # Clean up environment
  docker compose -f docker/docker-compose.yml down
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

# Step 4: Running comprehensive tests for new architecture components
echo ">>> [STEP 4/7] Running tests for new architecture components..."

echo ">>> Running Gateway functionality tests..."
./test_scripts/test_gateway_functionality.sh

echo ">>> Running JWT security tests..."
./test_scripts/test_jwt_security.sh

echo ">>> Running Redis functionality tests..."
./test_scripts/test_redis_functionality.sh

echo ">>> Running PostgreSQL functionality tests..."
./test_scripts/test_postgresql_functionality.sh

echo ">>> Running distributed system tests..."
./test_scripts/test_distributed_system.sh

echo ">>> Running full integration tests..."
./test_scripts/test_full_integration.sh

echo ">>> Architecture component tests completed."
echo ""

# -----------------------------------------------------------------------------

# Step 5: Running the stress test
echo ">>> [STEP 5/7] Running stress test with 10 clients..."
./test_scripts/test_stress.sh

# -----------------------------------------------------------------------------

# Step 6: Running final validation test
echo ">>> [STEP 6/7] Running final validation test..."
./test_scripts/test_distributed_matchmaking.sh

# -----------------------------------------------------------------------------

# Step 7: Running additional comprehensive tests
echo ">>> [STEP 7/11] Running contract compliance tests..."
./test_scripts/test_contract_compliance.sh

echo ">>> [STEP 8/11] Running performance and scalability tests..."
./test_scripts/test_performance_scalability.sh

echo ">>> [STEP 9/11] Running advanced security tests..."
./test_scripts/test_advanced_security.sh

echo ">>> [STEP 10/11] Running disaster recovery tests..."
./test_scripts/test_disaster_recovery.sh

# -----------------------------------------------------------------------------

# Step 11: Running leader failure test
echo ">>> [STEP 11/12] Running leader failure test..."
../test_leader_failure.sh

# -----------------------------------------------------------------------------

# Step 12: Running final validation tests
echo ">>> [STEP 12/12] Running additional validation tests..."
./test_scripts/test_configuration_deployment.sh
./test_scripts/test_observability.sh
./test_scripts/test_data_consistency.sh
./test_scripts/test_data_migration.sh

echo "======================================================="
echo ">>> COMPLETE TEST SUITE FINISHED"
echo ">>> All tests have been executed including:"
echo ">>> - Basic functionality tests"
echo ">>> - Gateway, JWT, Redis, PostgreSQL tests" 
echo ">>> - Distributed system tests"
echo ">>> - Full integration tests"
echo ">>> - Contract compliance tests"
echo ">>> - Performance and scalability tests"
echo ">>> - Advanced security tests"
echo ">>> - Disaster recovery tests"
echo ">>> - Configuration and deployment tests"
echo ">>> - Observability tests"
echo ">>> - Data consistency tests"
echo ">>> - Data migration tests"
echo ">>> - Stress and validation tests"
echo "======================================================="
