#!/bin/bash

SCRIPT_DIR=$(cd "$(dirname -- \"$0\")" && pwd)

# Main Menu Script for Dueling Protocol
# Provides easy access to all execution options

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
MAGENTA='\033[0;35m'
BOLD='\033[1m'
NC='\033[0m' # No Color

# Function to display the menu
show_menu() {
    clear
    echo -e "${CYAN}${BOLD}"
    echo "╔════════════════════════════════════════════════════════════════════╗"
    echo "║               🎮  DUELING PROTOCOL - CONTROL CENTER  🎮           ║"
    echo "╚════════════════════════════════════════════════════════════════════╝"
    echo -e "${NC}"
    echo

    echo -e "${BOLD}${YELLOW}┌─── 🎯 CRITICAL FEATURES ───────────────────────────────────────────┐${NC}"
    echo -e "${YELLOW}│${NC}  1.  Test PURCHASE (Compra de Pacotes)"
    echo -e "${YELLOW}│${NC}  2.  Test TRADE (Troca de Cartas)"
    echo -e "${YELLOW}│${NC}  3.  Test MATCHMAKING (Sistema de Partidas)"
    echo -e "${YELLOW}│${NC}  4.  Test ALL Critical Features"
    echo -e "${YELLOW}└────────────────────────────────────────────────────────────────────┘${NC}"
    echo

    echo -e "${BOLD}${MAGENTA}┌─── 🚀 AUTOMATED WORKFLOWS ────────────────────────────────────────┐${NC}"
    echo -e "${MAGENTA}│${NC}  50. ${GREEN}🚀 Full System Deploy${NC} (Build + Start + Verify)"
    echo -e "${MAGENTA}│${NC}  52. ${GREEN}🔄 Restart System${NC} (Down + Up + Auto Deploy)"
    echo -e "${MAGENTA}│${NC}  55. ${GREEN}🛠️  Fix & Rebuild${NC} (compile + build + restart)"
    echo -e "${MAGENTA}│${NC}  54. ${GREEN}📊 Quick Ledger Check${NC}"
    echo -e "${MAGENTA}│${NC}  51. 📋 Copy Deployment Info"
    echo -e "${MAGENTA}└────────────────────────────────────────────────────────────────────┘${NC}"
    echo

    echo -e "${BOLD}${CYAN}┌─── 🔧 MODULAR SERVICE STARTUP ───────────────────────────────────┐${NC}"
    echo -e "${CYAN}│${NC}  ${BOLD}Infrastructure:${NC}"
    echo -e "${CYAN}│${NC}    60. 🗄️  Start Redis Master"
    echo -e "${CYAN}│${NC}    61. 🗄️  Start Redis Slave"
    echo -e "${CYAN}│${NC}    62. 👁️  Start Redis Sentinels (Cluster)"
    echo -e "${CYAN}│${NC}    63. 🐘 Start PostgreSQL"
    echo -e "${CYAN}│${NC}    64. ⛓️  Start Blockchain Node"
    echo -e "${CYAN}│${NC}"
    echo -e "${CYAN}│${NC}  ${BOLD}Application Layer:${NC}"
    echo -e "${CYAN}│${NC}    65. 🌐 Start NGINX Load Balancer"
    echo -e "${CYAN}│${NC}    66. 🎮 Start Game Servers"
    echo -e "${CYAN}│${NC}    10. 👤 Run Client"
    echo -e "${CYAN}│${NC}    11. 🖥️  Run Additional Server"
    echo -e "${CYAN}└────────────────────────────────────────────────────────────────────┘${NC}"
    echo

    echo -e "${BOLD}${BLUE}┌─── 🔬 RESILIENCE & FAILOVER TESTS ──────────────────────────────┐${NC}"
    echo -e "${BLUE}│${NC}  70. Test NGINX Failover (Load Balancer)"
    echo -e "${BLUE}│${NC}  71. Test Redis Sentinel Failover"
    echo -e "${BLUE}│${NC}  72. ${BOLD}${GREEN}Validate Complete System${NC} (All Components)"
    echo -e "${BLUE}│${NC}  27. Test Redis Sentinel Status"
    echo -e "${BLUE}│${NC}  28. Test Redis Failover (Legacy)"
    echo -e "${BLUE}└────────────────────────────────────────────────────────────────────┘${NC}"
    echo

    echo -e "${BOLD}${GREEN}┌─── 📊 SYSTEM MANAGEMENT ──────────────────────────────────────────┐${NC}"
    echo -e "${GREEN}│${NC}  5.  Start Complete System (Docker Compose)"
    echo -e "${GREEN}│${NC}  6.  Start Game Local (Java + Docker)"
    echo -e "${GREEN}│${NC}  7.  Stop All Services"
    echo -e "${GREEN}│${NC}  8.  Build Project"
    echo -e "${GREEN}│${NC}  9.  System Status Check"
    echo -e "${GREEN}│${NC}  12. View Running Containers"
    echo -e "${GREEN}└────────────────────────────────────────────────────────────────────┘${NC}"
    echo

    echo -e "${BOLD}${YELLOW}┌─── 🧪 FUNCTIONAL TESTS ───────────────────────────────────────────┐${NC}"
    echo -e "${YELLOW}│${NC}  17-21: WebSocket, Protocol, Purchase, Match, Trade"
    echo -e "${YELLOW}│${NC}  22-26: X-Server(Trade/Match), Consistency, Disconnect, S2S"
    echo -e "${YELLOW}│${NC}  29-33: Concurrency, Distributed, Security Tests"
    echo -e "${YELLOW}└────────────────────────────────────────────────────────────────────┘${NC}"
    echo

    echo -e "${BOLD}${CYAN}┌─── 🔗 BLOCKCHAIN OPERATIONS ─────────────────────────────────────┐${NC}"
    echo -e "${CYAN}│${NC}  39-42: Start Node, Deploy, Verify Ledger, Simulate Tx"
    echo -e "${CYAN}│${NC}  43-46: Verify (Ownership, Uniqueness, Purchases, Matches)"
    echo -e "${CYAN}│${NC}  47-49: Complete Verification, Console, Tests"
    echo -e "${CYAN}└────────────────────────────────────────────────────────────────────┘${NC}"
    echo

    echo -e "${BOLD}${BLUE}┌─── 🌐 MULTI-PC DEPLOYMENT ───────────────────────────────────────┐${NC}"
    echo -e "${BLUE}│${NC}  13: Setup Distributed Environment"
    echo -e "${BLUE}│${NC}  14-16: Start Remote (Client, Gateway, Server)"
    echo -e "${BLUE}└────────────────────────────────────────────────────────────────────┘${NC}"
    echo

    echo -e "${BOLD}${MAGENTA}┌─── 📈 MONITORING & UTILITIES ────────────────────────────────────┐${NC}"
    echo -e "${MAGENTA}│${NC}  34-38: Monitor Logs, WS Status, Reset Stock, View Logs, Docs"
    echo -e "${MAGENTA}└────────────────────────────────────────────────────────────────────┘${NC}"
    echo
    
    echo -e "${BOLD}${RED}  0. Exit${NC}"
    echo
    echo -n "Choose an option: "
}

