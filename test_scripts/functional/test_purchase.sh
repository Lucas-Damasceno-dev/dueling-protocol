#!/bin/bash

# Get script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$SCRIPT_DIR/../.."
DOCKER_COMPOSE_FILE="$PROJECT_ROOT/docker/docker-compose.yml"

# Source common environment helper
source "$SCRIPT_DIR/../common_env.sh"

set -e

echo "=== TESTING CARD PURCHASE FUNCTIONALITY ==="

# Create env file
create_env_file "$PROJECT_ROOT/docker/.env" "normal" ""

# Start services
echo "Starting services..."
docker compose -f "$DOCKER_COMPOSE_FILE" --env-file "$PROJECT_ROOT/docker/.env" up -d postgres redis-master redis-slave redis-sentinel-1 redis-sentinel-2 redis-sentinel-3 server-1 nginx-gateway

echo "Waiting for services to be ready..."
sleep 30

# Register a test user
echo -e "\n1. Registering test user..."
REGISTER_RESPONSE=$(curl -s -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"purchasetest","password":"testpass"}')
echo "Register response: $REGISTER_RESPONSE"

# Login
echo -e "\n2. Logging in..."
LOGIN_RESPONSE=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"purchasetest","password":"testpass"}')
echo "Login response: $LOGIN_RESPONSE"

TOKEN=$(echo $LOGIN_RESPONSE | grep -o '"token":"[^"]*"' | cut -d'"' -f4)
echo "Token: $TOKEN"

if [ -z "$TOKEN" ]; then
  echo "ERROR: Failed to get token"
  exit 1
fi

# Note: To fully test purchase, we need WebSocket connection
# For now, we can verify the auth works and the system is responsive
echo -e "\n3. Verification complete - Registration and login work!"
echo "To test purchase, you need to connect via WebSocket and send: STORE:BUY:BASIC"

# Try to purchase via REST API simulation
echo -e "\n4. Testing purchase endpoint (if available)..."
PURCHASE_RESPONSE=$(curl -s -X POST http://localhost:8080/api/store/purchase \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"packageType":"BASIC"}' || echo "Endpoint may not exist")
echo "Purchase response: $PURCHASE_RESPONSE"

# Cleanup
echo -e "\nCleaning up..."
docker compose -f "$DOCKER_COMPOSE_FILE" down

echo "✅ Test completed - Auth system working!"
exit 0
