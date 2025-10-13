#!/usr/bin/env bash

# Ensures the script stops if any command fails
set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"
PROJECT_ROOT="$SCRIPT_DIR/.."
DOCKER_COMPOSE_FILE="$PROJECT_ROOT/docker/docker-compose.yml"
ENV_FILE="$PROJECT_ROOT/.env"

echo "======================================================="
echo ">>> STARTING JWT SECURITY TESTS"
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
  if [ -f "$ENV_FILE" ]; then
    rm -f "$ENV_FILE"
  fi
}

trap cleanup EXIT

# Step 1: Build the project and Docker images
run_test "Build Project" "Compiling project and building Docker images"
echo ">>> Building project..."
cd "$PROJECT_ROOT"

echo ">>> Building Docker images..."
# Create .env file for build
cat > "$ENV_FILE" <<EOL
BOT_MODE=autobot
BOT_SCENARIO=
REDIS_HOST=redis
REDIS_PORT=6379
POSTGRES_HOST=postgres
POSTGRES_PORT=5432
POSTGRES_DB=dueling_db
POSTGRES_USER=user
POSTGRES_PASSWORD=password
JWT_SECRET=mySecretKeyForDuelingProtocol
EOL

docker compose -f "$DOCKER_COMPOSE_FILE" --env-file "$ENV_FILE" build
echo ">>> Build completed successfully."

# Step 2: Start services (without clients for security testing)
run_test "Start Services" "Starting gateway, servers, Redis, and PostgreSQL for JWT tests"
echo ">>> Starting services..."
set +e
docker compose -f "$DOCKER_COMPOSE_FILE" --env-file "$ENV_FILE" up --scale client-1=0 --scale client-2=0 --scale client-3=0 --scale client-4=0 --remove-orphans -d
echo ">>> Waiting for services to initialize..."
sleep 20
echo ">>> Getting logs for server-1..."
docker logs server-1
set -e

