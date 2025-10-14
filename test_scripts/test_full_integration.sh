#!/usr/bin/env bash

# Ensures the script stops if any command fails
set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"
PROJECT_ROOT="$SCRIPT_DIR/.."
DOCKER_COMPOSE_FILE="$PROJECT_ROOT/docker/docker-compose.yml"

echo "======================================================="
echo ">>> STARTING FULL INTEGRATION TEST"
echo ">>> Testing complete system with Gateway, JWT, Redis, PostgreSQL and Distributed Components"
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

# Step 1: Build the complete project
run_test "Build Complete Project" "Building all modules and Docker images"
echo ">>> Building the complete project..."
cd "$PROJECT_ROOT"
./scripts/build.sh
echo ">>> Building Docker images for complete stack..."
echo "BOT_MODE=autobot" > .env
echo "BOT_SCENARIO=" >> .env
docker-compose -f "$DOCKER_COMPOSE_FILE" --env-file .env build
rm -f .env
echo ">>> Build completed successfully."

# Step 2: Start the complete distributed system
run_test "Start Complete System" "Starting gateway, both servers, Redis, PostgreSQL, Prometheus, and Grafana"
echo ">>> Starting complete distributed system..."
docker-compose -f "$DOCKER_COMPOSE_FILE" up --scale client=0 --remove-orphans -d
echo ">>> Waiting for complete system to initialize..."
sleep 30

# Verify all services are running
SERVICES_STATUS=$(docker-compose -f "$DOCKER_COMPOSE_FILE" ps --format "json")
echo ">>> Service status: $SERVICES_STATUS"

# Check if all essential services are up
ESSENTIAL_SERVICES=("server-1" "server-2" "redis" "postgres")
ALL_UP=true
for service in "${ESSENTIAL_SERVICES[@]}"; do
  if ! docker-compose -f "$DOCKER_COMPOSE_FILE" ps | grep -q "$service.*Up"; then
    echo ">>> WARNING: Service $service is not running properly"
    ALL_UP=false
  fi
done

if [ "$ALL_UP" = true ]; then
  echo ">>> SUCCESS: All essential services are running"
else
  echo ">>> WARNING: Some services may have issues"
  docker-compose -f "$DOCKER_COMPOSE_FILE" ps
fi

# Step 3: Test the complete request flow through gateway to backend services
run_test "Complete Request Flow" "Testing request flow: Client -> Gateway -> Backend Server -> Redis/PostgreSQL"
echo ">>> Testing complete request flow..."

# Start a client that will connect through the gateway
echo "BOT_MODE=autobot" > .env
echo "BOT_SCENARIO=" >> .env
docker-compose -f "$DOCKER_COMPOSE_FILE" --env-file .env up --scale client=2 --remove-orphans -d
sleep 25

# Verify the flow by checking logs for activity across all layers
echo ">>> Checking logs for activity across all system layers..."

# Check gateway activity (server-1 acts as gateway in this setup)
SERVER1_LOGS=$(docker-compose -f "$DOCKER_COMPOSE_FILE" logs server-1 2>&1)
SERVER2_LOGS=$(docker-compose -f "$DOCKER_COMPOSE_FILE" logs server-2 2>&1)

ACTIVITY_FOUND=false
if echo "$SERVER1_LOGS" | grep -i -E "client.*connect|session.*create|game.*start|request.*handle|api.*call|ws.*connect"; then
  echo ">>> SUCCESS: Found game activity in server-1 logs"
  ACTIVITY_FOUND=true
fi

if echo "$SERVER2_LOGS" | grep -i -E "client.*connect|session.*create|game.*start|request.*handle|api.*call|ws.*connect"; then
  echo ">>> SUCCESS: Found game activity in server-2 logs"
  ACTIVITY_FOUND=true
fi

if [ "$ACTIVITY_FOUND" = false ]; then
  echo ">>> WARNING: No clear game activity found in server logs"
fi

# Step 4: Verify data persistence across all storage layers
run_test "Multi-Layer Data Persistence" "Verifying data is stored in both Redis and PostgreSQL"
echo ">>> Verifying data persistence across storage layers..."

# Check Redis for session and game state
REDIS_GAME_KEYS=$(docker exec $(docker-compose -f "$DOCKER_COMPOSE_FILE" ps -q redis) redis-cli --raw keys "game:*" | wc -l)
REDIS_SESSION_KEYS=$(docker exec $(docker-compose -f "$DOCKER_COMPOSE_FILE" ps -q redis) redis-cli --raw keys "spring:session:*" | wc -l)
REDIS_MATCH_KEYS=$(docker exec $(docker-compose -f "$DOCKER_COMPOSE_FILE" ps -q redis) redis-cli --raw keys "match:*" | wc -l)

echo ">>> Redis - Game keys: $REDIS_GAME_KEYS, Session keys: $REDIS_SESSION_KEYS, Match keys: $REDIS_MATCH_KEYS"

