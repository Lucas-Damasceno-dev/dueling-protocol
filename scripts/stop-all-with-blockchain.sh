#!/bin/bash

# Stop All Services Including Blockchain

echo "🛑 Stopping All Dueling Protocol Services..."
echo "============================================="
echo ""

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$PROJECT_DIR"

echo "1/2: Stopping all running Java processes..."
# Stop any running game servers or clients started locally
pkill -f "dueling-server" 2>/dev/null && echo "✅ Killed Java server processes" || true
pkill -f "dueling-client" 2>/dev/null && echo "✅ Killed Java client processes" || true
pkill -f "dueling-gateway" 2>/dev/null && echo "✅ Killed Java gateway processes" || true


echo ""
echo "2/2: Stopping all Docker Containers..."
cd docker
# The 'down' command stops and removes containers, networks, volumes, and images created by 'up'.
# The -v flag also removes named volumes.
docker compose down -v --remove-orphans 2>/dev/null || true
cd ..

echo ""
echo "============================================="
echo "✅ All Services Stopped!"
echo "============================================="
echo ""
echo "To start again:"
echo "   ./menu.sh → 5 (Complete System)"
echo "   ./menu.sh → 6 (Local Mode)"
echo ""