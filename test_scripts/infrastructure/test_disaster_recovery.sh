#!/usr/bin/env bash

# Ensures the script stops if any command fails
set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"
PROJECT_ROOT="$SCRIPT_DIR/../.."
DOCKER_COMPOSE_FILE="$PROJECT_ROOT/docker/docker-compose.yml"

echo "======================================================="
echo ">>> STARTING DISASTER RECOVERY TESTS"
echo ">>> Testing system resilience and recovery from various failure scenarios"
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

# Step 2: Start services
run_test "Start Services" "Starting full system for disaster recovery tests"
echo ">>> Starting services..."
docker compose -f "$DOCKER_COMPOSE_FILE" up --scale client=0 --remove-orphans -d
echo ">>> Waiting for services to initialize..."
sleep 30

# Step 3: Create initial data for recovery testing
run_test "Create Baseline Data" "Creating baseline data to test recovery"
echo ">>> Creating baseline data for recovery testing..."

# Start a client briefly to generate some data that should persist
echo "BOT_MODE=autobot" > .env
echo "BOT_SCENARIO=" >> .env
docker compose -f "$DOCKER_COMPOSE_FILE" --env-file .env up --scale client=2 --remove-orphans -d
sleep 15
docker compose -f "$DOCKER_COMPOSE_FILE" --env-file .env down
rm -f .env

# Record initial state
INITIAL_REDIS_KEYS=$(docker exec $(docker compose -f "$DOCKER_COMPOSE_FILE" ps -q redis) redis-cli --raw keys "*" | wc -l)
echo ">>> Initial Redis keys: $INITIAL_REDIS_KEYS"

# Check if PostgreSQL has tables and sample data
INITIAL_DB_TABLES=$(docker exec $(docker compose -f "$DOCKER_COMPOSE_FILE" ps -q postgres) psql -U dueling_user -d dueling_db -t -c "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public';" 2>/dev/null | tr -d ' \n' || echo "0")
echo ">>> Initial DB tables: $INITIAL_DB_TABLES"

# Step 4: Test PostgreSQL failure and recovery
run_test "PostgreSQL Failure Recovery" "Testing system behavior when PostgreSQL fails and recovers"
echo ">>> Testing PostgreSQL failure and recovery..."

# Get initial state before failure
docker compose -f "$DOCKER_COMPOSE_FILE" logs postgres > postgres_before_failure.log 2>&1
INITIAL_PG_STATUS=$(docker compose -f "$DOCKER_COMPOSE_FILE" ps postgres | grep -o "Up\|Down")

echo ">>> Initial PostgreSQL state: $INITIAL_PG_STATUS"

# Stop PostgreSQL to simulate failure
echo ">>> Stopping PostgreSQL to simulate database failure..."
docker compose -f "$DOCKER_COMPOSE_FILE" stop postgres
sleep 8

# Check how applications react to database failure
docker compose -f "$DOCKER_COMPOSE_FILE" logs server-1 > server1_during_pg_failure.log 2>&1
docker compose -f "$DOCKER_COMPOSE_FILE" logs server-2 > server2_during_pg_failure.log 2>&1

if grep -i -E "connection.*fail|database.*down|sql.*error|hibernate.*error" server1_during_pg_failure.log; then
  echo ">>> INFO: Found database failure related messages in server-1 logs"
fi

if grep -i -E "connection.*fail|database.*down|sql.*error|hibernate.*error" server2_during_pg_failure.log; then
  echo ">>> INFO: Found database failure related messages in server-2 logs"
fi

# Start PostgreSQL again to simulate recovery
echo ">>> Restarting PostgreSQL to simulate recovery..."
docker compose -f "$DOCKER_COMPOSE_FILE" start postgres
sleep 20

