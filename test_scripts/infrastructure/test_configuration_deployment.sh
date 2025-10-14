#!/usr/bin/env bash

# Ensures the script stops if any command fails
set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"
PROJECT_ROOT="$SCRIPT_DIR/.."
DOCKER_COMPOSE_FILE="$PROJECT_ROOT/docker/docker-compose.yml"

echo "======================================================="
echo ">>> STARTING CONFIGURATION AND DEPLOYMENT TESTS"
echo ">>> Testing system configuration and deployment procedures"
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

# Step 1: Test build process
run_test "Build Process Validation" "Testing that the build process completes successfully"
echo ">>> Testing build process..."

# Build the project using Maven
cd "$PROJECT_ROOT"
BUILD_RESULT=$(./scripts/build.sh 2>&1 | tee build_output.log)
BUILD_EXIT_CODE=${PIPESTATUS[0]}

if [ $BUILD_EXIT_CODE -eq 0 ]; then
  echo ">>> SUCCESS: Build process completed successfully"
  
  # Verify JAR files were created
  if [ -f "dueling-server/target/dueling-server-1.0-SNAPSHOT.jar" ]; then
    echo ">>> SUCCESS: Server JAR file created"
  else
    echo ">>> FAILURE: Server JAR file not found"
    exit 1
  fi
  
  if [ -f "dueling-gateway/target/dueling-gateway-1.0-SNAPSHOT.jar" ]; then
    echo ">>> SUCCESS: Gateway JAR file created"
  else
    echo ">>> FAILURE: Gateway JAR file not found"
    exit 1
  fi
  
  if [ -f "dueling-client/target/dueling-client-1.0-SNAPSHOT.jar" ]; then
    echo ">>> SUCCESS: Client JAR file created"
  else
    echo ">>> INFO: Client JAR file not found (may be expected)"
  fi
else
  echo ">>> FAILURE: Build process failed"
  cat build_output.log
  exit 1
fi

rm -f build_output.log

# Step 2: Test Docker image creation
run_test "Docker Image Creation" "Testing Docker image creation process"
echo ">>> Testing Docker image creation..."

# Create .env file for Docker build
echo "BOT_MODE=autobot" > .env
echo "BOT_SCENARIO=" >> .env

# Build Docker images
DOCKER_BUILD_RESULT=$(docker compose -f "$DOCKER_COMPOSE_FILE" --env-file .env build 2>&1 | tee docker_build_output.log)
DOCKER_BUILD_EXIT_CODE=${PIPESTATUS[0]}

if [ $DOCKER_BUILD_EXIT_CODE -eq 0 ]; then
  echo ">>> SUCCESS: Docker images built successfully"
  
  # Check if images were created
  if docker images | grep -q "dueling-protocol"; then
    echo ">>> SUCCESS: Main Docker image created"
  else
    echo ">>> FAILURE: Main Docker image not found"
    exit 1
  fi
else
  echo ">>> FAILURE: Docker image build failed"
  cat docker_build_output.log
  exit 1
fi

rm -f docker_build_output.log .env

# Step 3: Test environment variables and configuration
run_test "Environment Variables Validation" "Testing environment variables configuration"
echo ">>> Testing environment variables and configuration..."

# Check if the docker-compose.yml has the expected environment variables
if grep -q "REDIS_HOST" "$DOCKER_COMPOSE_FILE"; then
  echo ">>> SUCCESS: Redis environment variables configured"
else
  echo ">>> WARNING: Redis environment variables not found in docker-compose.yml"
fi

if grep -q "POSTGRES_HOST\|POSTGRES_DB\|POSTGRES_USER\|POSTGRES_PASSWORD" "$DOCKER_COMPOSE_FILE"; then
  echo ">>> SUCCESS: PostgreSQL environment variables configured"
else
  echo ">>> WARNING: PostgreSQL environment variables not found in docker-compose.yml"
fi

if grep -q "SERVER_PORT\|SERVER_NAME" "$DOCKER_COMPOSE_FILE"; then
  echo ">>> SUCCESS: Server environment variables configured"
else
  echo ">>> WARNING: Server environment variables not found in docker-compose.yml"
fi

# Step 4: Test Spring profiles configuration
run_test "Spring Profiles Configuration" "Testing Spring profiles activation"
echo ">>> Testing Spring profiles configuration..."

# Check the docker-compose.yml for profile activation
if grep -q "distributed.*distributed-db" "$DOCKER_COMPOSE_FILE"; then
  echo ">>> SUCCESS: Distributed profiles configured in docker compose"
else
  echo ">>> INFO: Distributed profiles not explicitly found in docker compose"
fi

# Step 5: Test service startup configuration
run_test "Service Startup Configuration" "Testing service startup and health checks"
echo ">>> Testing service startup configuration..."

# Start services with specific configuration
docker compose -f "$DOCKER_COMPOSE_FILE" up --scale client=0 --remove-orphans -d

# Wait for services with longer timeout than normal
echo ">>> Waiting for services to become healthy..."
sleep 40

