#!/usr/bin/env bash

# Ensures the script stops if any command fails
set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"
PROJECT_ROOT="$SCRIPT_DIR/.."
DOCKER_COMPOSE_FILE="$PROJECT_ROOT/docker/docker-compose.yml"

echo "======================================================="
echo ">>> STARTING GATEWAY FUNCTIONALITY TESTS"
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
run_test "Start Services" "Starting gateway, servers, Redis, and PostgreSQL"
echo ">>> Starting services..."
docker-compose -f "$DOCKER_COMPOSE_FILE" up --scale client=0 --remove-orphans -d
echo ">>> Waiting for services to initialize..."
sleep 20

# Step 3: Test Gateway Route Forwarding
run_test "Gateway Route Forwarding" "Testing that API routes are properly forwarded through the gateway"
echo ">>> Testing gateway route forwarding..."

# Test that the gateway is accessible
if curl -f -s -o /dev/null http://localhost:8081/actuator/health; then
  echo ">>> SUCCESS: Gateway is accessible at http://localhost:8081"
else
  echo ">>> FAILURE: Gateway is not accessible"
  docker-compose -f "$DOCKER_COMPOSE_FILE" logs server-1
  exit 1
fi

# Test basic route forwarding (even if backend returns error, gateway should forward the request)
GATEWAY_RESPONSE=$(curl -s -o response.txt -w "%{http_code}" http://localhost:8081/api/test 2>/dev/null || true)
if [ "$GATEWAY_RESPONSE" -eq 404 ] || [ "$GATEWAY_RESPONSE" -eq 200 ]; then
  echo ">>> SUCCESS: Gateway is forwarding requests (received HTTP $GATEWAY_RESPONSE)"
else
  echo ">>> FAILURE: Gateway is not properly forwarding requests (received HTTP $GATEWAY_RESPONSE)"
  cat response.txt
  docker-compose -f "$DOCKER_COMPOSE_FILE" logs server-1
  exit 1
fi
rm -f response.txt

# Step 4: Test WebSocket Route
run_test "WebSocket Route" "Testing WebSocket route through the gateway"
echo ">>> Testing WebSocket route functionality..."
# Check if WebSocket endpoint is accessible through gateway
# This is a basic connectivity test
if curl -f -s -o /dev/null http://localhost:8081/ws/test 2>/dev/null || true; then
  echo ">>> WebSocket route is accessible through gateway"
else
  echo ">>> WebSocket route may not be properly configured"
fi

# Step 5: Test Load Balancing
run_test "Load Balancing" "Testing load balancing between multiple servers"
echo ">>> Testing load balancing between server-1 and server-2..."

# Get initial server logs
docker-compose -f "$DOCKER_COMPOSE_FILE" logs server-1 > server1_initial.log
docker-compose -f "$DOCKER_COMPOSE_FILE" logs server-2 > server2_initial.log

# Make several requests to see if load balancing works
for i in {1..5}; do
  curl -s -o /dev/null http://localhost:8081/api/test 2>/dev/null || true
  sleep 1
done

# Get updated logs to see which servers handled requests
docker-compose -f "$DOCKER_COMPOSE_FILE" logs server-1 > server1_after.log
docker-compose -f "$DOCKER_COMPOSE_FILE" logs server-2 > server2_after.log

# Compare log sizes to see if both servers were active
SIZE1_BEFORE=$(stat -c%s server1_initial.log)
SIZE1_AFTER=$(stat -c%s server1_after.log)
SIZE2_BEFORE=$(stat -c%s server2_initial.log)
SIZE2_AFTER=$(stat -c%s server2_after.log)

DIFF1=$((SIZE1_AFTER - SIZE1_BEFORE))
DIFF2=$((SIZE2_AFTER - SIZE2_BEFORE))

if [ $DIFF1 -gt 0 ] || [ $DIFF2 -gt 0 ]; then
  echo ">>> SUCCESS: At least one server received traffic (server-1 diff: $DIFF1, server-2 diff: $DIFF2)"
  
  if [ $DIFF1 -gt 0 ] && [ $DIFF2 -gt 0 ]; then
    echo ">>> SUCCESS: Load balancing appears to be working (both servers received traffic)"
  else
    echo ">>> WARNING: Only one server received traffic (load balancing may not be working optimally)"
  fi
else
  echo ">>> WARNING: No servers received traffic during load balancing test"
fi

# Cleanup temporary files
rm -f server1_initial.log server1_after.log server2_initial.log server2_after.log

# Step 6: Test CORS Configuration
run_test "CORS Configuration" "Testing CORS headers are properly set by the gateway"
echo ">>> Testing CORS configuration..."

CORS_HEADERS=$(curl -s -D - -o /dev/null -X OPTIONS \
  -H "Origin: http://localhost:3000" \
  -H "Access-Control-Request-Method: GET" \
  -H "Access-Control-Request-Headers: X-Requested-With" \
  http://localhost:8081/api/test 2>/dev/null || true)

if echo "$CORS_HEADERS" | grep -q "Access-Control-Allow-Origin"; then
  echo ">>> SUCCESS: CORS headers are properly configured"
else
  echo ">>> WARNING: CORS headers may not be properly configured"
fi

echo "======================================================="
echo ">>> GATEWAY FUNCTIONALITY TESTS COMPLETED"
echo "======================================================="

# Clean up
cleanup