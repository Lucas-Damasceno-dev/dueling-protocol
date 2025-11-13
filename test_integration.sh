#!/bin/bash

echo "🔍 Testing Blockchain Integration..."
echo ""

# Test 1: menu.sh syntax
echo "✓ Test 1: menu.sh syntax"
bash -n menu.sh && echo "  ✅ Pass" || echo "  ❌ Fail"

# Test 2: blockchain-verify.sh exists and is executable
echo "✓ Test 2: blockchain-verify.sh"
[ -x scripts/blockchain-verify.sh ] && echo "  ✅ Pass" || echo "  ❌ Fail"

# Test 3: All verification scripts exist
echo "✓ Test 3: Verification scripts"
all_exist=true
for script in verify-ownership verify-uniqueness verify-purchase-history verify-matches; do
    [ -f "dueling-blockchain/scripts/${script}.js" ] || all_exist=false
done
$all_exist && echo "  ✅ Pass" || echo "  ❌ Fail"

# Test 4: package.json has new scripts
echo "✓ Test 4: NPM scripts configured"
grep -q "verify:matches" dueling-blockchain/package.json && echo "  ✅ Pass" || echo "  ❌ Fail"

# Test 5: Documentation exists
echo "✓ Test 5: Documentation files"
docs_exist=true
for doc in BLOCKCHAIN_INTEGRATION.md INTEGRATION_SUMMARY.md; do
    [ -f "$doc" ] || docs_exist=false
done
for doc in HOW_PLAYERS_VERIFY.md PLAYER_VERIFICATION_GUIDE.md QUICK_START_PLAYERS.md; do
    [ -f "dueling-blockchain/$doc" ] || docs_exist=false
done
$docs_exist && echo "  ✅ Pass" || echo "  ❌ Fail"

# Test 6: GameClient.java has blockchain menu
echo "✓ Test 6: Client blockchain menu"
grep -q "blockchainMenu" dueling-client/src/main/java/client/GameClient.java && echo "  ✅ Pass" || echo "  ❌ Fail"

echo ""
echo "🎉 Integration Test Complete!"