# Check the health status for each service
SERVICES_STATUS=$(docker compose -f "$DOCKER_COMPOSE_FILE" ps --format "json")
echo ">>> Service status: $SERVICES_STATUS"

# Check individual service health
SERVER1_HEALTH_STATUS=$(docker compose -f "$DOCKER_COMPOSE_FILE" ps server-1 | awk '{print $NF}')
SERVER2_HEALTH_STATUS=$(docker compose -f "$DOCKER_COMPOSE_FILE" ps server-2 | awk '{print $NF}')
REDIS_HEALTH_STATUS=$(docker compose -f "$DOCKER_COMPOSE_FILE" ps redis | awk '{print $NF}')
POSTGRES_HEALTH_STATUS=$(docker compose -f "$DOCKER_COMPOSE_FILE" ps postgres | awk '{print $NF}')

echo ">>> Server-1 status: $SERVER1_HEALTH_STATUS"
echo ">>> Server-2 status: $SERVER2_HEALTH_STATUS"
echo ">>> Redis status: $REDIS_HEALTH_STATUS"
echo ">>> PostgreSQL status: $POSTGRES_HEALTH_STATUS"

# Verify all essential services are up
ALL_SERVICES_UP=true
if [[ "$SERVER1_HEALTH_STATUS" != *"Up"* ]]; then
  echo ">>> FAILURE: Server-1 is not running properly"
  ALL_SERVICES_UP=false
fi

if [[ "$SERVER2_HEALTH_STATUS" != *"Up"* ]]; then
  echo ">>> FAILURE: Server-2 is not running properly"
  ALL_SERVICES_UP=false
fi

if [[ "$REDIS_HEALTH_STATUS" != *"Up"* ]]; then
  echo ">>> FAILURE: Redis is not running properly"
  ALL_SERVICES_UP=false
fi

if [[ "$POSTGRES_HEALTH_STATUS" != *"Up"* ]]; then
  echo ">>> FAILURE: PostgreSQL is not running properly"
  ALL_SERVICES_UP=false
fi

if [ "$ALL_SERVICES_UP" = false ]; then
  echo ">>> FAILURE: Not all services are running properly"
  docker compose -f "$DOCKER_COMPOSE_FILE" logs
  exit 1
fi

echo ">>> SUCCESS: All services started successfully"

# Step 6: Test configuration validation through logs
run_test "Configuration Validation Through Logs" "Testing configuration through startup logs"
echo ">>> Testing configuration validation through startup logs..."

# Check server logs for configuration messages
docker compose -f "$DOCKER_COMPOSE_FILE" logs server-1 > server1_config.log 2>&1
docker compose -f "$DOCKER_COMPOSE_FILE" logs server-2 > server2_config.log 2>&1

# Look for configuration-related messages in logs
CONFIG_SUCCESS=0
if grep -i -E "profile.*active|config.*loaded|property.*resolved|startup.*complete|application.*run" server1_config.log; then
  echo ">>> SUCCESS: Found configuration messages in server-1 startup logs"
  CONFIG_SUCCESS=1
fi

if grep -i -E "profile.*active|config.*loaded|property.*resolved|startup.*complete|application.*run" server2_config.log; then
  echo ">>> SUCCESS: Found configuration messages in server-2 startup logs"
  CONFIG_SUCCESS=1
fi

if [ $CONFIG_SUCCESS -eq 0 ]; then
  echo ">>> WARNING: No clear configuration messages found in startup logs"
fi

# Check for Redis/PostgreSQL connection configuration
if grep -i -E "redis.*connect|database.*connect|datasource.*init" server1_config.log; then
  echo ">>> SUCCESS: Found database/Redis connection messages in server-1 logs"
fi

if grep -i -E "redis.*connect|database.*connect|datasource.*init" server2_config.log; then
  echo ">>> SUCCESS: Found database/Redis connection messages in server-2 logs"
fi

rm -f server1_config.log server2_config.log

# Step 7: Test health check endpoints
run_test "Health Check Validation" "Testing health check endpoints configuration"
echo ">>> Testing health check endpoints..."

