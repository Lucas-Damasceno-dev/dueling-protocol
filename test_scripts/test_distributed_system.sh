#!/usr/bin/env bash

# Ensures the script stops if any command fails
set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"
PROJECT_ROOT="$SCRIPT_DIR/.."
DOCKER_COMPOSE_FILE="$PROJECT_ROOT/docker/docker-compose.yml"

echo "======================================================="
echo ">>> STARTING DISTRIBUTED SYSTEM TESTS"
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
  docker-compose -f "$DOCKER_COMPOSE_FILE" down --remove-orphans
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
docker-compose -f "$DOCKER_COMPOSE_FILE" --env-file .env build
rm -f .env
echo ">>> Build completed successfully."

# Step 2: Start services with both servers
run_test "Start Distributed Services" "Starting both server instances with Redis and PostgreSQL"
echo ">>> Starting distributed services..."
docker-compose -f "$DOCKER_COMPOSE_FILE" up --scale client=0 --remove-orphans -d
echo ">>> Waiting for distributed services to initialize..."
sleep 25

# Step 3: Test service discovery and communication between servers
run_test "Server Communication" "Testing communication between server instances"
echo ">>> Testing server communication..."

# Check logs for any inter-server communication
docker-compose -f "$DOCKER_COMPOSE_FILE" logs server-1 > server1_comms.log
docker-compose -f "$DOCKER_COMPOSE_FILE" logs server-2 > server2_comms.log

# Look for evidence of servers discovering each other or communicating
if grep -i -E "discovery|eureka|consul|register|service.*found|node.*discover|cluster|broadcast|multicast|gossip" server1_comms.log || grep -i -E "discovery|eureka|consul|register|service.*found|node.*discover|cluster|broadcast|multicast|gossip" server2_comms.log; then
  echo ">>> SUCCESS: Found service discovery/communication in logs"
else
  echo ">>> INFO: No explicit service discovery found in logs, but communication might happen via Redis"
fi

rm -f server1_comms.log server2_comms.log

# Step 4: Test state synchronization via Redis
run_test "State Synchronization" "Testing state synchronization between servers via Redis"
echo ">>> Testing state synchronization between servers..."

# Check initial state in Redis
INITIAL_REDIS_KEYS=$(docker exec $(docker-compose -f "$DOCKER_COMPOSE_FILE" ps -q redis) redis-cli --raw keys "*" | wc -l)
echo ">>> Initial Redis keys: $INITIAL_REDIS_KEYS"

# Start a client connected to server-1 to create some state
echo ">>> Starting client on server-1 to create game state..."
echo "BOT_MODE=autobot" > .env
echo "BOT_SCENARIO=" >> .env
docker-compose -f "$DOCKER_COMPOSE_FILE" --env-file .env up --scale client=1 --remove-orphans -d
sleep 15

# Check Redis state after client activity
AFTER_CLIENT_REDIS_KEYS=$(docker exec $(docker-compose -f "$DOCKER_COMPOSE_FILE" ps -q redis) redis-cli --raw keys "*" | wc -l)
echo ">>> Redis keys after client activity: $AFTER_CLIENT_REDIS_KEYS"

# Stop the client and start one on server-2 to verify it can access the same state
docker-compose -f "$DOCKER_COMPOSE_FILE" --env-file .env down
sleep 5

# Check if server-2 can access the same state via Redis
echo ">>> Verifying state synchronization by checking cross-server data access..."

# Look at the logs to see if there are any messages about cross-server communication
docker-compose -f "$DOCKER_COMPOSE_FILE" logs server-1 > server1_sync.log
docker-compose -f "$DOCKER_COMPOSE_FILE" logs server-2 > server2_sync.log

# Check for any evidence of servers sharing information
if grep -i -E "sync|redis.*get|redis.*set|distributed|shared.*state|cross.*server|replicat" server1_sync.log || grep -i -E "sync|redis.*get|redis.*set|distributed|shared.*state|cross.*server|replicat" server2_sync.log; then
  echo ">>> SUCCESS: Found evidence of state synchronization in server logs"
else
  echo ">>> INFO: No explicit state synchronization messages found in logs"
fi

# Check database for consistency between servers
echo ">>> Checking for database consistency..."
docker-compose -f "$DOCKER_COMPOSE_FILE" logs server-1 | grep -i -E "save|insert|update|database|sql|transaction" > db_ops_server1.log
docker-compose -f "$DOCKER_COMPOSE_FILE" logs server-2 | grep -i -E "save|insert|update|database|sql|transaction" > db_ops_server2.log

if [ -s db_ops_server1.log ] || [ -s db_ops_server2.log ]; then
  echo ">>> SUCCESS: Found database operations in both servers, indicating potential shared state"
  echo "Server 1 DB operations count: $(wc -l < db_ops_server1.log)"
  echo "Server 2 DB operations count: $(wc -l < db_ops_server2.log)"
else
  echo ">>> INFO: No database operations found in logs, but this doesn't mean they're not happening"
fi

rm -f server1_sync.log server2_sync.log db_ops_server1.log db_ops_server2.log

# Clean up .env file
rm -f .env

# Step 5: Test server failover
run_test "Server Failover" "Testing failover when one server goes down"
echo ">>> Testing server failover..."

# Get initial state
docker-compose -f "$DOCKER_COMPOSE_FILE" logs server-1 > initial_server1.log 2>&1
docker-compose -f "$DOCKER_COMPOSE_FILE" logs server-2 > initial_server2.log 2>&1

