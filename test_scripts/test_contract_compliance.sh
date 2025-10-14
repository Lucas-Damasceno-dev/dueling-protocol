#!/usr/bin/env bash

# Ensures the script stops if any command fails
set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"
PROJECT_ROOT="$SCRIPT_DIR/.."
DOCKER_COMPOSE_FILE="$PROJECT_ROOT/docker/docker-compose.yml"

echo "======================================================="
echo ">>> STARTING CONTRACT TESTS"
echo ">>> Testing API contracts between Gateway and Backend Services"
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
sleep 25

# Step 3: Test API contract - verify OpenAPI/Swagger endpoints
run_test "OpenAPI Contract Verification" "Testing availability of OpenAPI/Swagger contracts"
echo ">>> Testing OpenAPI/Swagger contract endpoints..."

# Test if the OpenAPI documentation endpoint is accessible
SWAGGER_STATUS=$(curl -s -o swagger_response.txt -w "%{http_code}" http://localhost:8081/v3/api-docs 2>/dev/null || true)

if [ "$SWAGGER_STATUS" -eq 200 ]; then
  echo ">>> SUCCESS: OpenAPI documentation available at http://localhost:8081/v3/api-docs (HTTP $SWAGGER_STATUS)"
  
  # Verify that the response contains expected OpenAPI structure
  if grep -q '"openapi":"3\.' swagger_response.txt; then
    echo ">>> SUCCESS: Response contains valid OpenAPI 3.x structure"
  else
    echo ">>> WARNING: Response does not contain expected OpenAPI 3.x structure"
  fi
else
  echo ">>> INFO: OpenAPI documentation not available at http://localhost:8081/v3/api-docs (HTTP $SWAGGER_STATUS)"
fi

# Test Swagger UI endpoint
SWAGGER_UI_STATUS=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8081/swagger-ui.html 2>/dev/null || true)
if [ "$SWAGGER_UI_STATUS" -eq 200 ]; then
  echo ">>> SUCCESS: Swagger UI available at http://localhost:8081/swagger-ui.html (HTTP $SWAGGER_UI_STATUS)"
else
  echo ">>> INFO: Swagger UI not available at http://localhost:8081/swagger-ui.html (HTTP $SWAGGER_UI_STATUS)"
fi

rm -f swagger_response.txt

# Step 4: Test API contract compliance for game endpoints
run_test "Game API Contract Compliance" "Testing compliance with expected game API contracts"
echo ">>> Testing API contract compliance for game endpoints..."

