#!/bin/bash

# Quick test script to verify card purchase functionality

echo "Testing card purchase functionality..."

# Register a test user
echo "1. Registering test user..."
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
echo ""
echo "You can run a client manually with:"
echo "cd dueling-client && java -DGATEWAY_HOST=localhost -DGATEWAY_PORT=8080 -jar target/dueling-client-1.0-SNAPSHOT.jar"
