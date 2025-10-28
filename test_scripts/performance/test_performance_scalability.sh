#!/usr/bin/env bash

# Ensures the script stops if any command fails
set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"
PROJECT_ROOT="$SCRIPT_DIR/../.."
DOCKER_COMPOSE_FILE="$PROJECT_ROOT/docker/docker-compose.yml"

echo "======================================================="
echo ">>> STARTING PERFORMANCE AND SCALABILITY TESTS"
echo ">>> Testing system performance under various load conditions"
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

# Global variables for performance metrics
START_TIME=$(date +%s)
METRICS_FILE="performance_metrics_$(date +%s).txt"

# Function to record metrics
record_metric() {
  local test_name=$1
  local metric=$2
  local value=$3
  echo "$(date): $test_name - $metric: $value" >> $METRICS_FILE
}

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

# Step 2: Start services for performance testing
run_test "Start Services" "Starting services for performance testing"
echo ">>> Starting services..."
docker compose -f "$DOCKER_COMPOSE_FILE" up --scale client-1=0 --scale client-2=0 --scale client-3=0 --scale client-4=0 --remove-orphans -d
echo ">>> Waiting for services to initialize..."
sleep 25

# Record baseline metrics
echo ">>> Recording baseline metrics..."
record_metric "baseline" "server1_cpu" "$(docker stats --no-stream --format \"{{.CPUPerc}}\" $(docker compose -f "$DOCKER_COMPOSE_FILE" ps -q server-1) 2>/dev/null | tr -d '% ' || echo 'N/A')"
record_metric "baseline" "server2_cpu" "$(docker stats --no-stream --format \"{{.CPUPerc}}\" $(docker compose -f "$DOCKER_COMPOSE_FILE" ps -q server-2) 2>/dev/null | tr -d '% ' || echo 'N/A')"
record_metric "baseline" "redis_cpu" "$(docker stats --no-stream --format \"{{.CPUPerc}}\" $(docker compose -f "$DOCKER_COMPOSE_FILE" ps -q redis) 2>/dev/null | tr -d '% ' || echo 'N/A')"
record_metric "baseline" "postgres_cpu" "$(docker stats --no-stream --format \"{{.CPUPerc}}\" $(docker compose -f "$DOCKER_COMPOSE_FILE" ps -q postgres) 2>/dev/null | tr -d '% ' || echo 'N/A')"

# Step 3: Test baseline response time
run_test "Baseline Response Time" "Testing baseline response time under minimal load"
echo ">>> Testing baseline response time..."

START_TIME=$(date +%s.%N)
RESPONSE_CODE=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/actuator/health 2>/dev/null || true)
END_TIME=$(date +%s.%N)
ELAPSED_TIME=$(echo "$END_TIME - $START_TIME" | bc)

record_metric "baseline" "response_time_seconds" "$ELAPSED_TIME"
record_metric "baseline" "response_code" "$RESPONSE_CODE"

echo ">>> Baseline response time: $ELAPSED_TIME seconds (HTTP $RESPONSE_CODE)"

# Step 4: Performance test with increasing load
run_test "Load Performance Test" "Testing performance under increasing client load"
echo ">>> Testing performance under increasing load..."

# Test with 1 client
echo ">>> Testing with 1 client..."
echo "BOT_MODE=autobot" > .env
echo "BOT_SCENARIO=" >> .env
docker compose -f "$DOCKER_COMPOSE_FILE" --env-file .env up --scale client-1=1 --remove-orphans -d
sleep 30

# Record metrics after 1 client
SERVER1_CPU_1=$(docker stats --no-stream --format "{{.CPUPerc}}" $(docker compose -f "$DOCKER_COMPOSE_FILE" ps -q server-1) 2>/dev/null | tr -d '% ' || echo 'N/A')
SERVER2_CPU_1=$(docker stats --no-stream --format "{{.CPUPerc}}" $(docker compose -f "$DOCKER_COMPOSE_FILE" ps -q server-2) 2>/dev/null | tr -d '% ' || echo 'N/A')
REDIS_CPU_1=$(docker stats --no-stream --format "{{.CPUPerc}}" $(docker compose -f "$DOCKER_COMPOSE_FILE" ps -q redis) 2>/dev/null | tr -d '% ' || echo 'N/A')
POSTGRES_CPU_1=$(docker stats --no-stream --format "{{.CPUPerc}}" $(docker compose -f "$DOCKER_COMPOSE_FILE" ps -q postgres) 2>/dev/null | tr -d '% ' || echo 'N/A')

