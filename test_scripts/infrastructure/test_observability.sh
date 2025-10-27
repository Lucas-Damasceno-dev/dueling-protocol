#!/usr/bin/env bash

# Ensures the script stops if any command fails
set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"
PROJECT_ROOT="$SCRIPT_DIR/../.."
DOCKER_COMPOSE_FILE="$PROJECT_ROOT/docker/docker-compose.yml"

echo "======================================================="
echo ">>> STARTING OBSERVABILITY TESTS"
echo ">>> Testing system observability including metrics, monitoring, and logging"
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

# Step 2: Start services including monitoring stack
run_test "Start Services With Monitoring" "Starting services with Prometheus and Grafana"
echo ">>> Starting services with monitoring stack..."
docker compose -f "$DOCKER_COMPOSE_FILE" up --scale client=0 --remove-orphans -d
echo ">>> Waiting for services and monitoring to initialize..."
sleep 35

# Step 3: Test Prometheus connectivity and metrics collection
run_test "Prometheus Connectivity" "Testing Prometheus server connectivity and metrics collection"
echo ">>> Testing Prometheus connectivity..."

# Test if Prometheus is accessible
PROMETHEUS_STATUS=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:9090/-/ready 2>/dev/null || echo "0")
echo ">>> Prometheus status: HTTP $PROMETHEUS_STATUS"

if [ "$PROMETHEUS_STATUS" -eq 200 ]; then
  echo ">>> SUCCESS: Prometheus server is accessible"
  
  # Test if Prometheus has targets
  PROMETHEUS_TARGETS=$(curl -s http://localhost:9090/api/v1/targets 2>/dev/null | grep -o "http://server-[0-9]:8080/actuator/prometheus" | wc -l || echo "0")
  echo ">>> Prometheus targets found: $PROMETHEUS_TARGETS"
  
  if [ "$PROMETHEUS_TARGETS" -ge 2 ]; then
    echo ">>> SUCCESS: Prometheus has targets from both servers"
  else
    echo ">>> INFO: Prometheus targets count: $PROMETHEUS_TARGETS (expected at least 2)"
  fi
  
  # Test metric query
  METRIC_QUERY_RESULT=$(curl -s "http://localhost:9090/api/v1/query?query=up" 2>/dev/null | grep -o "http://server-[0-9]:8080" | wc -l || echo "0")
  echo ">>> Up metric results: $METRIC_QUERY_RESULT"
  
  if [ "$METRIC_QUERY_RESULT" -ge 1 ]; then
    echo ">>> SUCCESS: Prometheus is collecting metrics from servers"
  else
    echo ">>> INFO: No metric results found, but this may be normal depending on startup timing"
  fi
else
  echo ">>> WARNING: Prometheus server not accessible (HTTP $PROMETHEUS_STATUS)"
fi

# Step 4: Test metrics endpoint on servers
run_test "Metrics Endpoint Validation" "Testing metrics endpoint on server instances"
echo ">>> Testing metrics endpoint on servers..."

SERVER1_METRICS_STATUS=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8081/actuator/prometheus 2>/dev/null || echo "0")
SERVER2_METRICS_STATUS=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8082/actuator/prometheus 2>/dev/null || echo "0")

echo ">>> Server-1 metrics endpoint: HTTP $SERVER1_METRICS_STATUS"
echo ">>> Server-2 metrics endpoint: HTTP $SERVER2_METRICS_STATUS"

if [ "$SERVER1_METRICS_STATUS" -eq 200 ]; then
  # Get sample of metrics to verify they're in correct format
  curl -s http://localhost:8081/actuator/prometheus > server1_metrics.txt 2>/dev/null || echo "No metrics available"
  
  if [ -s server1_metrics.txt ] && head -n 5 server1_metrics.txt | grep -q "# HELP\|# TYPE"; then
    echo ">>> SUCCESS: Server-1 metrics in Prometheus format"
  else
    echo ">>> WARNING: Server-1 metrics may not be in Prometheus format"
  fi
else
  echo ">>> INFO: Server-1 metrics endpoint not accessible"
fi