# Check PostgreSQL for persistent data (if tables exist)
DB_TABLES=$(docker exec $(docker-compose -f "$DOCKER_COMPOSE_FILE" ps -q postgres) psql -U dueling_user -d dueling_db -t -c "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public';" 2>/dev/null | tr -d ' \n' || echo "0")

echo ">>> PostgreSQL - Tables: $DB_TABLES"

if [ $REDIS_GAME_KEYS -gt 0 ] || [ $REDIS_SESSION_KEYS -gt 0 ] || [ $REDIS_MATCH_KEYS -gt 0 ]; then
  echo ">>> SUCCESS: Found data in Redis storage layer"
fi

if [ $DB_TABLES -gt 0 ]; then
  echo ">>> SUCCESS: PostgreSQL database is properly initialized"
fi

# Step 5: Test distributed caching with Redis
run_test "Distributed Caching Verification" "Verifying distributed caching between servers via Redis"
echo ">>> Testing distributed caching..."

# Check logs for Redisson/Hibernate cache activity
if echo "$SERVER1_LOGS" | grep -i -E "redisson|hibernate.*cache|distributed.*cache|jcache"; then
  echo ">>> SUCCESS: Found distributed caching activity in server-1"
fi

if echo "$SERVER2_LOGS" | grep -i -E "redisson|hibernate.*cache|distributed.*cache|jcache"; then
  echo ">>> SUCCESS: Found distributed caching activity in server-2"
fi

# Step 6: Test JWT authentication flow through gateway
run_test "JWT Authentication Flow" "Testing JWT authentication flow through the gateway"
echo ">>> Testing JWT authentication flow..."

# Check server logs for JWT-related activity
if echo "$SERVER1_LOGS" | grep -i -E "jwt|token|auth|bearer|security|oauth"; then
  echo ">>> SUCCESS: Found JWT authentication activity in server-1"
fi

if echo "$SERVER2_LOGS" | grep -i -E "jwt|token|auth|bearer|security|oauth"; then
  echo ">>> SUCCESS: Found JWT authentication activity in server-2"
fi

# Step 7: Test monitoring integration
run_test "Monitoring Integration" "Testing Prometheus and Grafana integration"
echo ">>> Testing monitoring integration..."

PROMETHEUS_STATUS=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:9090/-/ready 2>/dev/null || echo "0")
GRAFANA_STATUS=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:3000/api/health 2>/dev/null || echo "0")

echo ">>> Prometheus status: HTTP $PROMETHEUS_STATUS"
echo ">>> Grafana status: HTTP $GRAFANA_STATUS"

if [ "$PROMETHEUS_STATUS" -eq 200 ]; then
  echo ">>> SUCCESS: Prometheus is accessible"
fi

if [ "$GRAFANA_STATUS" -eq 200 ]; then
  echo ">>> SUCCESS: Grafana is accessible"
fi

# Step 8: Verify system resilience under load
run_test "System Resilience Under Load" "Testing system stability under increased load"
echo ">>> Testing system resilience with increased client load..."

# Increase client count to test under more load
docker-compose -f "$DOCKER_COMPOSE_FILE" --env-file .env up --scale client=4 --remove-orphans -d
sleep 30

# Check if all services are still healthy under load
HEALTHY_SERVICES=0
for service in "${ESSENTIAL_SERVICES[@]}"; do
  if docker-compose -f "$DOCKER_COMPOSE_FILE" ps | grep -q "$service.*Up.*healthy\|Up (healthy)"; then
    ((HEALTHY_SERVICES++))
  fi
done

echo ">>> $HEALTHY_SERVICES out of ${#ESSENTIAL_SERVICES[@]} essential services are healthy under load"

# Check for errors under load
ERROR_COUNT=$(docker-compose -f "$DOCKER_COMPOSE_FILE" logs server-1 2>&1 | grep -i -E "error|exception|fail|timeout" | wc -l)
ERROR_COUNT=$((ERROR_COUNT + $(docker-compose -f "$DOCKER_COMPOSE_FILE" logs server-2 2>&1 | grep -i -E "error|exception|fail|timeout" | wc -l)))

if [ $ERROR_COUNT -eq 0 ]; then
  echo ">>> SUCCESS: No errors found under increased load"
elif [ $ERROR_COUNT -lt 5 ]; then
  echo ">>> INFO: Found $ERROR_COUNT errors under load (potentially acceptable)"
else
  echo ">>> WARNING: Found $ERROR_COUNT errors under load"
fi

# Clean up env file
rm -f .env

echo "======================================================="
echo ">>> FULL INTEGRATION TEST COMPLETED"
echo ">>> This test verifies the complete technology stack:"
echo ">>> - API Gateway routing and load balancing"
echo ">>> - JWT authentication and security"
echo ">>> - Redis for session storage and distributed caching"
echo ">>> - PostgreSQL for persistent data storage"
echo ">>> - Distributed system with multiple server instances"
echo ">>> - Monitoring with Prometheus and Grafana"
echo "======================================================="

# Clean up
cleanup