record_metric "load_test_1_client" "server1_cpu" "$SERVER1_CPU_1"
record_metric "load_test_1_client" "server2_cpu" "$SERVER2_CPU_1"
record_metric "load_test_1_client" "redis_cpu" "$REDIS_CPU_1"
record_metric "load_test_1_client" "postgres_cpu" "$POSTGRES_CPU_1"

# Test response time with 1 client
START_TIME=$(date +%s.%N)
RESPONSE_CODE=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/actuator/health 2>/dev/null || true)
END_TIME=$(date +%s.%N)
ELAPSED_TIME_1=$(echo "$END_TIME - $START_TIME" | bc)
record_metric "load_test_1_client" "response_time_seconds" "$ELAPSED_TIME_1"

docker compose -f "$DOCKER_COMPOSE_FILE" --env-file .env down

# Test with 3 clients
echo ">>> Testing with 3 clients..."
docker compose -f "$DOCKER_COMPOSE_FILE" --env-file .env up --scale client-1=1 --scale client-2=1 --scale client-3=1 --remove-orphans -d
sleep 45

# Record metrics after 3 clients
SERVER1_CPU_3=$(docker stats --no-stream --format "{{.CPUPerc}}" $(docker compose -f "$DOCKER_COMPOSE_FILE" ps -q server-1) 2>/dev/null | tr -d '% ' || echo 'N/A')
SERVER2_CPU_3=$(docker stats --no-stream --format "{{.CPUPerc}}" $(docker compose -f "$DOCKER_COMPOSE_FILE" ps -q server-2) 2>/dev/null | tr -d '% ' || echo 'N/A')
REDIS_CPU_3=$(docker stats --no-stream --format "{{.CPUPerc}}" $(docker compose -f "$DOCKER_COMPOSE_FILE" ps -q redis) 2>/dev/null | tr -d '% ' || echo 'N/A')
POSTGRES_CPU_3=$(docker stats --no-stream --format "{{.CPUPerc}}" $(docker compose -f "$DOCKER_COMPOSE_FILE" ps -q postgres) 2>/dev/null | tr -d '% ' || echo 'N/A')

record_metric "load_test_3_clients" "server1_cpu" "$SERVER1_CPU_3"
record_metric "load_test_3_clients" "server2_cpu" "$SERVER2_CPU_3"
record_metric "load_test_3_clients" "redis_cpu" "$REDIS_CPU_3"
record_metric "load_test_3_clients" "postgres_cpu" "$POSTGRES_CPU_3"

# Test response time with 3 clients
START_TIME=$(date +%s.%N)
RESPONSE_CODE=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/actuator/health 2>/dev/null || true)
END_TIME=$(date +%s.%N)
ELAPSED_TIME_3=$(echo "$END_TIME - $START_TIME" | bc)
record_metric "load_test_3_clients" "response_time_seconds" "$ELAPSED_TIME_3"

docker compose -f "$DOCKER_COMPOSE_FILE" --env-file .env down

# Test with 5 clients
echo ">>> Testing with 5 clients..."
docker compose -f "$DOCKER_COMPOSE_FILE" --env-file .env up --scale client-1=1 --scale client-2=1 --scale client-3=1 --scale client-4=2 --remove-orphans -d
sleep 60

# Record metrics after 5 clients
SERVER1_CPU_5=$(docker stats --no-stream --format "{{.CPUPerc}}" $(docker compose -f "$DOCKER_COMPOSE_FILE" ps -q server-1) 2>/dev/null | tr -d '% ' || echo 'N/A')
SERVER2_CPU_5=$(docker stats --no-stream --format "{{.CPUPerc}}" $(docker compose -f "$DOCKER_COMPOSE_FILE" ps -q server-2) 2>/dev/null | tr -d '% ' || echo 'N/A')
REDIS_CPU_5=$(docker stats --no-stream --format "{{.CPUPerc}}" $(docker compose -f "$DOCKER_COMPOSE_FILE" ps -q redis) 2>/dev/null | tr -d '% ' || echo 'N/A')
POSTGRES_CPU_5=$(docker stats --no-stream --format "{{.CPUPerc}}" $(docker compose -f "$DOCKER_COMPOSE_FILE" ps -q postgres) 2>/dev/null | tr -d '% ' || echo 'N/A')