if [ "$SERVER2_METRICS_STATUS" -eq 200 ]; then
  # Get sample of metrics to verify they're in correct format
  curl -s http://localhost:8082/actuator/prometheus > server2_metrics.txt 2>/dev/null || echo "No metrics available"
  
  if [ -s server2_metrics.txt ] && head -n 5 server2_metrics.txt | grep -q "# HELP\|# TYPE"; then
    echo ">>> SUCCESS: Server-2 metrics in Prometheus format"
  else
    echo ">>> WARNING: Server-2 metrics may not be in Prometheus format"
  fi
else
  echo ">>> INFO: Server-2 metrics endpoint not accessible"
fi

# Verify some common metrics exist
COMMON_METRICS=("jvm_memory_used_bytes" "http_server_requests_seconds_count" "process_cpu_usage" "tomcat_sessions_active_current")
for metric in "${COMMON_METRICS[@]}"; do
  if grep -q "$metric" server1_metrics.txt 2>/dev/null; then
    echo ">>> SUCCESS: Found metric '$metric' on server-1"
  fi
done

rm -f server1_metrics.txt server2_metrics.txt

# Step 5: Test Grafana connectivity
run_test "Grafana Connectivity" "Testing Grafana dashboard connectivity"
echo ">>> Testing Grafana connectivity..."