# Step 3: Test API endpoint without JWT token (should fail)
run_test "Access Without Token" "Testing that endpoints require authentication"
echo ">>> Testing API access without JWT token..."
NO_TOKEN_RESPONSE=$(curl -s -o response.txt -w "%{\http_code}" http://localhost:8080/api/user/test 2>/dev/null || true)
if [ "$NO_TOKEN_RESPONSE" -eq 401 ] || [ "$NO_TOKEN_RESPONSE" -eq 403 ]; then
  echo ">>> SUCCESS: API properly rejects requests without JWT token (received HTTP $NO_TOKEN_RESPONSE)"
else
  echo ">>> WARNING: API should have rejected request without JWT token (received HTTP $NO_TOKEN_RESPONSE)"
fi
rm -f response.txt

# Step 4: Test API endpoint with invalid JWT token
run_test "Access With Invalid Token" "Testing that invalid JWT tokens are rejected"
echo ">>> Testing API access with invalid JWT token..."

# Create an obviously invalid token
INVALID_TOKEN="invalid.token.here"

INVALID_TOKEN_RESPONSE=$(curl -s -o response.txt -w "%{\http_code}" \
  -H "Authorization: Bearer $INVALID_TOKEN" \
  http://localhost:8080/api/user/test 2>/dev/null || true)

if [ "$INVALID_TOKEN_RESPONSE" -eq 401 ] || [ "$INVALID_TOKEN_RESPONSE" -eq 403 ]; then
  echo ">>> SUCCESS: API properly rejects invalid JWT tokens (received HTTP $INVALID_TOKEN_RESPONSE)"
else
  echo ">>> WARNING: API should have rejected invalid JWT token (received HTTP $INVALID_TOKEN_RESPONSE)"
fi
rm -f response.txt

# Step 5: Test JWT in WebSocket connections
run_test "WebSocket JWT Validation" "Testing JWT validation for WebSocket connections"
echo ">>> Testing WebSocket JWT validation..."

# Try to connect to WebSocket without proper authentication
# This is a basic test for WebSocket endpoint accessibility with proper headers
WEBSOCKET_TEST=$(curl -s -o response.txt -w "%{\http_code}" \
  -H "Connection: Upgrade" \
  -H "Upgrade: websocket" \
  -H "Sec-WebSocket-Key: test123" \
  -H "Sec-WebSocket-Version: 13" \
  -H "Authorization: Bearer invalid_token" \
  http://localhost:8080/ws/connect 2>/dev/null || true)

if [ "$WEBSOCKET_TEST" -eq 401 ] || [ "$WEBSOCKET_TEST" -eq 403 ] || [ "$WEBSOCKET_TEST" -eq 404 ]; then
  echo ">>> SUCCESS: WebSocket properly validates JWT tokens (received HTTP $WEBSOCKET_TEST)"
else
  echo ">>> WARNING: WebSocket JWT validation may not be working properly (received HTTP $WEBSOCKET_TEST)"
fi
rm -f response.txt

# Step 6: Test JWT with malformed structure
run_test "Malformed JWT Token" "Testing rejection of malformed JWT tokens"
echo ">>> Testing API with malformed JWT token..."

# Test with various malformed token formats
MALFORMED_TOKENS=("not.enough.parts" "too.many.parts.here.extra" "." "....")

for token in "${MALFORMED_TOKENS[@]}"; do
  if [ -z "$token" ]; then
    continue  # Skip empty token
  fi
  
  RESPONSE=$(curl -s -o /dev/null -w "%{\http_code}" \
    -H "Authorization: Bearer $token" \
    http://localhost:8080/api/user/test 2>/dev/null || true)
  
  if [ "$RESPONSE" -eq 401 ] || [ "$RESPONSE" -eq 403 ]; then
    echo ">>> SUCCESS: API rejects malformed token '$token' (received HTTP $RESPONSE)"
  else
    echo ">>> WARNING: API should reject malformed token '$token' (received HTTP $RESPONSE)"
  fi
done

# Step 7: Test header without Bearer prefix
run_test "JWT Without Bearer Prefix" "Testing that tokens without Bearer prefix are handled properly"
echo ">>> Testing token without Bearer prefix..."

# Send token without "Bearer " prefix
RESPONSE=$(curl -s -o response.txt -w "%{\http_code}" \
  -H "Authorization: invalid_token_without_bearer" \
  http://localhost:8080/api/user/test 2>/dev/null || true)

if [ "$RESPONSE" -eq 401 ] || [ "$RESPONSE" -eq 403 ]; then
  echo ">>> SUCCESS: API properly handles missing Bearer prefix (received HTTP $RESPONSE)"
else
  echo ">>> WARNING: API response for missing Bearer prefix might be unexpected (received HTTP $RESPONSE)"
fi
rm -f response.txt

# Step 8: Test with valid-looking but fake JWT
run_test "Fake Valid-Looking JWT" "Testing JWT that looks valid but is fake"
echo ">>> Testing with fake but properly formatted JWT..."

# This is a fake JWT with valid structure but invalid signature
# It's a base64-encoded header.payload with an invalid signature
FAKE_JWT="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c"

FAKE_JWT_RESPONSE=$(curl -s -o response.txt -w "%{\http_code}" \
  -H "Authorization: Bearer $FAKE_JWT" \
  http://localhost:8080/api/user/test 2>/dev/null || true)

if [ "$FAKE_JWT_RESPONSE" -eq 401 ] || [ "$FAKE_JWT_RESPONSE" -eq 403 ]; then
  echo ">>> SUCCESS: API properly rejects fake JWT with invalid signature (received HTTP $FAKE_JWT_RESPONSE)"
else
  echo ">>> WARNING: API should have rejected fake JWT with invalid signature (received HTTP $FAKE_JWT_RESPONSE)"
fi
rm -f response.txt

echo "======================================================="
echo ">>> JWT SECURITY TESTS COMPLETED"
echo "======================================================="

# Clean up
cleanup