# Function to handle system status check
system_status() {
    echo -e "${BLUE}Checking system status...${NC}"
    if command -v docker &> /dev/null && docker ps &> /dev/null; then
        echo -e "${GREEN}Docker is running${NC}"
        echo "Active containers:"
        docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
    else
        echo -e "${YELLOW}Docker is not running or not installed${NC}"
    fi
    
    # Check for Java processes
    CLIENT_PIDS=$(pgrep -f "dueling-client-1.0-SNAPSHOT.jar" 2>/dev/null || true)
    SERVER_PIDS=$(pgrep -f "dueling-server-1.0-SNAPSHOT.jar" 2>/dev/null || true)
    GATEWAY_PIDS=$(pgrep -f "dueling-gateway-1.0-SNAPSHOT.jar" 2>/dev/null || true)
    
    if [ -n "$CLIENT_PIDS" ]; then
        echo -e "${GREEN}Client processes running:${NC} $CLIENT_PIDS"
    fi
    if [ -n "$SERVER_PIDS" ]; then
        echo -e "${GREEN}Server processes running:${NC} $SERVER_PIDS"
    fi
    if [ -n "$GATEWAY_PIDS" ]; then
        echo -e "${GREEN}Gateway processes running:${NC} $GATEWAY_PIDS"
    fi
    
    if [ -z "$CLIENT_PIDS" ] && [ -z "$SERVER_PIDS" ] && [ -z "$GATEWAY_PIDS" ] && [ "$(docker ps -q 2>/dev/null | wc -l)" -eq 0 ]; then
        echo -e "${YELLOW}No services appear to be running${NC}"
    fi
}