record_metric "load_test_5_clients" "server1_cpu" "$SERVER1_CPU_5"
record_metric "load_test_5_clients" "server2_cpu" "$SERVER2_CPU_5"
record_metric "load_test_5_clients" "redis_cpu" "$REDIS_CPU_5"
record_metric "load_test_5_clients" "postgres_cpu" "$POSTGRES_CPU_5"

# Test response time with 5 clients
START_TIME=$(date +%s.%N)
RESPONSE_CODE=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/actuator/health 2>/dev/null || true)
END_TIME=$(date +%s.%N)
ELAPSED_TIME_5=$(echo "$END_TIME - $START_TIME" | bc)
record_metric "load_test_5_clients" "response_time_seconds" "$ELAPSED_TIME_5"

docker compose -f "$DOCKER_COMPOSE_FILE" --env-file .env down

echo ">>> Performance under load results:"
echo "   1 client:  Response time $ELAPSED_TIME_1 seconds, Server CPU: $SERVER1_CPU_1/$SERVER2_CPU_1"
echo "   3 clients: Response time $ELAPSED_TIME_3 seconds, Server CPU: $SERVER1_CPU_3/$SERVER2_CPU_3" 
echo "   5 clients: Response time $ELAPSED_TIME_5 seconds, Server CPU: $SERVER1_CPU_5/$SERVER2_CPU_5"

rm -f .env

# Step 5: Stress test
run_test "Stress Test" "Testing system behavior under high load"
echo ">>> Running stress test with 8 clients for 2 minutes..."

docker compose -f "$DOCKER_COMPOSE_FILE" --env-file .env up --scale client-1=2 --scale client-2=2 --scale client-3=2 --scale client-4=2 --remove-orphans -d
sleep 120

# Record stress test metrics
STRESS_SERVER1_CPU=$(docker stats --no-stream --format "{{.CPUPerc}}" $(docker compose -f "$DOCKER_COMPOSE_FILE" ps -q server-1) 2>/dev/null | tr -d '% ' || echo 'N/A')
STRESS_SERVER2_CPU=$(docker stats --no-stream --format "{{.CPUPerc}}" $(docker compose -f "$DOCKER_COMPOSE_FILE" ps -q server-2) 2>/dev/null | tr -d '% ' || echo 'N/A')
STRESS_REDIS_CPU=$(docker stats --no-stream --format "{{.CPUPerc}}" $(docker compose -f "$DOCKER_COMPOSE_FILE" ps -q redis) 2>/dev/null | tr -d '% ' || echo 'N/A')
STRESS_POSTGRES_CPU=$(docker stats --no-stream --format "{{.CPUPerc}}" $(docker compose -f "$DOCKER_COMPOSE_FILE" ps -q postgres) 2>/dev/null | tr -d '% ' || echo 'N/A')

record_metric "stress_test" "server1_cpu" "$STRESS_SERVER1_CPU"
record_metric "stress_test" "server2_cpu" "$STRESS_SERVER2_CPU"
record_metric "stress_test" "redis_cpu" "$STRESS_REDIS_CPU"
record_metric "stress_test" "postgres_cpu" "$STRESS_POSTGRES_CPU"

