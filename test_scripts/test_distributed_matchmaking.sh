#!/usr/bin/env bash

set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"
PROJECT_ROOT="$SCRIPT_DIR/.."
DOCKER_COMPOSE_FILE="$PROJECT_ROOT/docker/docker-compose.yml"

echo "======================================================="
echo ">>> STARTING DISTRIBUTED MATCHMAKING INTEGRATION TEST"
echo "======================================================="

cleanup() {
  echo ">>> Cleaning up Docker environment..."
  docker compose -f "$DOCKER_COMPOSE_FILE" down --remove-orphans
}

trap cleanup EXIT

echo ">>> Building project JAR..."
(cd "$PROJECT_ROOT" && mvn clean package)

echo ">>> Building and starting services..."
docker compose -f "$DOCKER_COMPOSE_FILE" up --build -d

echo ">>> Waiting for clients to run and matchmaking to occur..."
sleep 20 

echo ">>> Verifying distributed matchmaking..."

docker compose -f "$DOCKER_COMPOSE_FILE" logs > matchmaking_logs.txt

if grep "New match created" matchmaking_logs.txt; then
  echo ">>> SUCCESS: Distributed match creation message found in logs."
  docker compose -f "$DOCKER_COMPOSE_FILE" logs
else
  echo ">>> FAILURE: Could not find match creation message in logs."
  docker compose -f "$DOCKER_COMPOSE_FILE" logs
  exit 1
fi

echo "======================================================="
echo ">>> DISTRIBUTED MATCHMAKING TEST FINISHED"
echo "======================================================="
