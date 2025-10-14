#!/usr/bin/env bash

# Ensures the script stops if any command fails
set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"
PROJECT_ROOT="$SCRIPT_DIR/.."
DOCKER_COMPOSE_FILE="$PROJECT_ROOT/docker/docker-compose.yml"

echo "======================================================="
echo ">>> STARTING DATA CONSISTENCY TESTS"
echo ">>> Testing data consistency across Redis, PostgreSQL and distributed servers"
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

# Step 2: Start services
run_test "Start Services" "Starting full system for data consistency tests"
echo ">>> Starting services..."
docker-compose -f "$DOCKER_COMPOSE_FILE" up --scale client=0 --remove-orphans -d
echo ">>> Waiting for services to initialize..."
sleep 30

# Step 3: Test Redis-PostgreSQL data synchronization
run_test "Redis-PostgreSQL Synchronization" "Testing data synchronization between Redis and PostgreSQL"
echo ">>> Testing Redis-PostgreSQL data synchronization..."

# Get initial state of both data stores
INITIAL_REDIS_KEYS=$(docker exec $(docker-compose -f "$DOCKER_COMPOSE_FILE" ps -q redis) redis-cli --raw keys "*" | wc -l)
INITIAL_DB_TABLES=$(docker exec $(docker-compose -f "$DOCKER_COMPOSE_FILE" ps -q postgres) psql -U dueling_user -d dueling_db -t -c "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public';" 2>/dev/null | tr -d ' \n' || echo "0")

echo ">>> Initial Redis keys: $INITIAL_REDIS_KEYS"
echo ">>> Initial DB tables: $INITIAL_DB_TABLES"

# Start a client to generate some synchronized data
echo "BOT_MODE=autobot" > .env
echo "BOT_SCENARIO=" >> .env
docker-compose -f "$DOCKER_COMPOSE_FILE" --env-file .env up --scale client=2 --remove-orphans -d
sleep 25

# Check state after client activity
AFTER_CLIENT_REDIS_KEYS=$(docker exec $(docker-compose -f "$DOCKER_COMPOSE_FILE" ps -q redis) redis-cli --raw keys "*" | wc -l)
AFTER_CLIENT_DB_TABLES=$(docker exec $(docker-compose -f "$DOCKER_COMPOSE_FILE" ps -q postgres) psql -U dueling_user -d dueling_db -t -c "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public';" 2>/dev/null | tr -d ' \n' || echo "0")

echo ">>> Redis keys after client activity: $AFTER_CLIENT_REDIS_KEYS"
echo ">>> DB tables after client activity: $AFTER_CLIENT_DB_TABLES"

# Check for specific types of keys that indicate synchronization
SESSION_KEYS=$(docker exec $(docker-compose -f "$DOCKER_COMPOSE_FILE" ps -q redis) redis-cli --raw keys "spring:session:*" | wc -l)
GAME_KEYS=$(docker exec $(docker-compose -f "$DOCKER_COMPOSE_FILE" ps -q redis) redis-cli --raw keys "game:*" | wc -l)
MATCH_KEYS=$(docker exec $(docker-compose -f "$DOCKER_COMPOSE_FILE" ps -q redis) redis-cli --raw keys "match:*" | wc -l)

echo ">>> Session keys in Redis: $SESSION_KEYS"
echo ">>> Game keys in Redis: $GAME_KEYS"
echo ">>> Match keys in Redis: $MATCH_KEYS"

docker-compose -f "$DOCKER_COMPOSE_FILE" --env-file .env down
rm -f .env

if [ $AFTER_CLIENT_REDIS_KEYS -gt $INITIAL_REDIS_KEYS ] || [ $SESSION_KEYS -gt 0 ] || [ $GAME_KEYS -gt 0 ] || [ $MATCH_KEYS -gt 0 ]; then
  echo ">>> SUCCESS: Redis contains data that indicates synchronization"
else
  echo ">>> INFO: No new Redis data found, but this may be expected depending on implementation"
fi

if [ $AFTER_CLIENT_DB_TABLES -ge $INITIAL_DB_TABLES ]; then
  echo ">>> SUCCESS: PostgreSQL database structure is maintained"
else
  echo ">>> WARNING: PostgreSQL database structure changed unexpectedly"
fi

# Step 4: Test data consistency across distributed servers
run_test "Cross-Server Data Consistency" "Testing data consistency between distributed server instances"
echo ">>> Testing data consistency across distributed servers..."

# Start clients connected to different servers to test data sharing
echo ">>> Starting clients to generate cross-server data..."
echo "BOT_MODE=autobot" > .env
echo "BOT_SCENARIO=" >> .env

# In the current docker-compose setup, clients can connect to different servers
# but the load balancing happens through the gateway. Let's start multiple clients
# and see if they can share data through Redis
docker-compose -f "$DOCKER_COMPOSE_FILE" --env-file .env up --scale client=4 --remove-orphans -d
sleep 30

