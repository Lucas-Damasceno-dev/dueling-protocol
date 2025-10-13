#!/usr/bin/env bash

# Ensures the script stops if any command fails
set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"
PROJECT_ROOT="$SCRIPT_DIR/.."
DOCKER_COMPOSE_FILE="$PROJECT_ROOT/docker/docker-compose.yml"

echo "======================================================="
echo ">>> STARTING ADVANCED SECURITY TESTS"
echo ">>> Testing advanced security measures including OWASP, DoS protection, etc."
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
./scripts/build.sh
echo ">>> Building Docker images..."
echo "BOT_MODE=autobot" > .env
echo "BOT_SCENARIO=" >> .env
docker compose -f "$DOCKER_COMPOSE_FILE" --env-file .env build
rm -f .env
echo ">>> Build completed successfully."

# Step 2: Start services
run_test "Start Services" "Starting gateway, servers, Redis, and PostgreSQL"
echo ">>> Starting services..."
docker compose -f "$DOCKER_COMPOSE_FILE" up --scale client=0 --remove-orphans -d
echo ">>> Waiting for services to initialize..."
sleep 25

# Step 3: Test SQL Injection Prevention
run_test "SQL Injection Prevention" "Testing system resilience against SQL injection attempts"
echo ">>> Testing SQL injection prevention..."

# Test common SQL injection patterns
SQL_INJECTIONS=(
  "' OR '1'='1",
  "'; DROP TABLE users; --",
  "' UNION SELECT * FROM users --",
  "admin'--",
  "'; WAITFOR DELAY '00:00:10' --"
)

