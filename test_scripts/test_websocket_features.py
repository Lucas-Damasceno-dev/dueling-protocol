#!/usr/bin/env python3
"""
Teste WebSocket para TRADE, MATCH e PURCHASE
"""

import asyncio
import websockets
import json
import requests
import sys
from datetime import datetime

API_URL = "http://localhost:8080"
WS_URL = "ws://localhost:8080/ws"

class Colors:
    GREEN = '\033[92m'
    RED = '\033[91m'
    YELLOW = '\033[93m'
    BLUE = '\033[94m'
    END = '\033[0m'

def log(msg, color=Colors.BLUE):
    print(f"{color}{msg}{Colors.END}")

def register_and_login(username, password):
    """Register user and get JWT token"""
    # Register
    resp = requests.post(f"{API_URL}/api/auth/register",
                        json={"username": username, "password": password})
    log(f"Register {username}: {resp.status_code}")
    
    # Login
    resp = requests.post(f"{API_URL}/api/auth/login",
                        json={"username": username, "password": password})
    data = resp.json()
    token = data.get('token')
    if not token:
        log(f"ERROR: No token for {username}", Colors.RED)
        sys.exit(1)
    log(f"Token for {username}: {token[:20]}...", Colors.GREEN)
    return token

async def test_purchase(token, username):
    """Test PURCHASE via WebSocket"""
    log(f"\n=== Testing PURCHASE for {username} ===", Colors.YELLOW)
    
    uri = f"{WS_URL}?token={token}"
    messages_received = []
    
    try:
        async with websockets.connect(uri) as websocket:
            log("✓ WebSocket connected")
            
            # Wait for initial SUCCESS:CONNECTED
            response = await asyncio.wait_for(websocket.recv(), timeout=5.0)
            log(f"Initial: {response}")
            messages_received.append(response)
            
            # Send STORE:BUY:BASIC
            await websocket.send("STORE:BUY:BASIC")
            log("→ Sent: STORE:BUY:BASIC")
            
            # Wait for response (timeout 10s)
            try:
                response = await asyncio.wait_for(websocket.recv(), timeout=10.0)
                log(f"← Response: {response}", Colors.GREEN)
                messages_received.append(response)
                
                if "SUCCESS" in response or "CARDS" in response:
                    log("✅ PURCHASE SUCCESS", Colors.GREEN)
                    return True
                elif "ERROR" in response:
                    log(f"❌ PURCHASE ERROR: {response}", Colors.RED)
                    return False
            except asyncio.TimeoutError:
                log("❌ PURCHASE TIMEOUT - No response", Colors.RED)
                return False
                
    except Exception as e:
        log(f"❌ PURCHASE Exception: {e}", Colors.RED)
        return False

async def test_trade(token1, username1, token2, username2):
    """Test TRADE between two users"""
    log(f"\n=== Testing TRADE: {username1} ↔ {username2} ===", Colors.YELLOW)
    
    messages1 = []
    messages2 = []
    
    try:
        # Connect both users
        uri1 = f"{WS_URL}?token={token1}"
        uri2 = f"{WS_URL}?token={token2}"
        
        async with websockets.connect(uri1) as ws1, websockets.connect(uri2) as ws2:
            log("✓ Both WebSockets connected")
            
            # Wait for initial messages
            await asyncio.wait_for(ws1.recv(), timeout=5.0)
            await asyncio.wait_for(ws2.recv(), timeout=5.0)
            
            # User 1 proposes trade
            # Format: TRADE:PROPOSE:targetPlayerId:offeredCardIds:requestedCardIds
            await ws1.send(f"TRADE:PROPOSE:2:basic-0:basic-1")
            log(f"→ {username1} sent: TRADE:PROPOSE:2:basic-0:basic-1")
            
            # Wait for responses
            await asyncio.sleep(2)
            
            # Check if user2 received proposal
            try:
                msg2 = await asyncio.wait_for(ws2.recv(), timeout=5.0)
                log(f"← {username2} received: {msg2}", Colors.GREEN)
                messages2.append(msg2)
                
                if "TRADE" in msg2 or "PROPOSAL" in msg2:
                    log("✅ TRADE PROPOSAL RECEIVED", Colors.GREEN)
                    
                    # User 2 accepts trade
                    # Extract trade ID from message if present
                    await ws2.send("TRADE:ACCEPT:1")  # Assuming trade ID 1
                    log(f"→ {username2} sent: TRADE:ACCEPT:1")
                    
                    # Wait for completion
                    await asyncio.sleep(2)
                    
                    # Check both users for completion
                    try:
                        msg1 = await asyncio.wait_for(ws1.recv(), timeout=5.0)
                        log(f"← {username1} received: {msg1}", Colors.GREEN)
                        if "COMPLETE" in msg1 or "SUCCESS" in msg1:
                            log("✅ TRADE COMPLETE", Colors.GREEN)
                            return True
                    except asyncio.TimeoutError:
                        pass
                        
            except asyncio.TimeoutError:
                log(f"❌ TRADE - {username2} did not receive proposal", Colors.RED)
                return False
                
    except Exception as e:
        log(f"❌ TRADE Exception: {e}", Colors.RED)
        return False
    
    return False