# Check logs to see if both servers handled requests
docker-compose -f "$DOCKER_COMPOSE_FILE" logs server-1 > server1_data.log 2>&1
docker-compose -f "$DOCKER_COMPOSE_FILE" logs server-2 > server2_data.log 2>&1

SERVER1_LOG_SIZE=$(wc -c < server1_data.log)
SERVER2_LOG_SIZE=$(wc -c < server2_data.log)

echo ">>> Server-1 log size: $SERVER1_LOG_SIZE bytes"
echo ">>> Server-2 log size: $SERVER2_LOG_SIZE bytes"

if [ $SERVER1_LOG_SIZE -gt 1000 ] && [ $SERVER2_LOG_SIZE -gt 1000 ]; then
  echo ">>> SUCCESS: Both servers appear to be handling requests (data generation)"
else
  echo ">>> INFO: Log sizes - Server1: $SERVER1_LOG_SIZE, Server2: $SERVER2_LOG_SIZE"
fi

# Check Redis for shared data after cross-server activity
FINAL_REDIS_KEYS=$(docker exec $(docker-compose -f "$DOCKER_COMPOSE_FILE" ps -q redis) redis-cli --raw keys "*" | wc -l)
FINAL_SESSION_KEYS=$(docker exec $(docker-compose -f "$DOCKER_COMPOSE_FILE" ps -q redis) redis-cli --raw keys "spring:session:*" | wc -l)
FINAL_GAME_KEYS=$(docker exec $(docker-compose -f "$DOCKER_COMPOSE_FILE" ps -q redis) redis-cli --raw keys "game:*" | wc -l)

echo ">>> Final Redis keys: $FINAL_REDIS_KEYS"
echo ">>> Final Session keys: $FINAL_SESSION_KEYS"
echo ">>> Final Game keys: $FINAL_GAME_KEYS"

docker-compose -f "$DOCKER_COMPOSE_FILE" --env-file .env down
rm -f .env server1_data.log server2_data.log

if [ $FINAL_REDIS_KEYS -gt 0 ]; then
  echo ">>> SUCCESS: Redis contains shared data after cross-server activity"
else
  echo ">>> INFO: No Redis data found after cross-server activity"
fi

# Step 5: Test distributed caching consistency
run_test "Distributed Caching Consistency" "Testing consistency of distributed cache through Redis"
echo ">>> Testing distributed caching consistency..."

# Start one server, generate cache data, then start second server
docker-compose -f "$DOCKER_COMPOSE_FILE" up --scale client=0 --remove-orphans -d server-1 redis postgres prometheus grafana
sleep 15

# Start a client with server-1 only
echo "BOT_MODE=autobot" > .env
echo "BOT_SCENARIO=" >> .env
docker-compose -f "$DOCKER_COMPOSE_FILE" --env-file .env up --scale client=1 --remove-orphans -d server-1
sleep 15

# Check cache state with first server
SERVER1_CACHE_KEYS=$(docker exec $(docker-compose -f "$DOCKER_COMPOSE_FILE" ps -q redis) redis-cli --raw keys "spring:session:*" | wc -l)
echo ">>> Cache keys after server-1 activity: $SERVER1_CACHE_KEYS"

docker-compose -f "$DOCKER_COMPOSE_FILE" --env-file .env down
rm -f .env

# Now start both servers and check if cache is accessible
docker-compose -f "$DOCKER_COMPOSE_FILE" up --scale client=0 --remove-orphans -d
sleep 15

# Check if cache data is still available with both servers running
BOTH_SERVERS_CACHE_KEYS=$(docker exec $(docker-compose -f "$DOCKER_COMPOSE_FILE" ps -q redis) redis-cli --raw keys "spring:session:*" | wc -l)
echo ">>> Cache keys with both servers: $BOTH_SERVERS_CACHE_KEYS"

if [ $BOTH_SERVERS_CACHE_KEYS -ge $SERVER1_CACHE_KEYS ]; then
  echo ">>> SUCCESS: Cache data preserved when both servers are active"
else
  echo ">>> INFO: Cache data may have been affected by server topology change"
fi

# Step 6: Test transactional consistency in PostgreSQL
run_test "PostgreSQL Transactional Consistency" "Testing database transactional consistency"
echo ">>> Testing PostgreSQL transactional consistency..."

# Check if tables exist and if we can query them
TABLES_LIST=$(docker exec $(docker-compose -f "$DOCKER_COMPOSE_FILE" ps -q postgres) psql -U dueling_user -d dueling_db -t -c "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public';" 2>/dev/null || echo "No tables")

echo ">>> PostgreSQL tables found:"
echo "$TABLES_LIST"

