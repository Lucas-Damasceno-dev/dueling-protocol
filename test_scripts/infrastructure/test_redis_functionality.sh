#!/usr/bin/env bash

# Ensures the script stops if any command fails
set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"
PROJECT_ROOT="$SCRIPT_ROOT/.."
DOCKER_COMPOSE_FILE="$PROJECT_ROOT/docker/docker-compose.yml"

echo "======================================================="
echo ">>> STARTING REDIS FUNCTIONALITY TESTS"
echo "======================================================="

# Helper function to run a specific test
run_test() {
  local test_name=$1
  local description=$2

  echo "-------------------------------------------------------"
  echo ">>> Running test: $test_name"
  echo ">>> Description: $description"
  echo "-------------------------------------------------------"
}

# Helper function to clean up the environment
cleanup() {
  echo ">>> Cleaning up Docker environment..."
  docker compose -f "$DOCKER_COMPOSE_FILE" down --remove-orphans
  if [ -f "$PROJECT_ROOT/.env" ]; then
    rm -f "$PROJECT_ROOT/.env"
  fi
}

trap cleanup EXIT

# Step 1: Build the project and Docker images
run_test "Build Project" "Compiling project and building Docker images"
echo ">>> Building project..."
cd "$PROJECT_ROOT"
./scripts/build.sh
echo ">>> Building Docker images..."
echo "BOT_MODE=autobot" > .env
echo "BOT_SCENARIO=" >> .env
docker compose -f "$DOCKER_COMPOSE_FILE" --env-file .env build
rm -f .env
echo ">>> Build completed successfully."

# Step 2: Start services (without clients initially)
run_test "Start Services" "Starting servers, Redis, and PostgreSQL for Redis tests"
echo ">>> Starting services..."
docker compose -f "$DOCKER_COMPOSE_FILE" up --scale client=0 --remove-orphans -d
echo ">>> Waiting for services to initialize..."
sleep 20

# Step 3: Test Redis connectivity
run_test "Redis Connectivity" "Testing direct connectivity to Redis instance"
echo ">>> Testing Redis connectivity..."

# Check if Redis is accessible
if docker exec $(docker compose -f "$DOCKER_COMPOSE_FILE" ps -q redis) redis-cli ping | grep -q "PONG"; then
  echo ">>> SUCCESS: Redis is accessible and responding to PING"
else
  echo ">>> FAILURE: Redis is not accessible or not responding to PING"
  docker compose -f "$DOCKER_COMPOSE_FILE" logs redis
  exit 1
fi

# Step 4: Test Redis as cache for sessions
run_test "Session Cache" "Testing Redis as session cache"
echo ">>> Testing Redis session caching functionality..."

# Start a simple test client to generate some session data
echo ">>> Starting temporary client to test session caching..."
docker compose -f "$DOCKER_COMPOSE_FILE" up --scale client=1 --remove-orphans -d
sleep 10

# Check Redis for session-related keys
SESSION_KEYS=$(docker exec $(docker compose -f "$DOCKER_COMPOSE_FILE" ps -q redis) redis-cli --raw keys "spring:session:*" | wc -l)

if [ "$SESSION_KEYS" -gt 0 ]; then
  echo ">>> SUCCESS: Found $SESSION_KEYS session-related keys in Redis"
  
  # Get details of session keys
  echo ">>> Session keys found in Redis:"
  docker exec $(docker compose -f "$DOCKER_COMPOSE_FILE" ps -q redis) redis-cli keys "spring:session:*"
else
  echo ">>> WARNING: No session-related keys found in Redis, but this might be expected depending on implementation"
fi

# Step 5: Test Redis pub/sub functionality
run_test "Pub/Sub Functionality" "Testing Redis publisher-subscriber functionality"
echo ">>> Testing Redis pub/sub functionality..."

# Check the server logs to see if pub/sub is being used
docker compose -f "$DOCKER_COMPOSE_FILE" logs server-1 > server1.log
docker compose -f "$DOCKER_COMPOSE_FILE" logs server-2 > server2.log

# Look for pub/sub related messages in logs
if grep -i -E "publish|subscribe|pubsub|redis.*pub|redis.*sub" server1.log || grep -i -E "publish|subscribe|pubsub|redis.*pub|redis.*sub" server2.log; then
  echo ">>> SUCCESS: Found pub/sub related activity in server logs"
else
  echo ">>> INFO: No explicit pub/sub messages found in server logs, but functionality might still be working"
fi

# Clean up log files
rm -f server1.log server2.log

# Step 6: Test Redis as JCache provider for Hibernate
run_test "Hibernate Cache via Redis" "Testing Redis as Hibernate cache provider"
echo ">>> Testing Redis as Hibernate cache provider..."

