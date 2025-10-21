#!/bin/bash

echo "=== WebSocket Status Check ==="
echo

echo "📊 Active WebSocket Sessions in Redis:"
docker exec redis-master redis-cli HLEN "websocket:sessions"
echo

echo "🔍 Recent WebSocket Activity (last 20 connections):"
docker logs server-1 2>&1 | grep "WebSocket connection" | tail -20
echo

echo "⚠️  Recent Disconnections:"
docker logs server-1 2>&1 | grep "connection closed" | tail -10
echo

echo "📝 Recent STORE Commands:"
docker logs server-1 2>&1 | grep "STORE:BUY" | tail -10
echo

echo "💡 If a client can't send commands:"
echo "   1. Check if WebSocket connected: look for 'WebSocket connection established'"
echo "   2. Check if it disconnected: look for 'connection closed'"
echo "   3. Restart the client if connection was lost"