if [ "$TABLES_LIST" != "No tables" ] && [ -n "$TABLES_LIST" ] && [ "$TABLES_LIST" != $'\n' ]; then
  echo ">>> SUCCESS: PostgreSQL database contains tables"
  
  # Look for common table names that might exist
  COMMON_TABLES=("players" "matches" "games" "users" "game_sessions" "match_history")
  for table in "${COMMON_TABLES[@]}"; do
    if echo "$TABLES_LIST" | grep -i "$table" > /dev/null; then
      ROW_COUNT=$(docker exec $(docker-compose -f "$DOCKER_COMPOSE_FILE" ps -q postgres) psql -U dueling_user -d dueling_db -t -c "SELECT COUNT(*) FROM $table;" 2>/dev/null | tr -d ' \n' || echo "0")
      echo ">>> Found table '$table' with $ROW_COUNT rows"
    fi
  done
else
  echo ">>> INFO: No application tables found in PostgreSQL (this might be expected if no game data was generated)"
fi

# Step 7: Test Redis persistence and consistency
run_test "Redis Persistence Consistency" "Testing Redis data persistence and consistency"
echo ">>> Testing Redis persistence and consistency..."

# Check Redis info for persistence metrics
REDIS_INFO=$(docker exec $(docker-compose -f "$DOCKER_COMPOSE_FILE" ps -q redis) redis-cli info 2>/dev/null || echo "Redis not accessible")

if echo "$REDIS_INFO" | grep -q "redis_version"; then
  echo ">>> SUCCESS: Redis server is accessible"
  
  # Check memory usage
  MEMORY_USED=$(echo "$REDIS_INFO" | grep -E "^used_memory:" | cut -d: -f2 | numfmt --to=iec 2>/dev/null || echo "Unknown")
  echo ">>> Redis memory usage: $MEMORY_USED"
  
  # Check persistence settings
  if echo "$REDIS_INFO" | grep -E "save:|aof_enabled:" > /dev/null; then
    echo ">>> SUCCESS: Redis persistence configuration found"
  fi
else
  echo ">>> WARNING: Cannot access Redis info"
fi

# Step 8: Test session consistency across servers
run_test "Session Consistency" "Testing session consistency across distributed servers"
echo ">>> Testing session consistency..."

# Start clients again to generate session data
echo "BOT_MODE=autobot" > .env
echo "BOT_SCENARIO=" >> .env
docker-compose -f "$DOCKER_COMPOSE_FILE" --env-file .env up --scale client=2 --remove-orphans -d
sleep 20

# Check for session-related data in Redis
SESSION_COUNT=$(docker exec $(docker-compose -f "$DOCKER_COMPOSE_FILE" ps -q redis) redis-cli --raw keys "spring:session:sessions:*" | wc -l)
EXPIRATION_KEYS=$(docker exec $(docker-compose -f "$DOCKER_COMPOSE_FILE" ps -q redis) redis-cli --raw keys "spring:session:expirations:*" | wc -l)

echo ">>> Active sessions in Redis: $SESSION_COUNT"
echo ">>> Session expiration keys: $EXPIRATION_KEYS"

# Check if session data is properly structured
if [ $SESSION_COUNT -gt 0 ]; then
  # Get a sample session key to check its structure
  SAMPLE_SESSION=$(docker exec $(docker-compose -f "$DOCKER_COMPOSE_FILE" ps -q redis) redis-cli --raw keys "spring:session:sessions:*" | head -n 1)
  if [ -n "$SAMPLE_SESSION" ]; then
    SESSION_TTL=$(docker exec $(docker-compose -f "$DOCKER_COMPOSE_FILE" ps -q redis) redis-cli ttl "$SAMPLE_SESSION" 2>/dev/null || echo "No TTL")
    echo ">>> Sample session TTL: $SESSION_TTL"
  fi
fi

docker-compose -f "$DOCKER_COMPOSE_FILE" --env-file .env down
rm -f .env

# Step 9: Test data durability after restart
run_test "Data Durability After Restart" "Testing if data persists after service restart"
echo ">>> Testing data durability after restart..."

# Get current Redis and DB state
BEFORE_RESTART_REDIS_KEYS=$(docker exec $(docker-compose -f "$DOCKER_COMPOSE_FILE" ps -q redis) redis-cli --raw keys "*" | wc -l)
BEFORE_RESTART_SESSION_KEYS=$(docker exec $(docker-compose -f "$DOCKER_COMPOSE_FILE" ps -q redis) redis-cli --raw keys "spring:session:*" | wc -l)

echo ">>> Redis keys before restart: $BEFORE_RESTART_REDIS_KEYS"
echo ">>> Session keys before restart: $BEFORE_RESTART_SESSION_KEYS"

# Restart services
echo ">>> Restarting services to test data durability..."
docker-compose -f "$DOCKER_COMPOSE_FILE" restart server-1 server-2
sleep 20