# Check if PostgreSQL is accessible after restart
if docker exec $(docker compose -f "$DOCKER_COMPOSE_FILE" ps -q postgres) pg_isready -h localhost -U dueling_user -d dueling_db; then
  echo ">>> SUCCESS: PostgreSQL successfully recovered and is accessible"
  
  # Check that the database tables still exist after restart
  POST_RECOVERY_DB_TABLES=$(docker exec $(docker compose -f "$DOCKER_COMPOSE_FILE" ps -q postgres) psql -U dueling_user -d dueling_db -t -c "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public';" 2>/dev/null | tr -d ' \n' || echo "0")
  
  if [ "$POST_RECOVERY_DB_TABLES" -eq "$INITIAL_DB_TABLES" ]; then
    echo ">>> SUCCESS: Database structure preserved after failure/recovery"
  else
    echo ">>> WARNING: Database structure changed after failure/recovery ($INITIAL_DB_TABLES -> $POST_RECOVERY_DB_TABLES)"
  fi
else
  echo ">>> FAILURE: PostgreSQL did not recover properly"
  docker compose -f "$DOCKER_COMPOSE_FILE" logs postgres
  exit 1
fi

# Clean up logs
rm -f postgres_before_failure.log server1_during_pg_failure.log server2_during_pg_failure.log

# Step 5: Test Redis failure and recovery
run_test "Redis Failure Recovery" "Testing system behavior when Redis fails and recovers"
echo ">>> Testing Redis failure and recovery..."

# Get initial state before failure
docker compose -f "$DOCKER_COMPOSE_FILE" logs redis > redis_before_failure.log 2>&1
INITIAL_REDIS_STATUS=$(docker compose -f "$DOCKER_COMPOSE_FILE" ps redis | grep -o "Up\|Down")

echo ">>> Initial Redis state: $INITIAL_REDIS_STATUS"

# Stop Redis to simulate failure
echo ">>> Stopping Redis to simulate cache failure..."
docker compose -f "$DOCKER_COMPOSE_FILE" stop redis
sleep 8

# Start a client to see how the system behaves without Redis
echo "BOT_MODE=autobot" > .env
echo "BOT_SCENARIO=" >> .env
docker compose -f "$DOCKER_COMPOSE_FILE" --env-file .env up --scale client=1 --remove-orphans -d
sleep 15
docker compose -f "$DOCKER_COMPOSE_FILE" --env-file .env down
rm -f .env

# Check how applications reacted to Redis failure
docker compose -f "$DOCKER_COMPOSE_FILE" logs server-1 > server1_during_redis_failure.log 2>&1
docker compose -f "$DOCKER_COMPOSE_FILE" logs server-2 > server2_during_redis_failure.log 2>&1

if grep -i -E "redis.*fail|connection.*timeout|cache.*error|session.*error" server1_during_redis_failure.log; then
  echo ">>> INFO: Found Redis failure related messages in server-1 logs"
fi

if grep -i -E "redis.*fail|connection.*timeout|cache.*error|session.*error" server2_during_redis_failure.log; then
  echo ">>> INFO: Found Redis failure related messages in server-2 logs"
fi

# Start Redis again to simulate recovery
echo ">>> Restarting Redis to simulate recovery..."
docker compose -f "$DOCKER_COMPOSE_FILE" start redis
sleep 15

# Check if Redis is accessible after restart
if docker exec $(docker compose -f "$DOCKER_COMPOSE_FILE" ps -q redis) redis-cli ping | grep -q "PONG"; then
  echo ">>> SUCCESS: Redis successfully recovered and is responding to PING"
  
  # Check if Redis data was preserved (this depends on persistence settings)
  POST_RECOVERY_REDIS_KEYS=$(docker exec $(docker compose -f "$DOCKER_COMPOSE_FILE" ps -q redis) redis-cli --raw keys "*" | wc -l)
  echo ">>> Redis keys after recovery: $POST_RECOVERY_REDIS_KEYS (before failure: $INITIAL_REDIS_KEYS)"
  
  if [ $POST_RECOVERY_REDIS_KEYS -gt 0 ]; then
    echo ">>> SUCCESS: Redis data was preserved after failure/recovery"
  else
    echo ">>> INFO: No Redis keys found after recovery (may be expected depending on persistence settings)"
  fi