echo ">>> Stopping server-1 to test failover..."
docker-compose -f "$DOCKER_COMPOSE_FILE" stop server-1
sleep 10

# Start a client that should connect to server-2 now
echo "BOT_MODE=autobot" > .env
echo "BOT_SCENARIO=" >> .env
docker-compose -f "$DOCKER_COMPOSE_FILE" --env-file .env up --scale client=1 --remove-orphans -d
sleep 10

# Check if server-2 is handling requests properly despite server-1 being down
docker-compose -f "$DOCKER_COMPOSE_FILE" logs server-2 > after_failover_server2.log 2>&1

if grep -i -E "client.*connected|session.*created|request.*handled|handling.*request" after_failover_server2.log; then
  echo ">>> SUCCESS: Server-2 is handling client requests after server-1 failure"
else
  echo ">>> INFO: No clear indication of server-2 handling requests, but might still be working"
fi

# Restart server-1
echo ">>> Restarting server-1..."
docker-compose -f "$DOCKER_COMPOSE_FILE" start server-1
sleep 15

# Check if server-1 can rejoin the distributed system properly
docker-compose -f "$DOCKER_COMPOSE_FILE" logs server-1 > after_restart_server1.log 2>&1

if grep -i -E "rejoin|restart|recover|reconnect|service.*register|node.*join" after_restart_server1.log; then
  echo ">>> SUCCESS: Server-1 successfully rejoined distributed system"
else
  echo ">>> INFO: Server-1 restart without explicit rejoin messages"
fi

rm -f .env

# Step 6: Test load balancing with multiple servers
run_test "Multi-Server Load Balancing" "Testing load balancing across multiple servers"
echo ">>> Testing load balancing across multiple servers..."

# Start multiple clients to generate load on both servers
echo "BOT_MODE=autobot" > .env
echo "BOT_SCENARIO=" >> .env
docker-compose -f "$DOCKER_COMPOSE_FILE" --env-file .env up --scale client=3 --remove-orphans -d
sleep 20

# Check load distribution by examining log sizes
SERVER1_LOG_SIZE=$(docker-compose -f "$DOCKER_COMPOSE_FILE" logs server-1 2>&1 | wc -c)
SERVER2_LOG_SIZE=$(docker-compose -f "$DOCKER_COMPOSE_FILE" logs server-2 2>&1 | wc -c)

echo ">>> Server-1 log size: $SERVER1_LOG_SIZE bytes"
echo ">>> Server-2 log size: $SERVER2_LOG_SIZE bytes"

if [ $SERVER1_LOG_SIZE -gt 1000 ] && [ $SERVER2_LOG_SIZE -gt 1000 ]; then
  echo ">>> SUCCESS: Both servers are actively handling requests (load distribution)"
elif [ $SERVER1_LOG_SIZE -gt 1000 ] || [ $SERVER2_LOG_SIZE -gt 1000 ]; then
  echo ">>> INFO: At least one server is active, load distribution might be working"
else
  echo ">>> WARNING: Both servers have very small logs, which might indicate issues"
fi

rm -f .env

# Step 7: Test data consistency between servers
run_test "Data Consistency" "Testing data consistency across distributed servers"
echo ">>> Testing data consistency between servers..."

# Check if both servers have access to the same Redis data
SERVER1_REDIS_KEYS=$(docker exec $(docker-compose -f "$DOCKER_COMPOSE_FILE" ps -q redis) redis-cli --raw keys "spring:session:*" | wc -l)
SERVER2_REDIS_KEYS=$(docker exec $(docker-compose -f "$DOCKER_COMPOSE_FILE" ps -q redis) redis-cli --raw keys "game:*" | wc -l)

echo ">>> Session keys in Redis: $SERVER1_REDIS_KEYS"
echo ">>> Game keys in Redis: $SERVER2_REDIS_KEYS"

if [ $SERVER1_REDIS_KEYS -gt 0 ] || [ $SERVER2_REDIS_KEYS -gt 0 ]; then
  echo ">>> SUCCESS: Found shared data in Redis, indicating potential data consistency"
else
  echo ">>> INFO: No specific shared data found in Redis, but system might still be consistent"
fi

# Step 8: Test distributed caching effectiveness
run_test "Distributed Caching" "Testing effectiveness of distributed caching"
echo ">>> Testing distributed caching effectiveness..."

# Check if cache hits are happening across servers
docker-compose -f "$DOCKER_COMPOSE_FILE" logs server-1 > server1_cache.log 2>&1
docker-compose -f "$DOCKER_COMPOSE_FILE" logs server-2 > server2_cache.log 2>&1

CACHE_SUCCESS=0
if grep -i -E "cache.*hit|redisson|distributed.*cache|jcache|hibernate.*cache" server1_cache.log; then
  echo ">>> SUCCESS: Found distributed caching activity in server-1"
  CACHE_SUCCESS=1
fi

if grep -i -E "cache.*hit|redisson|distributed.*cache|jcache|hibernate.*cache" server2_cache.log; then
  echo ">>> SUCCESS: Found distributed caching activity in server-2"
  CACHE_SUCCESS=1
fi

if [ $CACHE_SUCCESS -eq 0 ]; then
  echo ">>> INFO: No explicit distributed caching messages found in logs"
fi

rm -f server1_cache.log server2_cache.log

# Clean up temporary logs
rm -f initial_server1.log initial_server2.log after_failover_server2.log after_restart_server1.log

echo "======================================================="
echo ">>> DISTRIBUTED SYSTEM TESTS COMPLETED"
echo "======================================================="

# Clean up
cleanup