async def test_matchmaking(token1, username1, token2, username2):
    """Test MATCHMAKING between two users"""
    log(f"\n=== Testing MATCHMAKING: {username1} vs {username2} ===", Colors.YELLOW)
    
    try:
        uri1 = f"{WS_URL}?token={token1}"
        uri2 = f"{WS_URL}?token={token2}"
        
        async with websockets.connect(uri1) as ws1, websockets.connect(uri2) as ws2:
            log("✓ Both WebSockets connected")
            
            # Wait for initial messages
            await asyncio.wait_for(ws1.recv(), timeout=5.0)
            await asyncio.wait_for(ws2.recv(), timeout=5.0)
            
            # Both enter matchmaking
            await ws1.send("MATCHMAKING")
            log(f"→ {username1} sent: MATCHMAKING")
            await asyncio.sleep(1)
            
            await ws2.send("MATCHMAKING")
            log(f"→ {username2} sent: MATCHMAKING")
            
            # Wait for match creation (timeout 15s)
            await asyncio.sleep(5)
            
            # Check for match messages
            try:
                msg1 = await asyncio.wait_for(ws1.recv(), timeout=10.0)
                log(f"← {username1} received: {msg1}", Colors.GREEN)
                
                if "MATCH" in msg1 or "OPPONENT" in msg1 or "GAME" in msg1:
                    log("✅ MATCHMAKING SUCCESS - Match created", Colors.GREEN)
                    return True
                    
            except asyncio.TimeoutError:
                log("❌ MATCHMAKING TIMEOUT - No match created", Colors.RED)
                return False
                
    except Exception as e:
        log(f"❌ MATCHMAKING Exception: {e}", Colors.RED)
        return False
    
    return False

async def main():
    log("╔════════════════════════════════════════════════╗", Colors.BLUE)
    log("║   TESTE WEBSOCKET: PURCHASE + TRADE + MATCH   ║", Colors.BLUE)
    log("╚════════════════════════════════════════════════╝", Colors.BLUE)
    
    # Create test users
    timestamp = int(datetime.now().timestamp())
    user1 = f"testuser1_{timestamp}"
    user2 = f"testuser2_{timestamp}"
    
    log(f"\nCreating users: {user1}, {user2}")
    token1 = register_and_login(user1, "pass123")
    token2 = register_and_login(user2, "pass123")
    
    # Test 1: PURCHASE
    purchase_ok = await test_purchase(token1, user1)
    
    # Test 2: TRADE
    trade_ok = await test_trade(token1, user1, token2, user2)
    
    # Test 3: MATCHMAKING
    match_ok = await test_matchmaking(token1, user1, token2, user2)
    
    # Summary
    log("\n╔════════════════════════════════════════════════╗", Colors.BLUE)
    log("║                    RESUMO                      ║", Colors.BLUE)
    log("╚════════════════════════════════════════════════╝", Colors.BLUE)
    log(f"1. PURCHASE:    {'✅ PASSED' if purchase_ok else '❌ FAILED'}", 
        Colors.GREEN if purchase_ok else Colors.RED)
    log(f"2. TRADE:       {'✅ PASSED' if trade_ok else '❌ FAILED'}", 
        Colors.GREEN if trade_ok else Colors.RED)
    log(f"3. MATCHMAKING: {'✅ PASSED' if match_ok else '❌ FAILED'}", 
        Colors.GREEN if match_ok else Colors.RED)
    
    success = purchase_ok and trade_ok and match_ok
    sys.exit(0 if success else 1)

if __name__ == "__main__":
    asyncio.run(main())