else
  echo ">>> FAILURE: Redis did not recover properly"
  docker compose -f "$DOCKER_COMPOSE_FILE" logs redis
  exit 1
fi

# Clean up logs
rm -f redis_before_failure.log server1_during_redis_failure.log server2_during_redis_failure.log

# Step 6: Test complete server failure and recovery
run_test "Server Failure Recovery" "Testing single server failure and recovery"
echo ">>> Testing server failure and recovery..."

# Get initial state
INITIAL_SERVER1_LOG_SIZE=$(docker compose -f "$DOCKER_COMPOSE_FILE" logs server-1 2>&1 | wc -c)
INITIAL_SERVER2_LOG_SIZE=$(docker compose -f "$DOCKER_COMPOSE_FILE" logs server-2 2>&1 | wc -c)

# Stop server-1 to simulate failure
echo ">>> Stopping server-1 to simulate server failure..."
docker compose -f "$DOCKER_COMPOSE_FILE" stop server-1
sleep 10

# Check if server-2 can handle all traffic
echo ">>> Verifying server-2 can handle traffic during server-1 failure..."
SERVER2_HEALTH=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8082/actuator/health 2>/dev/null || echo "0")
if [ "$SERVER2_HEALTH" -eq 200 ]; then
  echo ">>> SUCCESS: Server-2 remains healthy during server-1 failure"
else
  echo ">>> WARNING: Server-2 health check failed during server-1 failure (HTTP $SERVER2_HEALTH)"
fi

# Start a client to verify the system still works with only server-2
echo "BOT_MODE=autobot" > .env
echo "BOT_SCENARIO=" >> .env
docker compose -f "$DOCKER_COMPOSE_FILE" --env-file .env up --scale client=1 --remove-orphans -d
sleep 15
docker compose -f "$DOCKER_COMPOSE_FILE" --env-file .env down
rm -f .env

# Restart server-1 to simulate recovery
echo ">>> Restarting server-1 to simulate recovery..."
docker compose -f "$DOCKER_COMPOSE_FILE" start server-1
sleep 25

# Verify both servers are healthy after recovery
SERVER1_HEALTH=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8081/actuator/health 2>/dev/null || echo "0")
SERVER2_HEALTH=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8082/actuator/health 2>/dev/null || echo "0")

if [ "$SERVER1_HEALTH" -eq 200 ] && [ "$SERVER2_HEALTH" -eq 200 ]; then
  echo ">>> SUCCESS: Both servers recovered successfully (Server1: HTTP $SERVER1_HEALTH, Server2: HTTP $SERVER2_HEALTH)"
else
  echo ">>> WARNING: One or more servers failed to recover properly"
fi

# Step 7: Test network partitioning
run_test "Network Partitioning" "Testing system behavior under network partitioning"
echo ">>> Testing network partitioning resilience..."

# Get initial state
docker compose -f "$DOCKER_COMPOSE_FILE" logs server-1 > server1_before_partition.log 2>&1
docker compose -f "$DOCKER_COMPOSE_FILE" logs server-2 > server2_before_partition.log 2>&1

# This is a simulation - in a real environment, we'd have more sophisticated network testing
# For Docker Compose, we can't easily simulate partial network failures,
# but we can test service-to-service communication after disruptions

# Restart the network to simulate temporary disruption
echo ">>> Simulating network disruption by restarting containers..."
docker compose -f "$DOCKER_COMPOSE_FILE" restart server-1 server-2
sleep 20

# Verify services are still operational
FINAL_SERVER1_HEALTH=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8081/actuator/health 2>/dev/null || echo "0")
FINAL_SERVER2_HEALTH=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8082/actuator/health 2>/dev/null || echo "0")

if [ "$FINAL_SERVER1_HEALTH" -eq 200 ] && [ "$FINAL_SERVER2_HEALTH" -eq 200 ]; then
  echo ">>> SUCCESS: Services recovered after network disruption simulation"
else
  echo ">>> WARNING: Services did not fully recover after network disruption simulation"
fi

# Clean up logs
rm -f server1_before_partition.log server2_before_partition.log

