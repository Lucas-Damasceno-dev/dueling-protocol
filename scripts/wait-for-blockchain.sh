#!/bin/bash

# Wait for blockchain contracts to be deployed
# This script checks if deployment-info.json is available via HTTP

echo "⏳ Waiting for blockchain contracts to be deployed..."

MAX_RETRIES=30
RETRY_COUNT=0

while [ $RETRY_COUNT -lt $MAX_RETRIES ]; do
    # Try to get deployment info from blockchain container
    if curl -s http://dueling-blockchain:3000/deployment-info.json > /dev/null 2>&1; then
        echo "✅ Blockchain contracts deployed!"
        exit 0
    fi
    
    RETRY_COUNT=$((RETRY_COUNT + 1))
    echo "  Attempt $RETRY_COUNT/$MAX_RETRIES - Waiting..."
    sleep 2
done

echo "⚠️  Warning: Could not verify blockchain deployment"
echo "   Continuing anyway..."
exit 0
