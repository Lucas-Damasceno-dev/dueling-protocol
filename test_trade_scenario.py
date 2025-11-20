#!/usr/bin/env python3
"""
Test script to simulate a complete trade scenario and verify blockchain recording
"""

import requests
import json
import time

GATEWAY_URL = "http://localhost:8080"

def register_player(nickname, password="test123"):
    """Register a new player"""
    response = requests.post(f"{GATEWAY_URL}/api/player", json={
        "nickname": nickname,
        "password": password
    })
    if response.status_code == 200:
        player = response.json()
        print(f"✅ Registered {nickname}: {player['id']}")
        return player
    else:
        print(f"❌ Failed to register {nickname}: {response.text}")
        return None

def buy_pack(player_id, pack_type="BASIC"):
    """Buy a card pack"""
    response = requests.post(f"{GATEWAY_URL}/api/store/buy", json={
        "playerId": player_id,
        "packType": pack_type
    })
    if response.status_code == 200:
        result = response.json()
        cards = result.get('cards', [])
        print(f"✅ Player {player_id[:8]} bought pack, received {len(cards)} cards")
        return cards
    else:
        print(f"❌ Failed to buy pack: {response.text}")
        return []

def main():
    print("╔════════════════════════════════════════════════════════════════╗")
    print("║          🔄 TRADE BLOCKCHAIN RECORDING TEST                    ║")
    print("╚════════════════════════════════════════════════════════════════╝")
    print()
    
    # Step 1: Register two players
    print("Step 1/4: Registering players...")
    player1 = register_player("TradeTest1")
    player2 = register_player("TradeTest2")
    
    if not player1 or not player2:
        print("❌ Failed to register players")
        return
    
    time.sleep(2)
    print()
    
    # Step 2: Buy packs for both players
    print("Step 2/4: Buying card packs...")
    cards1 = buy_pack(player1['id'])
    cards2 = buy_pack(player2['id'])
    
    if not cards1 or not cards2:
        print("❌ Failed to buy packs")
        return
    
    print(f"   Player1 cards: {[c['id'] for c in cards1[:3]]}")
    print(f"   Player2 cards: {[c['id'] for c in cards2[:3]]}")
    print()
    
    # Step 3: Wait for blockchain minting
    print("Step 3/4: Waiting for blockchain minting (10 seconds)...")
    time.sleep(10)
    print("✅ Blockchain minting should be complete")
    print()
    
    # Step 4: Note about trade execution
    print("Step 4/4: Trade execution...")
    print("⚠️  Note: Trade execution requires WebSocket connection.")
    print("   To complete this test, use the client application or menu.sh")
    print("   to connect as these players and execute a trade.")
    print()
    print("   Example trade commands:")
    print(f"   COMMAND:TRADE:PROPOSE:{player2['id']}:{cards1[0]['id']}:{cards2[0]['id']}")
    print(f"   COMMAND:TRADE:ACCEPT:<trade_id>")
    print()
    
    print("╔════════════════════════════════════════════════════════════════╗")
    print("║          ✅ SETUP COMPLETE                                      ║")
    print("╚════════════════════════════════════════════════════════════════╝")
    print()
    print("📝 Players created and packs purchased.")
    print("   Use the client to complete the trade, then verify with:")
    print("   ./scripts/verify_blockchain_ledger.sh")
    print()

if __name__ == "__main__":
    main()