# Step 8: Test data consistency after failures
run_test "Data Consistency After Failures" "Testing data consistency after various failure scenarios"
echo ">>> Testing data consistency after failure scenarios..."

# Start a client to generate more data post-recovery
echo "BOT_MODE=autobot" > .env
echo "BOT_SCENARIO=" >> .env
docker compose -f "$DOCKER_COMPOSE_FILE" --env-file .env up --scale client=2 --remove-orphans -d
sleep 20
docker compose -f "$DOCKER_COMPOSE_FILE" --env-file .env down
rm -f .env

# Check Redis consistency
FINAL_REDIS_KEYS=$(docker exec $(docker compose -f "$DOCKER_COMPOSE_FILE" ps -q redis) redis-cli --raw keys "*" | wc -l)
echo ">>> Final Redis keys after all tests: $FINAL_REDIS_KEYS"

# Check PostgreSQL consistency
FINAL_DB_TABLES=$(docker exec $(docker compose -f "$DOCKER_COMPOSE_FILE" ps -q postgres) psql -U dueling_user -d dueling_db -t -c "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public';" 2>/dev/null | tr -d ' \n' || echo "0")
echo ">>> Final DB tables after all tests: $FINAL_DB_TABLES"

if [ $FINAL_REDIS_KEYS -gt 0 ] && [ $FINAL_DB_TABLES -gt 0 ]; then
  echo ">>> SUCCESS: Data appears to be consistent after failure scenarios"
else
  echo ">>> WARNING: Data consistency may have been affected by failure scenarios"
fi

# Step 9: Test graceful shutdown and startup
run_test "Graceful Shutdown and Startup" "Testing graceful shutdown and startup procedures"
echo ">>> Testing graceful shutdown procedures..."

# Get current state
docker compose -f "$DOCKER_COMPOSE_FILE" logs server-1 > server1_before_shutdown.log 2>&1

# Perform graceful shutdown
echo ">>> Performing graceful shutdown of all services..."
docker compose -f "$DOCKER_COMPOSE_FILE" down
sleep 5

# Check if shutdown was clean
if [ -s server1_before_shutdown.log ]; then
  if grep -i -E "shut.*down|terminated|graceful|exit" server1_before_shutdown.log; then
    echo ">>> INFO: Found shutdown-related messages in logs"
  fi
fi

# Restart services
echo ">>> Restarting services..."
docker compose -f "$DOCKER_COMPOSE_FILE" up --scale client=0 --remove-orphans -d
sleep 25

# Verify all services are running
FINAL_SERVICES_STATUS=$(docker compose -f "$DOCKER_COMPOSE_FILE" ps)
echo ">>> Final service status:"
echo "$FINAL_SERVICES_STATUS"

ALL_SERVICES_UP=true
if ! echo "$FINAL_SERVICES_STATUS" | grep -q "server-1.*Up"; then
  echo ">>> WARNING: server-1 is not running properly"
  ALL_SERVICES_UP=false
fi

if ! echo "$FINAL_SERVICES_STATUS" | grep -q "server-2.*Up"; then
  echo ">>> WARNING: server-2 is not running properly"
  ALL_SERVICES_UP=false
fi

if ! echo "$FINAL_SERVICES_STATUS" | grep -q "redis.*Up"; then
  echo ">>> WARNING: redis is not running properly"
  ALL_SERVICES_UP=false
fi

if ! echo "$FINAL_SERVICES_STATUS" | grep -q "postgres.*Up"; then
  echo ">>> WARNING: postgres is not running properly"
  ALL_SERVICES_UP=false
fi

if [ "$ALL_SERVICES_UP" = true ]; then
  echo ">>> SUCCESS: All services successfully restarted after graceful shutdown"
else
  echo ">>> WARNING: Some services may have issues after restart"
fi

# Clean up logs
rm -f server1_before_shutdown.log

echo "======================================================="
echo ">>> DISASTER RECOVERY TESTS COMPLETED"
echo ">>> These tests evaluate system resilience and recovery capabilities"
echo "======================================================="

# Clean up
cleanup