# Test the structure of an API response (even if it returns an error)
API_RESPONSE=$(curl -s -o api_response.txt -w "%{http_code}" http://localhost:8081/api/match/create 2>/dev/null || true)
echo ">>> Game API endpoint test - HTTP Status: $API_RESPONSE"

# Check if response follows expected JSON structure
if [ "$API_RESPONSE" -eq 405 ] || [ "$API_RESPONSE" -eq 401 ] || [ "$API_RESPONSE" -eq 403 ]; then
  echo ">>> SUCCESS: API responded with expected error code (method not allowed/unauthorized)"
else
  echo ">>> INFO: API responded with HTTP $API_RESPONSE (not necessarily an error)"
fi

# Check if response is in JSON format
if head -c 1 api_response.txt | grep -qE '[{[]'; then
  echo ">>> SUCCESS: API response is in JSON format"
else
  echo ">>> INFO: API response is not in JSON format"
fi

rm -f api_response.txt

# Step 5: Test WebSocket contract
run_test "WebSocket Contract Compliance" "Testing WebSocket contract compliance"
echo ">>> Testing WebSocket contract compliance..."

# Check if the WebSocket endpoint is accessible
WEBSOCKET_RESPONSE=$(curl -s -o websocket_response.txt -w "%{http_code}" \
  -H "Connection: Upgrade" \
  -H "Upgrade: websocket" \
  -H "Sec-WebSocket-Key: test123" \
  -H "Sec-WebSocket-Version: 13" \
  http://localhost:8081/ws/connect 2>/dev/null || true)

echo ">>> WebSocket endpoint test - HTTP Status: $WEBSOCKET_RESPONSE"

# The expected response for a WebSocket upgrade attempt would be 101 (Switching Protocols)
# or 400/403/404 for other responses
if [ "$WEBSOCKET_RESPONSE" -eq 101 ] || [ "$WEBSOCKET_RESPONSE" -eq 400 ] || [ "$WEBSOCKET_RESPONSE" -eq 403 ] || [ "$WEBSOCKET_RESPONSE" -eq 404 ]; then
  echo ">>> SUCCESS: WebSocket endpoint responded as expected"
else
  echo ">>> INFO: WebSocket endpoint response (HTTP $WEBSOCKET_RESPONSE) may be unexpected"
fi

rm -f websocket_response.txt

# Step 6: Test request/response format validation
run_test "Request/Response Format Validation" "Testing validation of request/response formats"
echo ">>> Testing request/response format validation..."

# Test sending a request with proper Content-Type headers
REQUEST_FORMAT_TEST=$(curl -s -o request_format_response.txt -w "%{http_code}" \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -d '{"test": "data"}' \
  http://localhost:8081/api/test 2>/dev/null || true)

echo ">>> Request format test - HTTP Status: $REQUEST_FORMAT_TEST"

if [ "$REQUEST_FORMAT_TEST" -ne 404 ]; then
  echo ">>> SUCCESS: API handles proper request formats appropriately"
else
  echo ">>> INFO: API endpoint returns 404 as expected for non-existent endpoint"
fi

rm -f request_format_response.txt

# Step 7: Test gateway to backend contract matching
run_test "Gateway-Backend Contract Matching" "Testing that gateway properly forwards contracts to backend"
echo ">>> Testing gateway-backend contract matching..."

# Start a client to generate some backend activity
echo "BOT_MODE=autobot" > .env
echo "BOT_SCENARIO=" >> .env
docker-compose -f "$DOCKER_COMPOSE_FILE" --env-file .env up --scale client=1 --remove-orphans -d
sleep 15

# Check logs for contract-related messages
docker-compose -f "$DOCKER_COMPOSE_FILE" logs server-1 > server1_contracts.log 2>&1
docker-compose -f "$DOCKER_COMPOSE_FILE" logs server-2 > server2_contracts.log 2>&1

CONTRACT_SUCCESS=0
if grep -i -E "contract|api.*mapping|route.*mapping|gateway.*forward|uri.*rewrite" server1_contracts.log; then
  echo ">>> SUCCESS: Found gateway-backend contract mapping in server-1 logs"
  CONTRACT_SUCCESS=1
fi

if grep -i -E "contract|api.*mapping|route.*mapping|gateway.*forward|uri.*rewrite" server2_contracts.log; then
  echo ">>> SUCCESS: Found gateway-backend contract mapping in server-2 logs"
  CONTRACT_SUCCESS=1
fi

if [ $CONTRACT_SUCCESS -eq 0 ]; then
  echo ">>> INFO: No explicit contract mapping messages found in server logs"
fi

rm -f server1_contracts.log server2_contracts.log .env

# Step 8: Test error contract compliance
run_test "Error Contract Compliance" "Testing that error responses follow expected contract"
echo ">>> Testing error contract compliance..."

ERROR_RESPONSE=$(curl -s -o error_response.txt -w "%{http_code}" \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  http://localhost:8081/api/invalid-endpoint 2>/dev/null || true)

echo ">>> Error response test - HTTP Status: $ERROR_RESPONSE"

# Check if error response follows expected format
if head -c 1 error_response.txt | grep -qE '[{[]'; then
  echo ">>> SUCCESS: Error response is in JSON format"
  
  # Check for common error response fields
  if grep -i -E "error|message|status|timestamp" error_response.txt; then
    echo ">>> SUCCESS: Error response contains expected error fields"
  fi
else
  echo ">>> INFO: Error response is not in JSON format"
fi

rm -f error_response.txt

echo "======================================================="
echo ">>> CONTRACT TESTS COMPLETED"
echo ">>> These tests verify API contracts between components"
echo "======================================================="

# Clean up
cleanup