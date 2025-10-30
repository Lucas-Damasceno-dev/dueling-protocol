#!/bin/bash

# Test script to verify Redis Sentinel setup
echo "=== Testing Redis Sentinel Setup ==="

# Start the services with docker compose
echo "Starting Redis Sentinel services..."
docker compose -f docker/docker-compose.yml up -d redis-master redis-slave redis-sentinel-1 redis-sentinel-2 redis-sentinel-3

# Wait for services to be ready
echo "Waiting for services to be ready..."
sleep 45

# Test Redis master
echo "Testing Redis Master..."
if docker exec redis-master redis-cli ping; then
    echo "✓ Redis Master is responding"
else
    echo "✗ Redis Master is not responding"
fi

# Test Redis slave
echo "Testing Redis Slave..."
if docker exec redis-slave redis-cli ping; then
    echo "✓ Redis Slave is responding"
else
    echo "✗ Redis Slave is not responding"
fi

# Test Redis Sentinel instances
echo "Testing Redis Sentinel instances..."

for sentinel in redis-sentinel-1 redis-sentinel-2 redis-sentinel-3; do
    if docker exec $sentinel redis-cli -p 26379 ping; then
        echo "✓ $sentinel is responding"
    else
        echo "✗ $sentinel is not responding"
    fi
    
    # Check sentinel info
    echo "Checking $sentinel info..."
    docker exec $sentinel redis-cli -p 26379 sentinel master mymaster | grep -E "^(name|num-slaves|num-other-sentinels|flags|quorum):"
done

# Test that master and slave are properly configured
echo "Checking master-slave configuration..."
echo "Master info:"
docker exec redis-master redis-cli info replication | grep -E "^(role|connected_slaves):"

echo "Slave info:"
docker exec redis-slave redis-cli info replication | grep -E "^(role|master_host|master_port|master_link_status):"

# Test basic set/get operations
echo "Testing basic Redis operations..."
docker exec redis-master redis-cli set testkey "Hello from master"
MASTER_VALUE=$(docker exec redis-master redis-cli get testkey)
SLAVE_VALUE=$(docker exec redis-slave redis-cli get testkey)

echo "Master set value: $MASTER_VALUE"
echo "Slave replicated value: $SLAVE_VALUE"

if [ "$MASTER_VALUE" = "$SLAVE_VALUE" ] && [ "$MASTER_VALUE" = "Hello from master" ]; then
    echo "✓ Data replication is working correctly"
else
    echo "✗ Data replication issue"
fi

# Check sentinel monitoring
echo "Checking Sentinel monitoring configuration..."
for sid in 1 2 3; do
    SENTINEL_NAME="redis-sentinel-$sid"
    MASTER_INFO=$(docker exec $SENTINEL_NAME redis-cli -p 26379 sentinel master mymaster)
    # Extract the num-other-sentinels value from the output
    NUM_OTHER_SENTINELS=$(echo "$MASTER_INFO" | grep -A 1 "num-other-sentinels" | tail -1)
    if [ "$NUM_OTHER_SENTINELS" = "2" ]; then
        echo "✓ $SENTINEL_NAME correctly sees 2 other sentinels"
    else
        echo "✗ $SENTINEL_NAME sentinel count issue"
    fi
done

# Cleanup
echo "Stopping Redis Sentinel services..."
docker compose -f docker/docker-compose.yml down

echo "=== Redis Sentinel test completed ==="