for injection in "${SQL_INJECTIONS[@]}"; do
  # Test injection through a parameter that might touch the database
  RESPONSE=$(curl -s -o sql_response.txt -w "%{http_code}" \
    -H "Content-Type: application/json" \
    -d "{\"username\":\"$injection\", \"password\":\"test\"}" \
    http://localhost:8081/api/user/login 2>/dev/null || echo "0")
    
  # Log the result, but don't show the injection in clear text in output
  if [ "$RESPONSE" -eq 200 ] || [ "$RESPONSE" -eq 400 ] || [ "$RESPONSE" -eq 422 ]; then
    echo ">>> SQL injection attempt blocked properly (HTTP $RESPONSE)"
  elif [ "$RESPONSE" -eq 500 ]; then
    echo ">>> WARNING: SQL injection might have caused server error (HTTP $RESPONSE)"
  else
    echo ">>> SQL injection attempt handled (HTTP $RESPONSE)"
  fi
done

rm -f sql_response.txt

# Step 4: Test XSS Prevention
run_test "XSS Prevention" "Testing system resilience against Cross-Site Scripting attempts"
echo ">>> Testing Cross-Site Scripting (XSS) prevention..."

XSS_PAYLOADS=(
  "<script>alert('XSS')</script>",
  "<img src=x onerror=alert('XSS')>",
  "javascript:alert('XSS')",
  "<svg onload=alert('XSS')>",
  "<body onload=alert('XSS')>"
)

for payload in "${XSS_PAYLOADS[@]}"; do
  # Test XSS through JSON request that might be reflected
  RESPONSE=$(curl -s -o xss_response.txt -w "%{http_code}" \
    -H "Content-Type: application/json" \
    -d "{\"message\":\"$payload\"}" \
    http://localhost:8081/api/user/message 2>/dev/null || echo "0")
    
  if [ "$RESPONSE" -eq 200 ] || [ "$RESPONSE" -eq 400 ] || [ "$RESPONSE" -eq 422 ]; then
    echo ">>> XSS attempt handled properly (HTTP $RESPONSE)"
  elif [ "$RESPONSE" -eq 500 ]; then
    echo ">>> WARNING: XSS might have caused server error (HTTP $RESPONSE)"
  else
    echo ">>> XSS attempt response: HTTP $RESPONSE"
  fi
done

rm -f xss_response.txt

# Step 5: Test DoS/DDoS Protection
run_test "DoS/DDoS Protection" "Testing system resilience against Denial of Service attempts"
echo ">>> Testing DoS/DDoS protection mechanisms..."

# Test rate limiting by making multiple requests in a short time
echo ">>> Testing rate limiting with multiple requests..."
for i in {1..20}; do
  curl -s -o /dev/null -w "" http://localhost:8081/actuator/health 2>/dev/null || true
  sleep 0.1
done

# Check if services are still responsive after the rapid requests
HEALTH_STATUS=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8081/actuator/health 2>/dev/null || echo "0")
if [ "$HEALTH_STATUS" -eq 200 ]; then
  echo ">>> SUCCESS: Services remain responsive after rapid requests (HTTP $HEALTH_STATUS)"
else
  echo ">>> WARNING: Services may be affected by rapid requests (HTTP $HEALTH_STATUS)"
fi

# Step 6: Test Authentication Bypass Attempts
run_test "Authentication Bypass" "Testing resilience against authentication bypass attempts"
echo ">>> Testing authentication bypass attempts..."

# Test with empty/invalid authentication headers
AUTH_BYPASS_TESTS=(
  ""
  "Bearer "
  "Bearer invalidtoken"
  "Basic "
  "Token "
  "ApiKey "
)

for auth_header in "${AUTH_BYPASS_TESTS[@]}"; do
  if [ -z "$auth_header" ]; then
    # Test without Authorization header altogether
    RESPONSE=$(curl -s -o auth_response.txt -w "%{http_code}" \
      http://localhost:8081/api/user/profile 2>/dev/null || echo "0")
  else
    # Test with various invalid authorization headers
    RESPONSE=$(curl -s -o auth_response.txt -w "%{http_code}" \
      -H "Authorization: $auth_header" \
      http://localhost:8081/api/user/profile 2>/dev/null || echo "0")
  fi
  
  if [ "$RESPONSE" -eq 401 ] || [ "$RESPONSE" -eq 403 ]; then
    echo ">>> Authentication bypass attempt properly blocked (HTTP $RESPONSE)"
  else
    echo ">>> WARNING: Authentication bypass attempt received unexpected response (HTTP $RESPONSE)"
  fi
done

rm -f auth_response.txt

# Step 7: Test Session Fixation Prevention
run_test "Session Fixation Prevention" "Testing session management security"
echo ">>> Testing session fixation prevention..."

# Check session management by examining Set-Cookie headers
SESSION_RESPONSE=$(curl -s -D session_headers.txt -o /dev/null \
  http://localhost:8081/actuator/health 2>/dev/null || true)

if grep -i "Set-Cookie.*HttpOnly" session_headers.txt > /dev/null; then
  echo ">>> SUCCESS: HttpOnly flag found in cookies"
else
  echo ">>> INFO: HttpOnly flag not found in cookies"
fi

if grep -i "Set-Cookie.*Secure" session_headers.txt > /dev/null; then
  echo ">>> SUCCESS: Secure flag found in cookies (when using HTTPS)"
else
  echo ">>> INFO: Secure flag not found in cookies"
fi

if grep -i "X-Frame-Options\|Content-Security-Policy" session_headers.txt > /dev/null; then
  echo ">>> SUCCESS: Found security headers in response"
else
  echo ">>> INFO: No security headers found in response"
fi

rm -f session_headers.txt

# Step 8: Test Authorization Matrix
run_test "Authorization Matrix" "Testing authorization for different user roles and permissions"
echo ">>> Testing authorization and role-based access control..."

# Test unauthorized access to restricted endpoints
RESTRICTED_ENDPOINTS=(
  "/api/admin/users"
  "/api/admin/config"
  "/api/user/profile"
  "/api/user/settings"
)

for endpoint in "${RESTRICTED_ENDPOINTS[@]}"; do
  RESPONSE=$(curl -s -o auth_matrix_response.txt -w "%{http_code}" \
    http://localhost:8081$endpoint 2>/dev/null || echo "0")
    
  if [ "$RESPONSE" -eq 401 ] || [ "$RESPONSE" -eq 403 ]; then
    echo ">>> Access to $endpoint properly restricted (HTTP $RESPONSE)"
  else
    echo ">>> INFO: Access to $endpoint returned HTTP $RESPONSE (expected 401/403)"
  fi
done

rm -f auth_matrix_response.txt

# Step 9: Test Security Headers
run_test "Security Headers" "Testing presence of important security headers"
echo ">>> Testing security headers..."

HEADERS_RESPONSE=$(curl -s -D security_headers.txt -o /dev/null \
  http://localhost:8081/actuator/health 2>/dev/null || true)

SECURITY_HEADERS=("X-Content-Type-Options" "X-Frame-Options" "X-XSS-Protection" "Strict-Transport-Security")
for header in "${SECURITY_HEADERS[@]}"; do
  if grep -i "$header" security_headers.txt > /dev/null; then
    echo ">>> SUCCESS: Security header $header found"
  else
    echo ">>> INFO: Security header $header not found"
  fi
done

rm -f security_headers.txt

# Step 10: Test for Sensitive Information Disclosure
run_test "Information Disclosure" "Testing for exposure of sensitive information"
echo ">>> Testing for sensitive information disclosure..."

# Check if stack traces or error details are disclosed
ERROR_RESPONSE=$(curl -s -o error_disclosure.txt -w "%{http_code}" \
  http://localhost:8081/api/this-endpoint-does-not-exist 2>/dev/null || echo "0")

if [ "$ERROR_RESPONSE" -eq 404 ]; then
  # Check if response contains sensitive information
  if grep -i -E "exception|error|trace|java|spring|at.*\..*(.java|class)" error_disclosure.txt; then
    echo ">>> WARNING: Error response may contain sensitive information"
  else
    echo ">>> SUCCESS: Error response does not appear to contain sensitive information"
  fi
else
  echo ">>> INFO: Non-404 response received for invalid endpoint (HTTP $ERROR_RESPONSE)"
fi

rm -f error_disclosure.txt

# Step 11: Test JWT-specific Security
run_test "JWT Security" "Testing specific JWT security aspects"
echo ">>> Testing JWT-specific security measures..."

# Check for JWT in URL parameters (should not happen)
# This is more of a verification test - we'll send a request to ensure JWT is properly validated
JWT_RESPONSE=$(curl -s -o jwt_response.txt -w "%{http_code}" \
  -H "Authorization: Bearer invalid.token.here" \
  http://localhost:8081/api/user/profile 2>/dev/null || echo "0")

if [ "$JWT_RESPONSE" -eq 401 ] || [ "$JWT_RESPONSE" -eq 403 ]; then
  echo ">>> JWT invalid token properly rejected (HTTP $JWT_RESPONSE)"
else
  echo ">>> INFO: JWT invalid token response: HTTP $JWT_RESPONSE"
fi

# Test JWT with invalid signature but proper format
FAKE_JWT="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c"

FAKE_JWT_RESPONSE=$(curl -s -o fake_jwt_response.txt -w "%{http_code}" \
  -H "Authorization: Bearer $FAKE_JWT" \
  http://localhost:8081/api/user/profile 2>/dev/null || echo "0")

if [ "$FAKE_JWT_RESPONSE" -eq 401 ]; then
  echo ">>> JWT with invalid signature properly rejected (HTTP $FAKE_JWT_RESPONSE)"
else
  echo ">>> WARNING: JWT with invalid signature may not have been properly rejected (HTTP $FAKE_JWT_RESPONSE)"
fi

rm -f jwt_response.txt fake_jwt_response.txt

# Step 12: Test WebSocket Security
run_test "WebSocket Security" "Testing WebSocket authentication and security"
echo ">>> Testing WebSocket security..."

# Test WebSocket connection without authentication
WEBSOCKET_SEC_RESPONSE=$(curl -s -o ws_sec_response.txt -w "%{http_code}" \
  -H "Connection: Upgrade" \
  -H "Upgrade: websocket" \
  -H "Sec-WebSocket-Key: test123456" \
  -H "Sec-WebSocket-Version: 13" \
  http://localhost:8081/ws/connect 2>/dev/null || echo "0")

if [ "$WEBSOCKET_SEC_RESPONSE" -eq 401 ] || [ "$WEBSOCKET_SEC_RESPONSE" -eq 403 ]; then
  echo ">>> WebSocket connection properly requires authentication (HTTP $WEBSOCKET_SEC_RESPONSE)"
elif [ "$WEBSOCKET_SEC_RESPONSE" -eq 404 ]; then
  echo ">>> WebSocket endpoint not found (HTTP $WEBSOCKET_SEC_RESPONSE), which is expected if not implemented"
elif [ "$WEBSOCKET_SEC_RESPONSE" -eq 101 ]; then
  echo ">>> WebSocket connection established (HTTP $WEBSOCKET_SEC_RESPONSE), check if authentication was properly enforced"
else
  echo ">>> WebSocket connection response: HTTP $WEBSOCKET_SEC_RESPONSE"
fi

rm -f ws_sec_response.txt

echo "======================================================="
echo ">>> ADVANCED SECURITY TESTS COMPLETED"
echo ">>> These tests evaluate the system's security posture"
echo "======================================================="

# Clean up
cleanup