# Function to view documentation
view_docs() {
    echo -e "${BLUE}Available Documentation:${NC}"
    if [ -d "$SCRIPT_DIR/docs" ]; then
        for doc in "$SCRIPT_DIR"/docs/*.md;
 do
            if [ -f "$doc" ]; then
                echo " - $(basename "$doc")"
            fi
        done
        echo
        echo -e "${YELLOW}To view a document:${NC}"
        echo "  cat $SCRIPT_DIR/docs/DOCUMENT_NAME.md"
    else
        echo -e "${YELLOW}No documentation directory found${NC}"
    fi
}

# Function to view running containers
view_containers() {
    echo -e "${BLUE}Currently running containers:${NC}"
    docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" 2>/dev/null || echo "No Docker containers running or Docker not available"
}

# Main menu loop
while true; do
    show_menu
    read -r choice
    
    case $choice in
        1)
            echo -e "${GREEN}Testing PURCHASE (Compra de Pacotes)...${NC}"
            echo "Starting Docker services if needed..."
            (cd "$SCRIPT_DIR/docker" && docker compose up -d)
            sleep 10
            echo ""
            echo "Running PURCHASE test..."
            (cd "$SCRIPT_DIR/test_scripts" && TEST_FEATURE=PURCHASE node test_websocket_features.js)
            echo ""
            read -p "Press Enter to continue..."
            ;;
        2)
            echo -e "${GREEN}Testing TRADE (Troca de Cartas)...${NC}"
            echo "Starting Docker services if needed..."
            (cd "$SCRIPT_DIR/docker" && docker compose up -d)
            sleep 10
            echo ""
            echo "Running TRADE test..."
            (cd "$SCRIPT_DIR/test_scripts" && TEST_FEATURE=TRADE node test_websocket_features.js)
            echo ""
            read -p "Press Enter to continue..."
            ;;
        3)
            echo -e "${GREEN}Testing MATCHMAKING (Sistema de Partidas)...${NC}"
            echo "Starting Docker services if needed..."
            (cd "$SCRIPT_DIR/docker" && docker compose up -d)
            sleep 10
            echo ""
            echo "Running MATCHMAKING test..."
            (cd "$SCRIPT_DIR/test_scripts" && TEST_FEATURE=MATCHMAKING node test_websocket_features.js)
            echo ""
            read -p "Press Enter to continue..."
            ;;
        4)
            echo -e "${GREEN}Testing ALL Critical Features (PURCHASE + TRADE + MATCHMAKING)...${NC}"
            echo "Starting Docker services if needed..."
            (cd "$SCRIPT_DIR/docker" && docker compose up -d)
            sleep 10
            echo ""
            echo "Running ALL tests..."
            (cd "$SCRIPT_DIR/test_scripts" && node test_websocket_features.js)
            echo ""
            echo -e "${BLUE}════════════════════════════════════════${NC}"
            echo -e "${GREEN}Test completed!${NC}"
            echo -e "Check the summary above for results."
            echo -e "${BLUE}════════════════════════════════════════${NC}"
            read -p "Press Enter to continue..."
            ;;
        5)
            echo -e "${GREEN}Starting Complete System via Docker Compose...${NC}"
            echo -e "${YELLOW}This will build and run all services in Docker, including:${NC}"
            echo "  - Blockchain Node (Hardhat)"
            echo "  - PostgreSQL Database & Redis Sentinel Cluster"
            echo "  - NGINX Gateway & Multiple Game Servers"
            echo -e "${YELLOW}This may take 2-3 minutes on the first run...${NC}"
            echo ""
            bash "$SCRIPT_DIR/scripts/start-complete-with-blockchain.sh"
            read -p "Press Enter to continue..."
            ;;
        6)
            echo -e "${GREEN}Starting Game Local (Java + Docker for DB/Blockchain)...${NC}"
            echo -e "${YELLOW}This will start:${NC}"
            echo "  - Blockchain, PostgreSQL, and Redis (in Docker)"
            echo "  - A single Game Server (as a local Java process)"
            echo -e "${YELLOW}This may take 1-2 minutes...${NC}"
            echo ""
            bash "$SCRIPT_DIR/scripts/start-local-with-blockchain.sh"
            read -p "Press Enter to continue..."
            ;;
        7)
            echo -e "${GREEN}Stopping All Services (Java Processes and Docker Containers)...${NC}"
            bash "$SCRIPT_DIR/scripts/stop-all-with-blockchain.sh"
            read -p "Press Enter to continue..."
            ;;
        8)
            echo -e "${GREEN}Building Project...${NC}"
            (cd "$SCRIPT_DIR" && mvn clean package -DskipTests)
            read -p "Press Enter to continue..."
            ;;
        9)
            system_status
            echo ""
            echo "Blockchain Status:"
            if lsof -i:8545 > /dev/null 2>&1; then
                echo -e "   ${GREEN}✅ Blockchain Node: Running on http://localhost:8545${NC}"
                if [ -f "$SCRIPT_DIR/logs/blockchain.pid" ]; then
                    echo "   PID: $(cat "$SCRIPT_DIR/logs/blockchain.pid")"
                fi
            else
                echo -e "   ${YELLOW}⚠️  Blockchain Node: Not running${NC}"
                echo "   Start with: $SCRIPT_DIR/menu.sh → 39"
            fi
            read -p "Press Enter to continue..."
            ;;
        10)
            echo -e "${GREEN}Running Client...${NC}"
            bash "$SCRIPT_DIR/scripts/run_client.sh"
            read -p "Press Enter to continue..."
            ;;
        11)
            echo -e "${GREEN}Running Server...${NC}"
            bash "$SCRIPT_DIR/scripts/run_server.sh"
            read -p "Press Enter to continue..."
            ;;
        12)
            view_containers
            read -p "Press Enter to continue..."
            ;;
        13)
            echo -e "${GREEN}Setting up Distributed Environment...${NC}"
            bash "$SCRIPT_DIR/scripts/deploy/setup_distributed.sh"
            read -p "Press Enter to continue..."
            ;;
        14)
            echo -e "${GREEN}Starting Client Remote...${NC}"
            bash "$SCRIPT_DIR/scripts/deploy/start_client_remote.sh"
            read -p "Press Enter to continue..."
            ;;
        15)
            echo -e "${GREEN}Starting Gateway Remote...${NC}"
            bash "$SCRIPT_DIR/scripts/deploy/start_gateway_remote.sh"
            read -p "Press Enter to continue..."
            ;;
        16)
            echo -e "${GREEN}Starting Server Remote...${NC}"
            bash "$SCRIPT_DIR/scripts/deploy/start_server_remote.sh"
            read -p "Press Enter to continue..."
            ;;
        17)
            echo -e "${GREEN}Testing Client WebSocket...${NC}"
            bash "$SCRIPT_DIR/test_scripts/functional/test_client_websocket.sh"
            read -p "Press Enter to continue..."
            ;;
        18)
            echo -e "${GREEN}Testing Dueling Protocol...${NC}"
            bash "$SCRIPT_DIR/test_scripts/functional/test_dueling_protocol.sh"
            read -p "Press Enter to continue..."
            ;;
        19)
            echo -e "${GREEN}Testing Purchase...${NC}"
            bash "$SCRIPT_DIR/test_scripts/functional/test_purchase.sh"
            read -p "Press Enter to continue..."
            ;;
        20)
            echo -e "${GREEN}Testing Matchmaking...${NC}"
            bash "$SCRIPT_DIR/test_scripts/functional/test_matchmaking.sh"
            read -p "Press Enter to continue..."
            ;;
        21)
            echo -e "${GREEN}Testing Trade Functionality...${NC}"
            bash "$SCRIPT_DIR/test_scripts/functional/test_trade.sh"
            read -p "Press Enter to continue..."
            ;;
        22)
            echo -e "${GREEN}Testing Cross-Server Trade...${NC}"
            bash "$SCRIPT_DIR/test_scripts/functional/test_trade.sh"
            read -p "Press Enter to continue..."
            ;;
        23)
            echo -e "${GREEN}Testing Cross-Server Match...${NC}"
            bash "$SCRIPT_DIR/test_scripts/distributed/test_distributed_system.sh"
            read -p "Press Enter to continue..."
            ;;
        24)
            echo -e "${GREEN}Testing Game State Consistency...${NC}"
            bash "$SCRIPT_DIR/test_scripts/functional/test_game_state_consistency.sh"
            read -p "Press Enter to continue..."
            ;;
        25)
            echo -e "${GREEN}Testing Mid-Game Disconnection...${NC}"
            bash "$SCRIPT_DIR/test_scripts/functional/test_mid_game_disconnection.sh"
            read -p "Press Enter to continue..."
            ;;
        26)
            echo -e "${GREEN}Testing S2S Communication...${NC}"
            bash "$SCRIPT_DIR/test_scripts/functional/test_s2s_communication.sh"
            read -p "Press Enter to continue..."
            ;;
        27)
            echo -e "${GREEN}Testing Redis Sentinel...${NC}"
            bash "$SCRIPT_DIR/test_scripts/infrastructure/test_redis_sentinel.sh"
            read -p "Press Enter to continue..."
            ;;
        28)
            echo -e "${GREEN}Testing Redis Failover...${NC}"
            bash "$SCRIPT_DIR/test_scripts/infrastructure/test_redis_failover.sh"
            read -p "Press Enter to continue..."
            ;;
        29)
            echo -e "${GREEN}Testing Stock Concurrency...${NC}"
            bash "$SCRIPT_DIR/test_scripts/concurrency/test_stock_concurrency.sh"
            read -p "Press Enter to continue..."
            ;;
        30)
            echo -e "${GREEN}Testing Cross Server Matchmaking...${NC}"
            bash "$SCRIPT_DIR/test_scripts/distributed/test_distributed_matchmaking.sh"
            read -p "Press Enter to continue..."
            ;;
        31)
            echo -e "${GREEN}Testing Distributed Matchmaking...${NC}"
            bash "$SCRIPT_DIR/test_scripts/distributed/test_distributed_matchmaking.sh"
            read -p "Press Enter to continue..."
            ;;
        32)
            echo -e "${GREEN}Testing Advanced Security...${NC}"
            bash "$SCRIPT_DIR/test_scripts/security/test_advanced_security.sh"
            read -p "Press Enter to continue..."
            ;;
        33)
            echo -e "${GREEN}Running All Tests...${NC}"
            bash "$SCRIPT_DIR/test_scripts/run_all_tests.sh"
            read -p "Press Enter to continue..."
            ;;
        34)
            echo -e "${GREEN}Monitoring All Logs...${NC}"
            bash "$SCRIPT_DIR/scripts/monitor/monitor_logs.sh"
            read -p "Press Enter to continue..."
            ;;
        35)
            echo -e "${GREEN}Checking WebSocket Status...${NC}"
            bash "$SCRIPT_DIR/scripts/check_websocket_status.sh" 2>/dev/null || echo "WebSocket status script not found"
            read -p "Press Enter to continue..."
            ;;
        36)
            echo -e "${GREEN}Resetting Card Stock...${NC}"
            bash "$SCRIPT_DIR/scripts/reset_stock.sh" 2>/dev/null || echo "Reset stock script not found"
            read -p "Press Enter to continue..."
            ;;
        37)
            echo -e "${GREEN}Viewing Logs...${NC}"
            bash "$SCRIPT_DIR/test_scripts/functional/test_logs.sh" 2>/dev/null || docker logs $(docker ps -q) 2>/dev/null || echo "No logs available"
            read -p "Press Enter to continue..."
            ;;
        38)
            view_docs
            read -p "Press Enter to continue..."
            ;;
        39)
            echo -e "${GREEN}Starting Blockchain Node (Hardhat)...${NC}"
            
            # Check if already running
            if lsof -i:8545 > /dev/null 2>&1; then
                echo -e "${YELLOW}⚠️  Blockchain node is already running on port 8545${NC}"
                echo ""
                read -p "Stop and restart? (y/N): " restart_choice
                if [[ "$restart_choice" == "y" || "$restart_choice" == "Y" ]]; then
                    echo "Stopping existing node..."
                    pkill -f "hardhat node" 2>/dev/null || true
                    sleep 2
                else
                    read -p "Press Enter to continue..."
                    continue
                fi
            fi
            
            echo ""
            echo -e "${BLUE}This will start a local Ethereum node on http://localhost:8545${NC}"
            echo -e "${YELLOW}Keep this terminal open while using blockchain features.${NC}"
            echo -e "${YELLOW}Press Ctrl+C to stop the node.${NC}"
            echo ""
            
            # Check if node_modules exists
            if [ ! -d "$SCRIPT_DIR/dueling-blockchain/node_modules" ]; then
                echo "📦 Installing blockchain dependencies first..."
                (cd "$SCRIPT_DIR/dueling-blockchain" && npm install)
                echo ""
            fi
            
            # Create logs directory
            mkdir -p "$SCRIPT_DIR/logs"
            
            echo "Starting node... (logging to $SCRIPT_DIR/logs/blockchain-node.log)"
            (cd "$SCRIPT_DIR/dueling-blockchain" && npm run node 2>&1 | tee "$SCRIPT_DIR/logs/blockchain-node.log")
            read -p "Press Enter to continue..."
            ;;
        40)
            echo -e "${GREEN}Deploying Blockchain Contracts...${NC}"
            
            # Check if blockchain node is running
            if ! lsof -i:8545 > /dev/null 2>&1; then
                echo -e "${RED}❌ Blockchain node is NOT running!${NC}"
                echo ""
                echo "You need to start the blockchain node first:"
                echo "  1. Open a NEW terminal"
                echo "  2. Run: $SCRIPT_DIR/menu.sh"
                echo "  3. Select option 39 (Start Blockchain Node)"
                echo "  4. Keep that terminal open"
                echo "  5. Return here and try again"
                echo ""
                read -p "Press Enter to continue..."
                continue
            fi
            
            echo -e "${YELLOW}Make sure the blockchain node is running (option 39)${NC}"
            echo ""
            
            # Create logs directory
            mkdir -p "$SCRIPT_DIR/logs"
            
            (cd "$SCRIPT_DIR/dueling-blockchain" && npm run deploy:local 2>&1 | tee "$SCRIPT_DIR/logs/blockchain-deploy.log")
            
            echo ""
            echo -e "${GREEN}✅ Contracts deployed successfully!${NC}"
            echo ""
            echo "Contract addresses have been saved."
            echo "You can now:"
            echo "  - Run simulations (option 41)"
            echo "  - Verify cards (options 42-46)"
            echo ""
            read -p "Press Enter to continue..."
            ;;
        41)
            echo -e "${GREEN}Verifying Distributed Ledger...${NC}"
            bash "$SCRIPT_DIR/scripts/verify_blockchain_ledger.sh"
            read -p "Press Enter to continue..."
            ;;
        42)
            echo -e "${GREEN}Simulating Blockchain Transactions...${NC}"
            
            # Check if blockchain node is running
            if ! lsof -i:8545 > /dev/null 2>&1; then
                echo -e "${RED}❌ Blockchain node is NOT running!${NC}"
                echo "Start it with: $SCRIPT_DIR/menu.sh → 39"
                read -p "Press Enter to continue..."
                continue
            fi
            
            (cd "$SCRIPT_DIR/dueling-blockchain" && npm run simulate)
            read -p "Press Enter to continue..."
            ;;
        43)
            echo -e "${GREEN}Verifying Card Ownership...${NC}"
            
            # Check if blockchain node is running
            if ! lsof -i:8545 > /dev/null 2>&1; then
                echo -e "${RED}❌ Blockchain node is NOT running!${NC}"
                echo "Start it with: $SCRIPT_DIR/menu.sh → 39"
                read -p "Press Enter to continue..."
                continue
            fi
            
            echo ""
            read -p "Enter player address (or press Enter for default): " player_addr
            if [ -z "$player_addr" ]; then
                (cd "$SCRIPT_DIR/dueling-blockchain" && npm run verify:ownership)
            else
                (cd "$SCRIPT_DIR/dueling-blockchain" && PLAYER_ADDRESS="$player_addr" npm run verify:ownership)
            fi
            read -p "Press Enter to continue..."
            ;;
        44)
            echo -e "${GREEN}Verifying Card Uniqueness...${NC}"
            
            # Check if blockchain node is running
            if ! lsof -i:8545 > /dev/null 2>&1; then
                echo -e "${RED}❌ Blockchain node is NOT running!${NC}"
                echo "Start it with: $SCRIPT_DIR/menu.sh → 39"
                read -p "Press Enter to continue..."
                continue
            fi
            
            (cd "$SCRIPT_DIR/dueling-blockchain" && npm run verify:uniqueness)
            read -p "Press Enter to continue..."
            ;;
        45)
            echo -e "${GREEN}Viewing Purchase History...${NC}"
            
            # Check if blockchain node is running
            if ! lsof -i:8545 > /dev/null 2>&1; then
                echo -e "${RED}❌ Blockchain node is NOT running!${NC}"
                echo "Start it with: $SCRIPT_DIR/menu.sh → 39"
                read -p "Press Enter to continue..."
                continue
            fi
            
            echo ""
            read -p "Enter player address (or press Enter for default): " player_addr
            if [ -z "$player_addr" ]; then
                (cd "$SCRIPT_DIR/dueling-blockchain" && npm run verify:purchases)
            else
                (cd "$SCRIPT_DIR/dueling-blockchain" && PLAYER_ADDRESS="$player_addr" npm run verify:purchases)
            fi
            read -p "Press Enter to continue..."
            ;;
        46)
            echo -e "${GREEN}Viewing Match Results (Blockchain)...${NC}"
            
            # Check if blockchain node is running
            if ! lsof -i:8545 > /dev/null 2>&1; then
                echo -e "${RED}❌ Blockchain node is NOT running!${NC}"
                echo "Start it with: $SCRIPT_DIR/menu.sh → 39"
                read -p "Press Enter to continue..."
                continue
            fi
            
            echo ""
            read -p "Enter player address (or press Enter for default): " player_addr
            if [ -z "$player_addr" ]; then
                (cd "$SCRIPT_DIR/dueling-blockchain" && npm run verify:matches)
            else
                (cd "$SCRIPT_DIR/dueling-blockchain" && PLAYER_ADDRESS="$player_addr" npm run verify:matches)
            fi
            read -p "Press Enter to continue..."
            ;;
        47)
            echo -e "${GREEN}Complete Blockchain Verification (All 3)...${NC}"
            
            # Check if blockchain node is running
            if ! lsof -i:8545 > /dev/null 2>&1; then
                echo -e "${RED}❌ Blockchain node is NOT running!${NC}"
                echo ""
                echo "You need to:"
                echo "   Start with: $SCRIPT_DIR/menu.sh → 39"
                read -p "Press Enter to continue..."
                continue
            fi
            
            echo ""
            read -p "Enter player address (or press Enter for default): " player_addr
            echo ""
            echo -e "${BLUE}════════════════════════════════════════${NC}"
            echo -e "${BLUE}1/3: Verifying Card Ownership${NC}"
            echo -e "${BLUE}════════════════════════════════════════${NC}"
            if [ -z "$player_addr" ]; then
                (cd "$SCRIPT_DIR/dueling-blockchain" && npm run verify:ownership)
            else
                (cd "$SCRIPT_DIR/dueling-blockchain" && PLAYER_ADDRESS="$player_addr" npm run verify:ownership)
            fi
            echo ""
            echo -e "${BLUE}════════════════════════════════════════${NC}"
            echo -e "${BLUE}2/3: Viewing Purchase History${NC}"
            echo -e "${BLUE}════════════════════════════════════════${NC}"
            if [ -z "$player_addr" ]; then
                (cd "$SCRIPT_DIR/dueling-blockchain" && npm run verify:purchases)
            else
                (cd "$SCRIPT_DIR/dueling-blockchain" && PLAYER_ADDRESS="$player_addr" npm run verify:purchases)
            fi
            echo ""
            echo -e "${BLUE}════════════════════════════════════════${NC}"
            echo -e "${BLUE}3/3: Viewing Match Results${NC}"
            echo -e "${BLUE}════════════════════════════════════════${NC}"
            if [ -z "$player_addr" ]; then
                (cd "$SCRIPT_DIR/dueling-blockchain" && npm run verify:matches)
            else
                (cd "$SCRIPT_DIR/dueling-blockchain" && PLAYER_ADDRESS="$player_addr" npm run verify:matches)
            fi
            echo ""
            echo -e "${GREEN}════════════════════════════════════════${NC}"
            echo -e "${GREEN}Complete Verification Finished!${NC}"
            echo -e "${GREEN}════════════════════════════════════════${NC}"
            read -p "Press Enter to continue..."
            ;;
        48)
            echo -e "${GREEN}Opening Blockchain Console...${NC}"
            echo -e "${YELLOW}Type 'exit' or Ctrl+D to exit console${NC}"
            (cd "$SCRIPT_DIR/dueling-blockchain" && npx hardhat console --network localhost)
            read -p "Press Enter to continue..."
            ;;
        49)
            echo -e "${GREEN}Running Blockchain Tests...${NC}"
            (cd "$SCRIPT_DIR/dueling-blockchain" && npm test)
            read -p "Press Enter to continue..."
            ;;
        50)
            echo -e "${BLUE}╔════════════════════════════════════════════════════════════════╗${NC}"
            echo -e "${BLUE}║        🚀 FULL SYSTEM DEPLOY - Automated Workflow             ║${NC}"
            echo -e "${BLUE}╚════════════════════════════════════════════════════════════════╝${NC}"
            echo ""
            
            echo -e "${GREEN}Step 1/4: Building project...${NC}"
            (cd "$SCRIPT_DIR" && mvn clean package -DskipTests)
            
            echo ""
            echo -e "${GREEN}Step 2/4: Building Docker images...${NC}"
            (cd "$SCRIPT_DIR/docker" && docker compose build)
            
            echo ""
            echo -e "${GREEN}Step 3/4: Starting services...${NC}"
            (cd "$SCRIPT_DIR/docker" && docker compose down && docker compose up -d)
            
            echo ""
            echo -e "${GREEN}Step 4/4: Waiting for blockchain deploy (45 seconds)...${NC}"
            sleep 45
            
            echo ""
            echo -e "${BLUE}Verifying deployment...${NC}"
            docker logs dueling-blockchain 2>&1 | grep "DEPLOYMENT COMPLETE" || echo -e "${RED}⚠️  Check blockchain logs${NC}"
            docker logs server-1 2>&1 | grep "Contract addresses loaded" || echo -e "${RED}⚠️  Check server logs${NC}"
            
            echo ""
            echo -e "${GREEN}╔════════════════════════════════════════════════════════════════╗${NC}"
            echo -e "${GREEN}║                    ✅ DEPLOY COMPLETE!                         ║${NC}"
            echo -e "${GREEN}╚════════════════════════════════════════════════════════════════╝${NC}"
            echo ""
            echo "System is ready! You can now:"
            echo "  • Run clients: $SCRIPT_DIR/menu.sh → 10"
            echo "  • Verify ledger: $SCRIPT_DIR/menu.sh → 54"
            echo ""
            read -p "Press Enter to continue..."
            ;;
        51)
            echo -e "${GREEN}📋 Copying Deployment Info from Blockchain Container...${NC}"
            echo ""
            
            if ! docker ps | grep -q "dueling-blockchain"; then
                echo -e "${RED}❌ Blockchain container is not running!${NC}"
                echo "Start system first: $SCRIPT_DIR/menu.sh → 5"
                read -p "Press Enter to continue..."
                continue
            fi
            
            docker exec dueling-blockchain cat /usr/src/app/deployment-info.json > "$SCRIPT_DIR/dueling-blockchain/deployment-info.json"
            
            if [ $? -eq 0 ]; then
                echo -e "${GREEN}✅ Deployment info copied successfully!${NC}"
                echo ""
                echo "File location: $SCRIPT_DIR/dueling-blockchain/deployment-info.json"
                echo ""
                cat "$SCRIPT_DIR/dueling-blockchain/deployment-info.json" | python3 -m json.tool 2>/dev/null || \
                    cat "$SCRIPT_DIR/dueling-blockchain/deployment-info.json"
            else
                echo -e "${RED}❌ Failed to copy deployment info${NC}"
            fi
            
            echo ""
            read -p "Press Enter to continue..."
            ;;
        52)
            echo -e "${BLUE}╔════════════════════════════════════════════════════════════════╗${NC}"
            echo -e "${BLUE}║           🔄 RESTART SYSTEM - Automated Workflow              ║${NC}"
            echo -e "${BLUE}╚════════════════════════════════════════════════════════════════╝${NC}"
            echo ""
            
            echo -e "${GREEN}Step 1/3: Stopping all services...${NC}"
            (cd "$SCRIPT_DIR/docker" && docker compose down)
            
            echo ""
            echo -e "${GREEN}Step 2/3: Starting services...${NC}"
            (cd "$SCRIPT_DIR/docker" && docker compose up -d)
            
            echo ""
            echo -e "${GREEN}Step 3/3: Waiting for auto deploy (45 seconds)...${NC}"
            sleep 45
            
            echo ""
            echo -e "${BLUE}Verifying...${NC}"
            docker logs dueling-blockchain 2>&1 | tail -20 | grep "DEPLOYMENT COMPLETE" && \
                echo -e "${GREEN}✅ Blockchain deployed${NC}" || \
                echo -e "${RED}⚠️  Check: docker logs dueling-blockchain${NC}"
            
            docker logs server-1 2>&1 | tail -20 | grep "Contract addresses loaded" && \
                echo -e "${GREEN}✅ Servers loaded addresses${NC}" || \
                echo -e "${RED}⚠️  Check: docker logs server-1${NC}"
            
            echo ""
            echo -e "${GREEN}╔════════════════════════════════════════════════════════════════╗${NC}"
            echo -e "${GREEN}║                  ✅ RESTART COMPLETE!                          ║${NC}"
            echo -e "${GREEN}╚════════════════════════════════════════════════════════════════╝${NC}"
            read -p "Press Enter to continue..."
            ;;
        53)
            echo -e "${BLUE}╔════════════════════════════════════════════════════════════════╗${NC}"
            echo -e "${BLUE}║      🧪 AUTO TEST FULL FLOW - Requires Manual Input           ║${NC}"
            echo -e "${BLUE}╚════════════════════════════════════════════════════════════════╝${NC}"
            echo ""
            echo -e "${YELLOW}This will guide you through testing the complete flow:${NC}"
            echo "  1. Open 2 terminals"
            echo "  2. Register 2 users"
            echo "  3. Buy packs"
            echo "  4. Trade cards"
            echo "  5. Play a match"
            echo "  6. Verify ledger"
            echo ""
            echo -e "${GREEN}Instructions saved in: $SCRIPT_DIR/FINAL_STATUS.md${NC}"
            echo ""
            echo -e "${BLUE}Opening guide...${NC}"
            cat "$SCRIPT_DIR/FINAL_STATUS.md" | less
            read -p "Press Enter to continue..."
            ;;
        54)
            echo -e "${BLUE}╔════════════════════════════════════════════════════════════════╗${NC}"
            echo -e "${BLUE}║          📊 QUICK LEDGER CHECK - Automated                     ║${NC}"
            echo -e "${BLUE}╚════════════════════════════════════════════════════════════════╝${NC}"
            echo ""
            
            if ! docker ps | grep -q "dueling-blockchain"; then
                echo -e "${RED}❌ Blockchain is not running!${NC}"
                echo "Start system: $SCRIPT_DIR/menu.sh → 5"
                read -p "Press Enter to continue..."
                continue
            fi
            
            echo -e "${GREEN}Step 1/2: Copying deployment info...${NC}"
            docker exec dueling-blockchain cat /usr/src/app/deployment-info.json > \
                "$SCRIPT_DIR/dueling-blockchain/deployment-info.json" 2>/dev/null
            
            if [ $? -eq 0 ]; then
                echo -e "${GREEN}✅ Deployment info updated${NC}"
            else
                echo -e "${RED}⚠️  Using cached deployment info${NC}"
            fi
            
            echo ""
            echo -e "${GREEN}Step 2/2: Verifying ledger...${NC}"
            echo ""
            bash "$SCRIPT_DIR/scripts/verify_blockchain_ledger.sh"
            
            echo ""
            read -p "Press Enter to continue..."
            ;;
        55)
            echo -e "${BLUE}╔════════════════════════════════════════════════════════════════╗${NC}"
            echo -e "${BLUE}║         🛠️  FIX & REBUILD - Complete Rebuild Workflow         ║${NC}"
            echo -e "${BLUE}╚════════════════════════════════════════════════════════════════╝${NC}"
            echo ""
            
            echo -e "${GREEN}Step 1/5: Maven clean compile...${NC}"
            (cd "$SCRIPT_DIR" && mvn clean compile)
            
            echo ""
            echo -e "${GREEN}Step 2/5: Maven package...${NC}"
            (cd "$SCRIPT_DIR" && mvn package -DskipTests)
            
            echo ""
            echo -e "${GREEN}Step 3/5: Docker build...${NC}"
            (cd "$SCRIPT_DIR/docker" && docker compose build)
            
            echo ""
            echo -e "${GREEN}Step 4/5: Restart system...${NC}"
            (cd "$SCRIPT_DIR/docker" && docker compose down && docker compose up -d)
            
            echo ""
            echo -e "${GREEN}Step 5/5: Waiting for deploy (45s)...${NC}"
            sleep 45
            
            echo ""
            echo -e "${BLUE}Verification:${NC}"
            docker logs dueling-blockchain 2>&1 | grep "DEPLOYMENT COMPLETE" && \
                echo -e "${GREEN}✅ Blockchain OK${NC}" || \
                echo -e "${RED}⚠️  Blockchain issue${NC}"
            
            docker logs server-1 2>&1 | grep "Contract addresses loaded" && \
                echo -e "${GREEN}✅ Server OK${NC}" || \
                echo -e "${RED}⚠️  Server issue${NC}"
            
            echo ""
            echo -e "${GREEN}╔════════════════════════════════════════════════════════════════╗${NC}"
            echo -e "${GREEN}║                 ✅ REBUILD COMPLETE!                           ║${NC}"
            echo -e "${GREEN}╚════════════════════════════════════════════════════════════════╝${NC}"
            read -p "Press Enter to continue..."
            ;;
        60)
            echo -e "${GREEN}Starting Redis Master...${NC}"
            bash "$SCRIPT_DIR/scripts/run/start_redis_master.sh"
            read -p "Press Enter to continue..."
            ;;
        61)
            echo -e "${GREEN}Starting Redis Slave...${NC}"
            bash "$SCRIPT_DIR/scripts/run/start_redis_slave.sh"
            read -p "Press Enter to continue..."
            ;;
        62)
            echo -e "${GREEN}Starting Redis Sentinels...${NC}"
            bash "$SCRIPT_DIR/scripts/run/start_redis_sentinels.sh"
            read -p "Press Enter to continue..."
            ;;
        63)
            echo -e "${GREEN}Starting PostgreSQL...${NC}"
            bash "$SCRIPT_DIR/scripts/run/start_postgresql.sh"
            read -p "Press Enter to continue..."
            ;;
        64)
            echo -e "${GREEN}Starting Blockchain Node...${NC}"
            bash "$SCRIPT_DIR/scripts/run/start_blockchain.sh"
            read -p "Press Enter to continue..."
            ;;
        65)
            echo -e "${GREEN}Starting NGINX Load Balancer...${NC}"
            bash "$SCRIPT_DIR/scripts/run/start_nginx.sh"
            read -p "Press Enter to continue..."
            ;;
        66)
            echo -e "${GREEN}Starting Game Servers...${NC}"
            bash "$SCRIPT_DIR/scripts/run/start_servers.sh"
            read -p "Press Enter to continue..."
            ;;
        70)
            echo -e "${GREEN}Testing NGINX Failover...${NC}"
            bash "$SCRIPT_DIR/scripts/test_nginx_failover.sh"
            read -p "Press Enter to continue..."
            ;;
        71)
            echo -e "${GREEN}Testing Redis Sentinel Failover...${NC}"
            bash "$SCRIPT_DIR/scripts/test_redis_failover.sh"
            read -p "Press Enter to continue..."
            ;;
        72)
            echo -e "${GREEN}Validating Complete System...${NC}"
            bash "$SCRIPT_DIR/scripts/validate_system.sh"
            read -p "Press Enter to continue..."
            ;;
        73)
            echo -e "${GREEN}Testing Redis Reconnection After Failover...${NC}"
            bash "$SCRIPT_DIR/scripts/test_redis_reconnection.sh"
            read -p "Press Enter to continue..."
            ;;
        0)
            echo -e "${GREEN}Thank you for using Dueling Protocol!${NC}"
            exit 0
            ;;
        *)
            echo -e "${RED}Invalid option. Please try again.${NC}"
            read -p "Press Enter to continue..."
            ;;
    esac
done