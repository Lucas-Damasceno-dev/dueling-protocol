#!/usr/bin/env python3
"""
Simple WebSocket test for cross-server functionality
Requires: pip install websocket-client requests
"""

import json
import time
import requests
import websocket
import threading
from datetime import datetime

# Configuration
SERVER1_URL = "http://localhost:8080"
SERVER2_URL = "http://localhost:8083"
WS1_URL = "ws://localhost:8080/ws"
WS2_URL = "ws://localhost:8083/ws"

TIMESTAMP = str(int(time.time()))
USER_A = f"player_a_{TIMESTAMP}"
USER_B = f"player_b_{TIMESTAMP}"
PASSWORD = "pass123"

class Colors:
    GREEN = '\033[92m'
    YELLOW = '\033[93m'
    RED = '\033[91m'
    BLUE = '\033[94m'
    END = '\033[0m'

def authenticate(server_url, username, password):
    """Register and login user, return JWT token"""
    print(f"{Colors.GREEN}Authenticating {username} on {server_url}...{Colors.END}")
    
    # Register
    reg_response = requests.post(
        f"{server_url}/api/auth/register",
        json={"username": username, "password": password}
    )
    print(f"  Register: {reg_response.json()}")
    
    # Login
    login_response = requests.post(
        f"{server_url}/api/auth/login",
        json={"username": username, "password": password}
    )
    
    data = login_response.json()
    token = data.get('token')
    
    if not token:
        print(f"{Colors.RED}❌ Failed to get token for {username}{Colors.END}")
        return None
    
    print(f"{Colors.GREEN}✓ {username} authenticated{Colors.END}")
    return token

def on_message(ws, message):
    """Handle WebSocket messages"""
    print(f"{Colors.BLUE}[WS] Received: {message}{Colors.END}")

def on_error(ws, error):
    """Handle WebSocket errors"""
    print(f"{Colors.RED}[WS] Error: {error}{Colors.END}")

def on_close(ws, close_status_code, close_msg):
    """Handle WebSocket close"""
    print(f"{Colors.YELLOW}[WS] Closed: {close_status_code} - {close_msg}{Colors.END}")

def on_open(ws, username):
    """Handle WebSocket connection open"""
    print(f"{Colors.GREEN}[WS] Connected as {username}{Colors.END}")
    
    # Send commands
    time.sleep(1)
    ws.send(f"PLAYER:CREATE:{username}")
    print(f"  Sent: PLAYER:CREATE:{username}")
    
    time.sleep(1)
    ws.send("PLAYER:DECK:DEFAULT")
    print(f"  Sent: PLAYER:DECK:DEFAULT")
    
    time.sleep(2)
    ws.send("MATCHMAKING:ENTER")
    print(f"  Sent: MATCHMAKING:ENTER")

def connect_player(ws_url, token, username):
    """Connect a player via WebSocket"""
    ws = websocket.WebSocketApp(
        ws_url,
        header=[f"Authorization: Bearer {token}"],
        on_message=on_message,
        on_error=on_error,
        on_close=on_close,
        on_open=lambda ws: on_open(ws, username)
    )
    
    ws.run_forever()

def main():
    print(f"{Colors.BLUE}╔══════════════════════════════════════════════════════════════╗{Colors.END}")
    print(f"{Colors.BLUE}║     Python WebSocket Test (Cross-Server)                    ║{Colors.END}")
    print(f"{Colors.BLUE}╚══════════════════════════════════════════════════════════════╝{Colors.END}\n")
    
    # Authenticate both users
    print(f"{Colors.YELLOW}═══ Step 1: Authentication ═══{Colors.END}")
    token_a = authenticate(SERVER1_URL, USER_A, PASSWORD)
    if not token_a:
        return 1
    
    token_b = authenticate(SERVER2_URL, USER_B, PASSWORD)
    if not token_b:
        return 1
    
    print()
    
    # Connect via WebSocket
    print(f"{Colors.YELLOW}═══ Step 2: WebSocket Connections ═══{Colors.END}")
    
    # Start player A in thread
    thread_a = threading.Thread(
        target=connect_player,
        args=(WS1_URL, token_a, USER_A)
    )
    thread_a.daemon = True
    thread_a.start()
    
    time.sleep(2)
    
    # Start player B in thread
    thread_b = threading.Thread(
        target=connect_player,
        args=(WS2_URL, token_b, USER_B)
    )
    thread_b.daemon = True
    thread_b.start()
    
    # Wait for connections and matchmaking
    print(f"\n{Colors.YELLOW}═══ Step 3: Waiting for Match (30s) ═══{Colors.END}")
    time.sleep(30)
    
    print(f"\n{Colors.GREEN}═══ Test completed ═══{Colors.END}")
    return 0

if __name__ == "__main__":
    try:
        exit(main())
    except KeyboardInterrupt:
        print(f"\n{Colors.YELLOW}Test interrupted by user{Colors.END}")
        exit(0)
    except Exception as e:
        print(f"{Colors.RED}Error: {e}{Colors.END}")
        exit(1)
