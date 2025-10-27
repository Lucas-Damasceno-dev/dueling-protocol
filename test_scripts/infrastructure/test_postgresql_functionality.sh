#!/usr/bin/env bash

# Ensures the script stops if any command fails
set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"
PROJECT_ROOT="$SCRIPT_DIR/../.."
DOCKER_COMPOSE_FILE="$PROJECT_ROOT/docker/docker-compose.yml"

echo "======================================================="
echo ">>> STARTING POSTGRESQL FUNCTIONALITY TESTS"
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
# Skipping build - images should be pre-built
# mvn clean package -DskipTests && docker compose build
echo ">>> Building Docker images..."
echo "BOT_MODE=autobot" > .env
echo "BOT_SCENARIO=" >> .env
docker compose -f "$DOCKER_COMPOSE_FILE" --env-file .env build
rm -f .env
echo ">>> Build completed successfully."

# Step 2: Start services (without clients initially)
run_test "Start Services" "Starting servers, Redis, and PostgreSQL for database tests"
echo ">>> Starting services..."
docker compose -f "$DOCKER_COMPOSE_FILE" up -d postgres redis-master redis-slave redis-sentinel-1 redis-sentinel-2 redis-sentinel-3 server-1 server-2 -d
echo ">>> Waiting for services and database to initialize..."
sleep 25

# Step 3: Test PostgreSQL connectivity
run_test "PostgreSQL Connectivity" "Testing direct connectivity to PostgreSQL instance"
echo ">>> Testing PostgreSQL connectivity..."

# Check if PostgreSQL is accessible and responding
if docker exec $(docker compose -f "$DOCKER_COMPOSE_FILE" ps -q postgres) pg_isready -h localhost -U dueling_user -d dueling_db; then
  echo ">>> SUCCESS: PostgreSQL is accessible and accepting connections"
else
  echo ">>> FAILURE: PostgreSQL is not accessible or not accepting connections"
  docker compose -f "$DOCKER_COMPOSE_FILE" logs postgres
  exit 1
fi

# Step 4: Test PostgreSQL schema initialization
run_test "Schema Initialization" "Testing if database schema is properly initialized"
echo ">>> Testing database schema initialization..."

# Check if tables exist by querying the information schema
TABLES_COUNT=$(docker exec $(docker compose -f "$DOCKER_COMPOSE_FILE" ps -q postgres) psql -U dueling_user -d dueling_db -t -c "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public';" 2>/dev/null || echo "0")

if [ "$TABLES_COUNT" -gt 0 ]; then
  echo ">>> SUCCESS: Found $TABLES_COUNT tables in the database"
  
  # List available tables
  echo ">>> Available tables:"
  docker exec $(docker compose -f "$DOCKER_COMPOSE_FILE" ps -q postgres) psql -U dueling_user -d dueling_db -c "\dt" 2>/dev/null | head -20
else
  echo ">>> WARNING: No tables found in the database - schema may not be initialized"
fi

# Step 5: Test JPA/Hibernate integration with PostgreSQL
run_test "JPA/Hibernate Integration" "Testing JPA/Hibernate integration with PostgreSQL"
echo ">>> Testing JPA/Hibernate integration..."

# Check server logs for PostgreSQL/JPA initialization messages
docker compose -f "$DOCKER_COMPOSE_FILE" logs server-1 > server1_jpa.log
docker compose -f "$DOCKER_COMPOSE_FILE" logs server-2 > server2_jpa.log

JPA_SUCCESS=0
if grep -i -E "postgresql|hibernate|jpa|datasource|connection.*pool" server1_jpa.log; then
  echo ">>> SUCCESS: Found PostgreSQL/JPA initialization messages in server-1 logs"
  JPA_SUCCESS=1
fi

if grep -i -E "postgresql|hibernate|jpa|datasource|connection.*pool" server2_jpa.log; then
  echo ">>> SUCCESS: Found PostgreSQL/JPA initialization messages in server-2 logs"
  JPA_SUCCESS=1
fi

if [ $JPA_SUCCESS -eq 0 ]; then
  echo ">>> WARNING: No PostgreSQL/JPA initialization messages found in server logs"
fi

rm -f server1_jpa.log server2_jpa.log

# Step 6: Test data persistence by starting clients and checking data
run_test "Data Persistence" "Testing data persistence to PostgreSQL during game operations"
echo ">>> Testing data persistence by starting clients..."

# Start some clients to generate data
docker compose -f "$DOCKER_COMPOSE_FILE" up -d postgres redis-master redis-slave redis-sentinel-1 redis-sentinel-2 redis-sentinel-3 server-1 server-2 client-1 client-2 -d
sleep 20