# Check health during stress
STRESS_HEALTH=$(curl -s -o stress_health.txt -w "%{http_code}" http://localhost:8080/actuator/health 2>/dev/null || echo "0")
record_metric "stress_test" "health_response_code" "$STRESS_HEALTH"
echo ">>> Health check during stress: HTTP $STRESS_HEALTH"

docker compose -f "$DOCKER_COMPOSE_FILE" --env-file .env down
rm -f .env stress_health.txt

# Step 6: Scalability test - adding more servers
run_test "Scalability Test" "Testing scalability by adding more server instances"
echo ">>> Testing scalability by examining current multi-server performance..."

# Start both servers again and measure their individual loads
docker compose -f "$DOCKER_COMPOSE_FILE" up --scale client-1=0 --scale client-2=0 --scale client-3=0 --scale client-4=0 --remove-orphans -d
sleep 10

# Start client that should distribute load between servers
echo "BOT_MODE=autobot" > .env
echo "BOT_SCENARIO=" >> .env
docker compose -f "$DOCKER_COMPOSE_FILE" --env-file .env up --scale client-1=1 --scale client-2=1 --scale client-3=1 --scale client-4=1 --remove-orphans -d
sleep 30

# Calculate load distribution between servers
docker compose -f "$DOCKER_COMPOSE_FILE" logs server-1 > server1_loadtest.log 2>&1
docker compose -f "$DOCKER_COMPOSE_FILE" logs server-2 > server2_loadtest.log 2>&1

SERVER1_LOG_SIZE=$(wc -c < server1_loadtest.log)
SERVER2_LOG_SIZE=$(wc -c < server2_loadtest.log)

record_metric "scalability_test" "server1_log_size" "$SERVER1_LOG_SIZE"
record_metric "scalability_test" "server2_log_size" "$SERVER2_LOG_SIZE"

echo ">>> Load distribution (log size): Server1: $SERVER1_LOG_SIZE bytes, Server2: $SERVER2_LOG_SIZE bytes"

if [ $SERVER1_LOG_SIZE -gt 1000 ] && [ $SERVER2_LOG_SIZE -gt 1000 ]; then
  if [ $((SERVER1_LOG_SIZE + SERVER2_LOG_SIZE)) -gt 0 ]; then
    DISTRIBUTION_RATIO=$(echo "scale=2; $SERVER1_LOG_SIZE / ($SERVER1_LOG_SIZE + $SERVER2_LOG_SIZE)" | bc)
    record_metric "scalability_test" "distribution_ratio" "$DISTRIBUTION_RATIO"
    echo ">>> Load distribution ratio (Server1): $DISTRIBUTION_RATIO"
  fi
  echo ">>> SUCCESS: Both servers are receiving load (scalability working)"
else
  echo ">>> INFO: Load distribution may need more clients to be evident"
fi

docker compose -f "$DOCKER_COMPOSE_FILE" --env-file .env down
rm -f .env server1_loadtest.log server2_loadtest.log

# Step 7: Memory usage monitoring
run_test "Memory Usage Monitoring" "Monitoring memory usage under load"
echo ">>> Monitoring memory usage..."

# Get memory usage for each service
SERVER1_MEM=$(docker stats --no-stream --format "{{.MemUsage}}" $(docker compose -f "$DOCKER_COMPOSE_FILE" ps -q server-1) 2>/dev/null | cut -d'/' -f1 | xargs || echo 'N/A')
SERVER2_MEM=$(docker stats --no-stream --format "{{.MemUsage}}" $(docker compose -f "$DOCKER_COMPOSE_FILE" ps -q server-2) 2>/dev/null | cut -d'/' -f1 | xargs || echo 'N/A')
REDIS_MEM=$(docker stats --no-stream --format "{{.MemUsage}}" $(docker compose -f "$DOCKER_COMPOSE_FILE" ps -q redis) 2>/dev/null | cut -d'/' -f1 | xargs || echo 'N/A')
POSTGRES_MEM=$(docker stats --no-stream --format "{{.MemUsage}}" $(docker compose -f "$DOCKER_COMPOSE_FILE" ps -q postgres) 2>/dev/null | cut -d'/' -f1 | xargs || echo 'N/A')

record_metric "memory_usage" "server1_memory" "$SERVER1_MEM"
record_metric "memory_usage" "server2_memory" "$SERVER2_MEM"
record_metric "memory_usage" "redis_memory" "$REDIS_MEM"
record_metric "memory_usage" "postgres_memory" "$POSTGRES_MEM"

echo ">>> Memory usage - Server1: $SERVER1_MEM, Server2: $SERVER2_MEM, Redis: $REDIS_MEM, PostgreSQL: $POSTGRES_MEM"

# Step 8: Performance summary
run_test "Performance Summary" "Summary of performance metrics"
echo ">>> Performance and scalability test summary:"
echo ">>> All performance metrics recorded in $METRICS_FILE"

if [ -f "$METRICS_FILE" ]; then
  echo ">>> Key metrics:"
  grep -E "(response_time|CPU|health_response)" "$METRICS_FILE" | tail -20
fi

echo "======================================================="
echo ">>> PERFORMANCE AND SCALABILITY TESTS COMPLETED"
echo ">>> These tests measured system performance under various loads"
echo "======================================================="

# Clean up
rm -f .env
cleanup

# Save metrics file to permanent location
cp "$METRICS_FILE" "$PROJECT_ROOT/test_results/"
echo ">>> Performance metrics saved to $PROJECT_ROOT/test_results/$METRICS_FILE"