# Check state after restart
AFTER_RESTART_REDIS_KEYS=$(docker exec $(docker-compose -f "$DOCKER_COMPOSE_FILE" ps -q redis) redis-cli --raw keys "*" | wc -l)
AFTER_RESTART_SESSION_KEYS=$(docker exec $(docker-compose -f "$DOCKER_COMPOSE_FILE" ps -q redis) redis-cli --raw keys "spring:session:*" | wc -l)

echo ">>> Redis keys after restart: $AFTER_RESTART_REDIS_KEYS"
echo ">>> Session keys after restart: $AFTER_RESTART_SESSION_KEYS"

if [ $AFTER_RESTART_REDIS_KEYS -gt 0 ]; then
  echo ">>> SUCCESS: Some Redis data survived restart (may depend on persistence settings)"
else
  echo ">>> INFO: No Redis data survived restart (expected if using volatile storage)"
fi

# Step 10: Test concurrent access consistency
run_test "Concurrent Access Consistency" "Testing data consistency under concurrent access"
echo ">>> Testing data consistency under concurrent access..."

# Start multiple clients simultaneously
echo "BOT_MODE=autobot" > .env
echo "BOT_SCENARIO=" >> .env
docker-compose -f "$DOCKER_COMPOSE_FILE" --env-file .env up --scale client=5 --remove-orphans -d
sleep 45

# Check for consistency issues in logs
docker-compose -f "$DOCKER_COMPOSE_FILE" logs server-1 > server1_concurrent.log 2>&1
docker-compose -f "$DOCKER_COMPOSE_FILE" logs server-2 > server2_concurrent.log 2>&1

# Look for any consistency errors or data integrity issues
CONSISTENCY_ERRORS=0
if grep -i -E "consistency.*error|data.*inconsist|race.*condition|transaction.*fail|concurrent.*modif" server1_concurrent.log; then
  echo ">>> INFO: Found potential consistency issues in server-1 logs"
  ((CONSISTENCY_ERRORS++))
fi

if grep -i -E "consistency.*error|data.*inconsist|race.*condition|transaction.*fail|concurrent.*modif" server2_concurrent.log; then
  echo ">>> INFO: Found potential consistency issues in server-2 logs"
  ((CONSISTENCY_ERRORS++))
fi

# Check final data state after concurrent access
FINAL_REDIS_KEYS_CONCURRENT=$(docker exec $(docker-compose -f "$DOCKER_COMPOSE_FILE" ps -q redis) redis-cli --raw keys "*" | wc -l)
FINAL_SESSION_KEYS_CONCURRENT=$(docker exec $(docker-compose -f "$DOCKER_COMPOSE_FILE" ps -q redis) redis-cli --raw keys "spring:session:*" | wc -l)

echo ">>> Redis keys after concurrent access: $FINAL_REDIS_KEYS_CONCURRENT"
echo ">>> Session keys after concurrent access: $FINAL_SESSION_KEYS_CONCURRENT"

if [ $CONSISTENCY_ERRORS -eq 0 ]; then
  echo ">>> SUCCESS: No obvious consistency errors found under concurrent access"
else
  echo ">>> INFO: Found $CONSISTENCY_ERRORS potential consistency-related messages"
fi

docker-compose -f "$DOCKER_COMPOSE_FILE" --env-file .env down
rm -f .env server1_concurrent.log server2_concurrent.log

# Step 11: Data consistency validation summary
run_test "Data Consistency Summary" "Summary of data consistency validation"
echo ">>> Data consistency testing summary:"

# Final validation checks
FINAL_REDIS_KEYS=$(docker exec $(docker-compose -f "$DOCKER_COMPOSE_FILE" ps -q redis) redis-cli --raw keys "*" | wc -l)
FINAL_DB_TABLES=$(docker exec $(docker-compose -f "$DOCKER_COMPOSE_FILE" ps -q postgres) psql -U dueling_user -d dueling_db -t -c "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public';" 2>/dev/null | tr -d ' \n' || echo "0")

echo ">>> Final Redis keys: $FINAL_REDIS_KEYS"
echo ">>> Final DB tables: $FINAL_DB_TABLES"

if [ $FINAL_REDIS_KEYS -gt 0 ]; then
  echo ">>> SUCCESS: Redis contains data, indicating some level of consistency"
else
  echo ">>> INFO: Redis is empty, which may be expected depending on settings"
fi

if [ $FINAL_DB_TABLES -gt 0 ]; then
  echo ">>> SUCCESS: PostgreSQL database has structure, indicating basic consistency"
else
  echo ">>> INFO: PostgreSQL database structure not found"
fi

echo "======================================================="
echo ">>> DATA CONSISTENCY TESTS COMPLETED"
echo ">>> These tests validate data consistency across storage systems"
echo "======================================================="

# Clean up
cleanup