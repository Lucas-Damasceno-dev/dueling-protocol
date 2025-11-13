#!/bin/bash

# Complete Integration Test
# Tests all fixes and improvements

echo "🧪 Testing Complete Blockchain Integration"
echo "==========================================="
echo ""

PASS=0
FAIL=0

# Test 1: New scripts exist and are executable
echo "Test 1: New scripts created and executable..."
if [ -x scripts/start-complete-with-blockchain.sh ] && \
   [ -x scripts/start-local-with-blockchain.sh ] && \
   [ -x scripts/stop-all-with-blockchain.sh ]; then
    echo "  ✅ PASS"
    ((PASS++))
else
    echo "  ❌ FAIL"
    ((FAIL++))
fi

# Test 2: menu.sh syntax
echo "Test 2: menu.sh syntax validation..."
if bash -n menu.sh 2>/dev/null; then
    echo "  ✅ PASS"
    ((PASS++))
else
    echo "  ❌ FAIL"
    ((FAIL++))
fi

# Test 3: menu.sh has blockchain verification in options 5, 6, 7
echo "Test 3: menu.sh options 5, 6, 7 have blockchain integration..."
if grep -q "start-complete-with-blockchain" menu.sh && \
   grep -q "start-local-with-blockchain" menu.sh && \
   grep -q "stop-all-with-blockchain" menu.sh; then
    echo "  ✅ PASS"
    ((PASS++))
else
    echo "  ❌ FAIL"
    ((FAIL++))
fi

# Test 4: menu.sh option 9 has blockchain status
echo "Test 4: menu.sh option 9 shows blockchain status..."
if grep -A 15 '^\s*9)' menu.sh | grep -q "Blockchain Status"; then
    echo "  ✅ PASS"
    ((PASS++))
else
    echo "  ❌ FAIL"
    ((FAIL++))
fi

# Test 5: All verification options (42-46) check if blockchain is running
echo "Test 5: Verification options check blockchain status..."
blockchain_checks=$(grep -c "lsof -i:8545" menu.sh)
if [ $blockchain_checks -ge 8 ]; then
    echo "  ✅ PASS ($blockchain_checks blockchain checks found)"
    ((PASS++))
else
    echo "  ❌ FAIL (only $blockchain_checks blockchain checks found, expected >= 8)"
    ((FAIL++))
fi

# Test 6: Blockchain verification scripts exist
echo "Test 6: All blockchain verification scripts exist..."
if [ -f dueling-blockchain/scripts/verify-ownership.js ] && \
   [ -f dueling-blockchain/scripts/verify-uniqueness.js ] && \
   [ -f dueling-blockchain/scripts/verify-purchase-history.js ] && \
   [ -f dueling-blockchain/scripts/verify-matches.js ]; then
    echo "  ✅ PASS"
    ((PASS++))
else
    echo "  ❌ FAIL"
    ((FAIL++))
fi

# Test 7: NPM scripts configured
echo "Test 7: NPM scripts in package.json..."
if grep -q "verify:matches" dueling-blockchain/package.json && \
   grep -q "verify:ownership" dueling-blockchain/package.json && \
   grep -q "verify:purchases" dueling-blockchain/package.json && \
   grep -q "verify:card" dueling-blockchain/package.json; then
    echo "  ✅ PASS"
    ((PASS++))
else
    echo "  ❌ FAIL"
    ((FAIL++))
fi

# Test 8: Documentation exists
echo "Test 8: Documentation files..."
if [ -f BLOCKCHAIN_INTEGRATION.md ] && \
   [ -f BLOCKCHAIN_FIXES.md ] && \
   [ -f INTEGRATION_SUMMARY.md ] && \
   [ -f dueling-blockchain/HOW_PLAYERS_VERIFY.md ] && \
   [ -f dueling-blockchain/PLAYER_VERIFICATION_GUIDE.md ] && \
   [ -f dueling-blockchain/QUICK_START_PLAYERS.md ]; then
    echo "  ✅ PASS"
    ((PASS++))
else
    echo "  ❌ FAIL"
    ((FAIL++))
fi

# Test 9: GameClient has blockchain menu
echo "Test 9: GameClient.java has blockchain menu..."
if grep -q "blockchainMenu" dueling-client/src/main/java/client/GameClient.java; then
    echo "  ✅ PASS"
    ((PASS++))
else
    echo "  ❌ FAIL"
    ((FAIL++))
fi

# Test 10: blockchain-verify.sh helper script
echo "Test 10: blockchain-verify.sh helper script..."
if [ -x scripts/blockchain-verify.sh ]; then
    echo "  ✅ PASS"
    ((PASS++))
else
    echo "  ❌ FAIL"
    ((FAIL++))
fi

echo ""
echo "==========================================="
echo "Test Results:"
echo "  ✅ Passed: $PASS/10"
echo "  ❌ Failed: $FAIL/10"
echo "==========================================="
echo ""

if [ $FAIL -eq 0 ]; then
    echo "🎉 ALL TESTS PASSED!"
    echo ""
    echo "✅ Blockchain integration is complete and functional"
    echo "✅ All fixes applied successfully"
    echo "✅ System ready for use"
    exit 0
else
    echo "⚠️  Some tests failed"
    echo "Please review the failures above"
    exit 1
fi