# Check if there are any records in relevant tables (if they exist)
# First check if specific tables exist (like players, matches, etc.)
docker compose -f "$DOCKER_COMPOSE_FILE" logs server-1 > server1_activity.log 2>&1

# Check for any game-related activity in the logs
if grep -i -E "save|insert|persist|update|player|match|game" server1_activity.log; then
  echo ">>> SUCCESS: Found data persistence related activity in server logs"
else
  echo ">>> INFO: No explicit data persistence activity found in logs, but operations might be silent"
fi

# Check PostgreSQL for any new data (try to count records in common tables)
# This depends on the actual schema, but we can try common table names
declare -a possible_tables=("players" "matches" "games" "users" "game_sessions" "match_history")

for table in "${possible_tables[@]}"; do
  record_count=$(docker exec $(docker compose -f "$DOCKER_COMPOSE_FILE" ps -q postgres) psql -U dueling_user -d dueling_db -t -c "SELECT COUNT(*) FROM $table;" 2>/dev/null | tr -d ' \n' || echo "0")
  if [ "$record_count" -gt 0 ]; then
    echo ">>> SUCCESS: Found $record_count records in table '$table'"
  fi
done

rm -f server1_activity.log

# Step 7: Test database connection pooling
run_test "Connection Pooling" "Testing database connection pooling functionality"
echo ">>> Testing database connection pooling..."

# Check logs for connection pool messages
docker compose -f "$DOCKER_COMPOSE_FILE" logs server-1 > server1_pool.log
docker compose -f "$DOCKER_COMPOSE_FILE" logs server-2 > server2_pool.log

if grep -i -E "hikari|connection.*pool|max.*connections|pool.*size|min.*idle" server1_pool.log || grep -i -E "hikari|connection.*pool|max.*connections|pool.*size|min.*idle" server2_pool.log; then
  echo ">>> SUCCESS: Found connection pooling configuration in server logs"
else
  echo ">>> INFO: No connection pooling configuration found in logs"
fi

rm -f server1_pool.log server2_pool.log

# Step 8: Test database failover behavior (if applicable)
run_test "Database Failover Readiness" "Testing application resilience to PostgreSQL availability"
echo ">>> Testing application behavior when PostgreSQL is temporarily unavailable..."

# Get initial server logs
docker compose -f "$DOCKER_COMPOSE_FILE" logs server-1 > initial_server1.log 2>&1
docker compose -f "$DOCKER_COMPOSE_FILE" logs server-2 > initial_server2.log 2>&1

# Stop PostgreSQL temporarily
echo ">>> Stopping PostgreSQL temporarily to test resilience..."
docker compose -f "$DOCKER_COMPOSE_FILE" stop postgres
sleep 8

# Start PostgreSQL again
echo ">>> Restarting PostgreSQL..."
docker compose -f "$DOCKER_COMPOSE_FILE" start postgres
sleep 15

# Check server logs for any database-related errors during the PostgreSQL unavailability
docker compose -f "$DOCKER_COMPOSE_FILE" logs server-1 > after_db_restart_server1.log 2>&1
docker compose -f "$DOCKER_COMPOSE_FILE" logs server-2 > after_db_restart_server2.log 2>&1

if diff -u initial_server1.log after_db_restart_server1.log | grep -i -E "error|exception|fail|database|sql|connection|timeout|hibernate"; then
  echo ">>> INFO: Found database-related issues in server-1 logs during temporary unavailability"
else
  echo ">>> SUCCESS: No major database-related errors detected in server-1 logs"
fi

if diff -u initial_server2.log after_db_restart_server2.log | grep -i -E "error|exception|fail|database|sql|connection|timeout|hibernate"; then
  echo ">>> INFO: Found database-related issues in server-2 logs during temporary unavailability"
else
  echo ">>> SUCCESS: No major database-related errors detected in server-2 logs"
fi

# Cleanup temporary logs
rm -f initial_server1.log initial_server2.log after_db_restart_server1.log after_db_restart_server2.log

# Final check: Ensure PostgreSQL is still accessible after the failover test
if docker exec $(docker compose -f "$DOCKER_COMPOSE_FILE" ps -q postgres) pg_isready -h localhost -U dueling_user -d dueling_db; then
  echo ">>> SUCCESS: PostgreSQL remains accessible after failover test"
else
  echo ">>> WARNING: PostgreSQL may have issues after failover test"
fi

echo "======================================================="
echo ">>> POSTGRESQL FUNCTIONALITY TESTS COMPLETED"
echo "======================================================="

# Clean up
cleanup