# Test health endpoints for all services
SERVER1_HEALTH=$(curl -s -o server1_health.txt -w "%{http_code}" http://localhost:8081/actuator/health 2>/dev/null || echo "0")
SERVER2_HEALTH=$(curl -s -o server2_health.txt -w "%{http_code}" http://localhost:8082/actuator/health 2>/dev/null || echo "0")

echo ">>> Server-1 health check: HTTP $SERVER1_HEALTH"
echo ">>> Server-2 health check: HTTP $SERVER2_HEALTH"

if [ "$SERVER1_HEALTH" -eq 200 ]; then
  echo ">>> SUCCESS: Server-1 health endpoint is accessible"
else
  echo ">>> FAILURE: Server-1 health endpoint not accessible (HTTP $SERVER1_HEALTH)"
  exit 1
fi

if [ "$SERVER2_HEALTH" -eq 200 ]; then
  echo ">>> SUCCESS: Server-2 health endpoint is accessible"
else
  echo ">>> FAILURE: Server-2 health endpoint not accessible (HTTP $SERVER2_HEALTH)"
  exit 1
fi

# Check that the health response is valid JSON
if head -c 1 server1_health.txt | grep -qE '[{[]'; then
  echo ">>> SUCCESS: Server-1 health response is valid JSON"
else
  echo ">>> WARNING: Server-1 health response is not valid JSON"
fi

if head -c 1 server2_health.txt | grep -qE '[{[]'; then
  echo ">>> SUCCESS: Server-2 health response is valid JSON"
else
  echo ">>> WARNING: Server-2 health response is not valid JSON"
fi

rm -f server1_health.txt server2_health.txt

# Step 8: Test service configuration under load
run_test "Configuration Under Load" "Testing configuration stability under load"
echo ">>> Testing configuration stability under load..."

# Start clients to create some load while checking configuration
echo "BOT_MODE=autobot" > .env
echo "BOT_SCENARIO=" >> .env
docker compose -f "$DOCKER_COMPOSE_FILE" --env-file .env up --scale client=2 --remove-orphans -d
sleep 20

# Check that services are still properly configured under load
LOAD_SERVER1_HEALTH=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8081/actuator/health 2>/dev/null || echo "0")
LOAD_SERVER2_HEALTH=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8082/actuator/health 2>/dev/null || echo "0")

if [ "$LOAD_SERVER1_HEALTH" -eq 200 ] && [ "$LOAD_SERVER2_HEALTH" -eq 200 ]; then
  echo ">>> SUCCESS: Services remain properly configured under load"
else
  echo ">>> WARNING: Services may have configuration issues under load (Server1: $LOAD_SERVER1_HEALTH, Server2: $LOAD_SERVER2_HEALTH)"
fi

docker compose -f "$DOCKER_COMPOSE_FILE" --env-file .env down
rm -f .env

# Step 9: Test configuration file integrity
run_test "Configuration File Integrity" "Testing configuration file integrity and access"
echo ">>> Testing configuration file integrity..."

# Check for application.yml files in services
if [ -f "$PROJECT_ROOT/dueling-gateway/src/main/resources/application.yml" ]; then
  echo ">>> SUCCESS: Gateway application.yml found"
  
  # Check if gateway configuration contains expected elements
  if grep -q "spring.cloud.gateway" "$PROJECT_ROOT/dueling-gateway/src/main/resources/application.yml"; then
    echo ">>> SUCCESS: Gateway configuration contains cloud gateway settings"
  else
    echo ">>> WARNING: Gateway configuration doesn't contain expected cloud gateway settings"
  fi
else
  echo ">>> FAILURE: Gateway application.yml not found"
  exit 1
fi

if [ -f "$PROJECT_ROOT/dueling-server/src/main/resources/application.yml" ]; then
  echo ">>> SUCCESS: Server application.yml found"
  
  # Check if server configuration contains expected elements
  if grep -q "spring.datasource\|spring.jpa\|spring.redis" "$PROJECT_ROOT/dueling-server/src/main/resources/application.yml"; then
    echo ">>> SUCCESS: Server configuration contains expected data source settings"
  else
    echo ">>> WARNING: Server configuration doesn't contain expected data source settings"
  fi
else
  echo ">>> FAILURE: Server application.yml not found"
  exit 1
fi

# Step 10: Test deployment validation summary
run_test "Deployment Validation Summary" "Summary of deployment configuration validation"
echo ">>> Testing deployment validation..."

echo ">>> Checking deployment configuration files..."

# Verify Dockerfile exists and is properly configured
if [ -f "$PROJECT_ROOT/docker/Dockerfile" ]; then
  echo ">>> SUCCESS: Main Dockerfile found"
  
  # Check for required elements in Dockerfile
  if grep -q "FROM.*eclipse-temurin" "$PROJECT_ROOT/docker/Dockerfile"; then
    echo ">>> SUCCESS: Dockerfile uses Eclipse Temurin JDK"
  fi
  
  if grep -q "COPY.*app.jar" "$PROJECT_ROOT/docker/Dockerfile"; then
    echo ">>> SUCCESS: Dockerfile copies application JAR"
  fi
else
  echo ">>> FAILURE: Main Dockerfile not found"
  exit 1
fi

# Check for docker compose file
if [ -f "$DOCKER_COMPOSE_FILE" ]; then
  echo ">>> SUCCESS: Docker Compose file found"
  
  # Check for essential services in docker compose file
  if grep -q "server.*:" "$DOCKER_COMPOSE_FILE"; then
    echo ">>> SUCCESS: Server service defined in docker compose"
  fi
  
  if grep -q "redis:" "$DOCKER_COMPOSE_FILE"; then
    echo ">>> SUCCESS: Redis service defined in docker compose"
  fi
  
  if grep -q "postgres:" "$DOCKER_COMPOSE_FILE"; then
    echo ">>> SUCCESS: PostgreSQL service defined in docker compose"
  fi
else
  echo ">>> FAILURE: Docker Compose file not found"
  exit 1
fi

echo "======================================================="
echo ">>> CONFIGURATION AND DEPLOYMENT TESTS COMPLETED"
echo ">>> These tests validate system configuration and deployment procedures"
echo "======================================================="

# Clean up
cleanup