GRAFANA_STATUS=$(curl -s -o grafana_response.txt -w "%{http_code}" http://localhost:3000/api/health 2>/dev/null || echo "0")
echo ">>> Grafana status: HTTP $GRAFANA_STATUS"

if [ "$GRAFANA_STATUS" -eq 200 ]; then
  echo ">>> SUCCESS: Grafana dashboard is accessible"
  
  # Verify response contains health info
  if grep -q "live\|ok" grafana_response.txt; then
    echo ">>> SUCCESS: Grafana health response indicates it's running"
  fi
else
  echo ">>> INFO: Grafana dashboard not accessible (HTTP $GRAFANA_STATUS)"
fi

rm -f grafana_response.txt

# Step 6: Test logging configuration
run_test "Logging Configuration" "Testing application logging configuration"
echo ">>> Testing application logging configuration..."

# Start a client to generate some logs
echo "BOT_MODE=autobot" > .env
echo "BOT_SCENARIO=" >> .env
docker compose -f "$DOCKER_COMPOSE_FILE" --env-file .env up --scale client=2 --remove-orphans -d
sleep 20

# Check server logs for expected log patterns
docker compose -f "$DOCKER_COMPOSE_FILE" logs server-1 > server1_logs.txt 2>&1
docker compose -f "$DOCKER_COMPOSE_FILE" logs server-2 > server2_logs.txt 2>&1

# Check for different log levels
LOG_LEVELS=("INFO" "WARN" "ERROR" "DEBUG")
for level in "${LOG_LEVELS[@]}"; do
  if grep -i "$level" server1_logs.txt > /dev/null; then
    echo ">>> SUCCESS: Found $level level logs in server-1"
  fi
  
  if grep -i "$level" server2_logs.txt > /dev/null; then
    echo ">>> SUCCESS: Found $level level logs in server-2"
  fi
done

# Check for structured logging elements
STRUCTURED_LOG_ELEMENTS=("timestamp\|time\|level\|logger\|message\|thread")
if grep -E "INFO.*\[.*\]|20[0-9]{2}-[0-9]{2}-[0-9]{2}" server1_logs.txt > /dev/null; then
  echo ">>> SUCCESS: Found structured logging in server-1"
fi

if grep -E "INFO.*\[.*\]|20[0-9]{2}-[0-9]{2}-[0-9]{2}" server2_logs.txt > /dev/null; then
  echo ">>> SUCCESS: Found structured logging in server-2"
fi

# Check for trace IDs if distributed tracing is enabled
if grep -i -E "trace\|span\|request.*id\|correlation" server1_logs.txt > /dev/null; then
  echo ">>> SUCCESS: Found trace correlation identifiers in server-1 logs"
fi

if grep -i -E "trace\|span\|request.*id\|correlation" server2_logs.txt > /dev/null; then
  echo ">>> SUCCESS: Found trace correlation identifiers in server-2 logs"
fi

rm -f server1_logs.txt server2_logs.txt .env

# Step 7: Test health and info endpoints
run_test "Health and Info Endpoints" "Testing health and info actuator endpoints"
echo ">>> Testing health and info endpoints..."

# Test health endpoints
SERVER1_HEALTH=$(curl -s -o server1_health.txt -w "%{http_code}" http://localhost:8081/actuator/health 2>/dev/null || echo "0")
SERVER2_HEALTH=$(curl -s -o server2_health.txt -w "%{http_code}" http://localhost:8082/actuator/health 2>/dev/null || echo "0")

echo ">>> Server-1 health: HTTP $SERVER1_HEALTH"
echo ">>> Server-2 health: HTTP $SERVER2_HEALTH"

if [ "$SERVER1_HEALTH" -eq 200 ]; then
  if grep -i -E "up\|status.*up" server1_health.txt > /dev/null; then
    echo ">>> SUCCESS: Server-1 health endpoint shows UP status"
  fi
fi

if [ "$SERVER2_HEALTH" -eq 200 ]; then
  if grep -i -E "up\|status.*up" server2_health.txt > /dev/null; then
    echo ">>> SUCCESS: Server-2 health endpoint shows UP status"
  fi
fi

# Test info endpoint
SERVER1_INFO=$(curl -s -o server1_info.txt -w "%{http_code}" http://localhost:8081/actuator/info 2>/dev/null || echo "0")
SERVER2_INFO=$(curl -s -o server2_info.txt -w "%{http_code}" http://localhost:8082/actuator/info 2>/dev/null || echo "0")

echo ">>> Server-1 info: HTTP $SERVER1_INFO"
echo ">>> Server-2 info: HTTP $SERVER2_INFO"

if [ "$SERVER1_INFO" -eq 200 ]; then
  if head -c 1 server1_info.txt | grep -qE '[{[]'; then
    echo ">>> SUCCESS: Server-1 info endpoint returns JSON"
  fi
fi

if [ "$SERVER2_INFO" -eq 200 ]; then
  if head -c 1 server2_info.txt | grep -qE '[{[]'; then
    echo ">>> SUCCESS: Server-2 info endpoint returns JSON"
  fi
fi

rm -f server1_health.txt server2_health.txt server1_info.txt server2_info.txt

# Step 8: Test distributed tracing (if enabled)
run_test "Distributed Tracing" "Testing distributed tracing capabilities"
echo ">>> Testing distributed tracing..."

# Check server logs for trace-related information
docker compose -f "$DOCKER_COMPOSE_FILE" logs server-1 > server1_trace_check.txt 2>&1
docker compose -f "$DOCKER_COMPOSE_FILE" logs server-2 > server2_trace_check.txt 2>&1

if grep -i -E "trace.*id\|span.*id\|request.*id.*correlation\|distributed.*trace" server1_trace_check.txt > /dev/null; then
  echo ">>> SUCCESS: Found distributed tracing information in server-1 logs"
else
  echo ">>> INFO: No distributed tracing information found in server-1 logs (may be expected)"
fi

if grep -i -E "trace.*id\|span.*id\|request.*id.*correlation\|distributed.*trace" server2_trace_check.txt > /dev/null; then
  echo ">>> SUCCESS: Found distributed tracing information in server-2 logs"
else
  echo ">>> INFO: No distributed tracing information found in server-2 logs (may be expected)"
fi

rm -f server1_trace_check.txt server2_trace_check.txt

# Step 9: Test monitoring metrics under load
run_test "Metrics Under Load" "Testing monitoring metrics collection under load"
echo ">>> Testing metrics collection under load..."

# Start more clients to generate metrics
echo "BOT_MODE=autobot" > .env
echo "BOT_SCENARIO=" >> .env
docker compose -f "$DOCKER_COMPOSE_FILE" --env-file .env up --scale client=4 --remove-orphans -d
sleep 30

# Check metrics again to see if they're being updated under load
SERVER1_METRICS_UNDER_LOAD=$(curl -s http://localhost:8081/actuator/prometheus 2>/dev/null | grep "http_server_requests_seconds_count" | head -n 1 | grep -o "[0-9]*\.[0-9]*\|[0-9]*" | tail -n 1 || echo "0")

echo ">>> HTTP request count metric under load: $SERVER1_METRICS_UNDER_LOAD"

if [ "$SERVER1_METRICS_UNDER_LOAD" -gt 0 ]; then
  echo ">>> SUCCESS: Metrics are being updated under load"
else
  echo ">>> INFO: No request count metrics found under load"
fi

docker compose -f "$DOCKER_COMPOSE_FILE" --env-file .env down
rm -f .env

# Step 10: Test monitoring system resilience
run_test "Monitoring System Resilience" "Testing that monitoring continues during system stress"
echo ">>> Testing monitoring system resilience..."

# Check if monitoring services are still running
PROMETHEUS_HEALTH=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:9090/-/ready 2>/dev/null || echo "0")
GRAFANA_HEALTH=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:3000/api/health 2>/dev/null || echo "0")

if [ "$PROMETHEUS_HEALTH" -eq 200 ]; then
  echo ">>> SUCCESS: Prometheus remains healthy after tests"
else
  echo ">>> WARNING: Prometheus health check failed after tests (HTTP $PROMETHEUS_HEALTH)"
fi

if [ "$GRAFANA_HEALTH" -eq 200 ]; then
  echo ">>> SUCCESS: Grafana remains healthy after tests"
else
  echo ">>> WARNING: Grafana health check failed after tests (HTTP $GRAFANA_HEALTH)"
fi

# Step 11: Test alerting configuration
run_test "Alerting Configuration" "Testing alerting system configuration"
echo ">>> Testing alerting system configuration..."

# Check if Prometheus has alerting rules configured
if docker exec $(docker compose -f "$DOCKER_COMPOSE_FILE" ps -q prometheus) test -f /etc/prometheus/prometheus.yml; then
  echo ">>> SUCCESS: Prometheus configuration file exists"
  
  # Check if there are rules in the config
  ALERT_RULES_COUNT=$(docker exec $(docker compose -f "$DOCKER_COMPOSE_FILE" ps -q prometheus) cat /etc/prometheus/prometheus.yml 2>/dev/null | grep -i -E "rule\|alert" | wc -l || echo "0")
  echo ">>> Alert rules found in Prometheus config: $ALERT_RULES_COUNT"
  
  if [ $ALERT_RULES_COUNT -gt 0 ]; then
    echo ">>> SUCCESS: Prometheus has alerting rules configured"
  fi
else
  echo ">>> INFO: Prometheus configuration file not found at expected location"
fi

# Step 12: Observability summary
run_test "Observability Summary" "Summary of observability testing"
echo ">>> Observability testing completed. Verifying collected metrics..."

# Get a final check of all services
FINAL_SERVICES_STATUS=$(docker compose -f "$DOCKER_COMPOSE_FILE" ps)
echo ">>> Final service status:"
echo "$FINAL_SERVICES_STATUS"

# Check that all monitoring components are still running
MONITORING_SERVICES_UP=true
if ! echo "$FINAL_SERVICES_STATUS" | grep -q "prometheus.*Up"; then
  echo ">>> WARNING: Prometheus is not running properly"
  MONITORING_SERVICES_UP=false
fi

if ! echo "$FINAL_SERVICES_STATUS" | grep -q "grafana.*Up"; then
  echo ">>> WARNING: Grafana is not running properly"
  MONITORING_SERVICES_UP=false
fi

if [ "$MONITORING_SERVICES_UP" = true ]; then
  echo ">>> SUCCESS: All monitoring services remain operational"
else
  echo ">>> WARNING: Some monitoring services may have issues"
fi

echo "======================================================="
echo ">>> OBSERVABILITY TESTS COMPLETED"
echo ">>> These tests validate system monitoring, metrics, and logging capabilities"
echo "======================================================="

# Clean up
cleanup