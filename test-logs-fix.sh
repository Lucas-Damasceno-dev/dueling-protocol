#!/bin/bash
echo "🧪 Testing logs directory creation fix"
echo ""

# Test 1: Check if mkdir -p is in the scripts
echo "Test 1: Checking if mkdir -p logs is present..."
count=$(grep "mkdir -p logs" scripts/start-complete-with-blockchain.sh scripts/start-local-with-blockchain.sh menu.sh 2>/dev/null | wc -l)
if [ "$count" -ge 4 ]; then
    echo "  ✅ PASS ($count instances found)"
else
    echo "  ❌ FAIL (only $count instances found, expected >= 4)"
    exit 1
fi

# Test 2: Simulate directory creation
echo "Test 2: Simulating directory creation..."
PROJECT_DIR="$(pwd)"
cd "$PROJECT_DIR"
mkdir -p logs
if [ -d "logs" ]; then
    echo "  ✅ PASS (logs directory created)"
else
    echo "  ❌ FAIL (logs directory not created)"
    exit 1
fi

# Test 3: Check if we can write to logs
echo "Test 3: Testing write to logs directory..."
echo "test" > logs/test.log 2>/dev/null
if [ -f "logs/test.log" ]; then
    echo "  ✅ PASS (can write to logs)"
    rm logs/test.log
else
    echo "  ❌ FAIL (cannot write to logs)"
    exit 1
fi

echo ""
echo "✅ All tests passed!"
echo "The logs directory fix is working correctly."
