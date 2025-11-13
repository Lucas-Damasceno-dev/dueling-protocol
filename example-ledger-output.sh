#!/bin/bash

# Example of the new enhanced blockchain ledger verification output

cat << 'EOF'

==================================================
  BLOCKCHAIN DISTRIBUTED LEDGER VERIFICATION
==================================================

✅ Blockchain node is running

✅ Loaded addresses from deployment-info.json

=== BLOCKCHAIN LEDGER VERIFICATION ===

Contract Addresses:
  AssetContract: 0x5FbDB2315678afecb367f032d93F642f64180aa3
  MatchContract: 0xDc64a140Aa3E981100a9becA4E685f962f0cF6C9

💳 CARDS MINTED (from pack purchases): 10 cards

  📦 Player1 cards (5 total):
     • Token #0 - Monster (Common) [ATK:50 DEF:1]
     • Token #1 - Monster (Common) [ATK:50 DEF:1]
     • Token #2 - Spell (Rare) [ATK:3 DEF:3]
     • Token #3 - Trap (Common) [ATK:0 DEF:2]
     • Token #4 - Monster (Epic) [ATK:100 DEF:50]

  📦 Player2 cards (5 total):
     • Token #5 - Monster (Common) [ATK:50 DEF:1]
     • Token #6 - Monster (Common) [ATK:50 DEF:1]
     • Token #7 - Spell (Uncommon) [ATK:2 DEF:2]
     • Token #8 - Monster (Rare) [ATK:75 DEF:25]
     • Token #9 - Spell (Legendary) [ATK:200 DEF:100]

🔄 CARD TRANSFERS (trades): 2 transfers

  🤝 Trade #1:
     Player1 gave:
       → Token #2 - Spell (Rare)
              ⇅ TRADE ⇅
     Player2 gave:
       → Token #7 - Spell (Uncommon)

⚔️  MATCHES: 1 recorded

  ⚔️  Match #1:
     Player1 (Score: 0) 💀 DEFEAT
              ⚔️  VS  ⚔️
     Player2 (Score: 100) 🏆 VICTORY
     Winner: Player2
     Block: 42 | TX: 0x1234567890abcdef...

=== SUMMARY ===
Total Cards Minted: 10
Total Card Transfers: 2
Total Matches Recorded: 1
Total Blockchain Transactions: 13

=== VERIFICATION COMPLETE ===

==================================================
  VERIFICATION COMPLETE
==================================================

EOF

echo ""
echo "📝 This is the NEW enhanced format for blockchain ledger verification!"
echo ""
echo "Features:"
echo "  ✅ Shows player cards with details (type, rarity, ATK/DEF)"
echo "  ✅ Shows trades with card details in a visual format"
echo "  ✅ Shows matches with VICTORY/DEFEAT status and scores"
echo "  ✅ More readable and informative"
echo ""