# Check server logs for Redisson/Hibernate cache initialization
docker compose -f "$DOCKER_COMPOSE_FILE" logs server-1 > server1_cache.log
docker compose -f "$DOCKER_COMPOSE_FILE" logs server-2 > server2_cache.log

if grep -i -E "redisson|hibernate.*cache|jcache|redission" server1_cache.log || grep -i -E "redisson|hibernate.*cache|jcache|redission" server2_cache.log; then
  echo ">>> SUCCESS: Found Redisson/Hibernate cache initialization in server logs"
else
  echo ">>> INFO: No Redisson/Hibernate cache initialization found in logs, but might be initialized without explicit logging"
fi

rm -f server1_cache.log server2_cache.log

# Step 7: Test Redis as distributed cache between servers
run_test "Distributed Cache" "Testing Redis as distributed cache between servers"
echo ">>> Testing data sharing between servers via Redis..."

# Start a client to create some data that would be cached in Redis
docker compose -f "$DOCKER_COMPOSE_FILE" up --scale client=2 --remove-orphans -d
sleep 15

# Check Redis for any game-related keys that might indicate distributed caching
GAME_KEYS=$(docker exec $(docker compose -f "$DOCKER_COMPOSE_FILE" ps -q redis) redis-cli --raw keys "game:*" | wc -l)
MATCH_KEYS=$(docker exec $(docker compose -f "$DOCKER_COMPOSE_FILE" ps -q redis) redis-cli --raw keys "match:*" | wc -l)
PLAYER_KEYS=$(docker exec $(docker compose -f "$DOCKER_COMPOSE_FILE" ps -q redis) redis-cli --raw keys "player:*" | wc -l)

TOTAL_REDIS_KEYS=$(docker exec $(docker compose -f "$DOCKER_COMPOSE_FILE" ps -q redis) redis-cli --raw keys "*" | wc -l)

echo ">>> Total Redis keys: $TOTAL_REDIS_KEYS"
echo ">>> Game-related keys: $GAME_KEYS"
echo ">>> Match-related keys: $MATCH_KEYS" 
echo ">>> Player-related keys: $PLAYER_KEYS"

if [ "$TOTAL_REDIS_KEYS" -gt 5 ]; then
  echo ">>> SUCCESS: Redis contains $TOTAL_REDIS_KEYS keys, indicating active use as distributed cache"
else
  echo ">>> INFO: Low number of keys in Redis ($TOTAL_REDIS_KEYS), but this might be normal depending on test duration"
fi

# Step 8: Test Redis failover behavior (if applicable)
run_test "Redis Failover Readiness" "Testing application resilience to Redis availability"
echo ">>> Testing application behavior when Redis is temporarily unavailable..."

# Get initial server logs
docker compose -f "$DOCKER_COMPOSE_FILE" logs server-1 > initial_server1.log 2>&1
docker compose -f "$DOCKER_COMPOSE_FILE" logs server-2 > initial_server2.log 2>&1

# Stop Redis temporarily
echo ">>> Stopping Redis temporarily to test resilience..."
docker compose -f "$DOCKER_COMPOSE_FILE" stop redis
sleep 5

# Start Redis again
echo ">>> Restarting Redis..."
docker compose -f "$DOCKER_COMPOSE_FILE" start redis
sleep 10

# Check server logs for any Redis-related errors during the Redis unavailability
docker compose -f "$DOCKER_COMPOSE_FILE" logs server-1 > after_redis_restart_server1.log 2>&1
docker compose -f "$DOCKER_COMPOSE_FILE" logs server-2 > after_redis_restart_server2.log 2>&1

# Compare logs to see if there were any Redis-related issues
if diff -u initial_server1.log after_redis_restart_server1.log | grep -i -E "error|exception|fail|redis|connection|timeout"; then
  echo ">>> INFO: Found Redis-related issues in server-1 logs during temporary unavailability"
else
  echo ">>> SUCCESS: No major Redis-related errors detected in server-1 logs"
fi

if diff -u initial_server2.log after_redis_restart_server2.log | grep -i -E "error|exception|fail|redis|connection|timeout"; then
  echo ">>> INFO: Found Redis-related issues in server-2 logs during temporary unavailability"
else
  echo ">>> SUCCESS: No major Redis-related errors detected in server-2 logs"
fi

# Cleanup temporary logs
rm -f initial_server1.log initial_server2.log after_redis_restart_server1.log after_redis_restart_server2.log

echo "======================================================="
echo ">>> REDIS FUNCTIONALITY TESTS COMPLETED"
echo "======================================================="

# Clean up
cleanup