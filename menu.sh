#!/bin/bash

# Main Menu Script for Dueling Protocol
# Provides easy access to all execution options

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to display the menu
show_menu() {
    clear
    echo -e "${BLUE}"
    echo "╔══════════════════════════════════════════════════════════════╗"
    echo "║                Dueling Protocol - Main Menu                  ║"
    echo "╚══════════════════════════════════════════════════════════════╝"
    echo
    echo -e "${YELLOW}🎯 CRITICAL FEATURES (Main Game Functions):${NC}"
    echo -e "1.  ${GREEN}Test PURCHASE (Compra de Pacotes)${NC}"
    echo -e "2.  ${GREEN}Test TRADE (Troca de Cartas)${NC}"
    echo -e "3.  ${GREEN}Test MATCHMAKING (Sistema de Partidas)${NC}"
    echo -e "4.  ${GREEN}Test ALL Critical Features (PURCHASE + TRADE + MATCHMAKING)${NC}"
    echo
    echo -e "${YELLOW}System Management:${NC}"
    echo -e "5.  Start Complete System (Docker Compose) ${GREEN}[Multi-PC Ready]${NC}"
    echo -e "6.  Start Game Local (Java + Docker DB/Blockchain) ${GREEN}[Single PC]${NC}"
    echo -e "7.  Stop All Services (Java + Docker)"
    echo -e "8.  Build Project"
    echo -e "9.  System Status Check"
    echo
    echo -e "${YELLOW}Run Client & Server (Docker Integrated):${NC}"
    echo "10. Run Client (Docker) ${GREEN}[Requires system running - option 5 or 6]${NC}"
    echo "11. Run Server (Docker) ${GREEN}[Requires system running - option 5 or 6]${NC}"
    echo "12. View Running Containers"
    echo
    echo -e "${YELLOW}Network/Multi-PC Deployment:${NC}"
    echo "13. Setup Distributed Environment"
    echo "14. Start Client Remote"
    echo "15. Start Gateway Remote"
    echo "16. Start Server Remote"
    echo
    echo -e "${YELLOW}Functional Tests:${NC}"
    echo "17. Test Client WebSocket"
    echo "18. Test Dueling Protocol"
    echo "19. Test Purchase (Shell)"
    echo "20. Test Matchmaking (Shell)"
    echo "21. Test Trade Functionality"
    echo "22. Test Cross-Server Trade"
    echo "23. Test Cross-Server Match"
    echo "24. Test Game State Consistency"
    echo "25. Test Mid-Game Disconnection"
    echo "26. Test S2S Communication"
    echo
    echo -e "${YELLOW}Advanced Tests:${NC}"
    echo "27. Test Redis Sentinel"
    echo "28. Test Redis Failover"
    echo "29. Test Stock Concurrency"
    echo "30. Test Cross Server Matchmaking"
    echo "31. Test Distributed Matchmaking"
    echo "32. Test Advanced Security"
    echo "33. Run All Tests"
    echo
    echo -e "${YELLOW}Monitoring & Utilities:${NC}"
    echo "34. Monitor All Logs"
    echo "35. Check WebSocket Status"
    echo "36. Reset Card Stock"
    echo "37. View Logs"
    echo "38. View Documentation"
    echo
    echo -e "${YELLOW}🔗 Blockchain Verification:${NC}"
    echo "39. Start Blockchain Node (Hardhat)"
    echo "40. Deploy Blockchain Contracts"
    echo "41. ${GREEN}Verify Distributed Ledger (Cards/Trades/Matches)${NC}"
    echo "42. Simulate Blockchain Transactions"
    echo "43. Verify Card Ownership"
    echo "44. Verify Card Uniqueness"
    echo "45. View Purchase History"
    echo "46. View Match Results (Blockchain)"
    echo "47. Complete Verification (All 3)"
    echo "48. Blockchain Console"
    echo "49. Run Blockchain Tests"
    echo
    echo -e "${YELLOW}🤖 Automated Workflows:${NC}"
    echo -e "50. ${GREEN}🚀 Full System Deploy (Build + Start + Verify)${NC}"
    echo -e "51. ${GREEN}📋 Copy Deployment Info (for ledger verification)${NC}"
    echo -e "52. ${GREEN}🔄 Restart System (Down + Up + Auto Deploy)${NC}"
    echo -e "53. ${GREEN}🧪 Auto Test Full Flow (2 players + purchases + trade + match)${NC}"
    echo -e "54. ${GREEN}📊 Quick Ledger Check (with auto copy deployment-info)${NC}"
    echo -e "55. ${GREEN}🛠️  Fix & Rebuild (compile + build + restart)${NC}"
    echo
    echo -e "${RED}0. Exit${NC}"
    echo
    echo -n "Choose an option (0-55): "
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
    if [ -d "docs" ]; then
        for doc in docs/*.md; do
            if [ -f "$doc" ]; then
                echo " - $(basename "$doc")"
            fi
        done
        echo
        echo -e "${YELLOW}To view a document:${NC}"
        echo "  cat docs/DOCUMENT_NAME.md"
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
            cd docker && docker compose up -d && cd ..
            sleep 10
            echo ""
            echo "Running PURCHASE test..."
            cd test_scripts && TEST_FEATURE=PURCHASE node test_websocket_features.js && cd ..
            echo ""
            read -p "Press Enter to continue..."
            ;;
        2)
            echo -e "${GREEN}Testing TRADE (Troca de Cartas)...${NC}"
            echo "Starting Docker services if needed..."
            cd docker && docker compose up -d && cd ..
            sleep 10
            echo ""
            echo "Running TRADE test..."
            cd test_scripts && TEST_FEATURE=TRADE node test_websocket_features.js && cd ..
            echo ""
            read -p "Press Enter to continue..."
            ;;
        3)
            echo -e "${GREEN}Testing MATCHMAKING (Sistema de Partidas)...${NC}"
            echo "Starting Docker services if needed..."
            cd docker && docker compose up -d && cd ..
            sleep 10
            echo ""
            echo "Running MATCHMAKING test..."
            cd test_scripts && TEST_FEATURE=MATCHMAKING node test_websocket_features.js && cd ..
            echo ""
            read -p "Press Enter to continue..."
            ;;
        4)
            echo -e "${GREEN}Testing ALL Critical Features (PURCHASE + TRADE + MATCHMAKING)...${NC}"
            echo "Starting Docker services if needed..."
            cd docker && docker compose up -d && cd ..
            sleep 10
            echo ""
            echo "Running ALL tests..."
            cd test_scripts && node test_websocket_features.js && cd ..
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
            bash ./scripts/start-complete-with-blockchain.sh
            read -p "Press Enter to continue..."
            ;;
        6)
            echo -e "${GREEN}Starting Game Local (Java + Docker for DB/Blockchain)...${NC}"
            echo -e "${YELLOW}This will start:${NC}"
            echo "  - Blockchain, PostgreSQL, and Redis (in Docker)"
            echo "  - A single Game Server (as a local Java process)"
            echo -e "${YELLOW}This may take 1-2 minutes...${NC}"
            echo ""
            bash ./scripts/start-local-with-blockchain.sh
            read -p "Press Enter to continue..."
            ;;
        7)
            echo -e "${GREEN}Stopping All Services (Java Processes and Docker Containers)...${NC}"
            bash ./scripts/stop-all-with-blockchain.sh
            read -p "Press Enter to continue..."
            ;;
        8)
            echo -e "${GREEN}Building Project...${NC}"
            mvn clean package -DskipTests
            read -p "Press Enter to continue..."
            ;;
        9)
            system_status
            echo ""
            echo "Blockchain Status:"
            if lsof -i:8545 > /dev/null 2>&1; then
                echo -e "   ${GREEN}✅ Blockchain Node: Running on http://localhost:8545${NC}"
                if [ -f logs/blockchain.pid ]; then
                    echo "   PID: $(cat logs/blockchain.pid)"
                fi
            else
                echo -e "   ${YELLOW}⚠️  Blockchain Node: Not running${NC}"
                echo "   Start with: ./menu.sh → 39"
            fi
            read -p "Press Enter to continue..."
            ;;
        10)
            echo -e "${GREEN}Running Client...${NC}"
            bash ./scripts/run_client.sh
            read -p "Press Enter to continue..."
            ;;
        11)
            echo -e "${GREEN}Running Server...${NC}"
            bash ./scripts/run_server.sh
            read -p "Press Enter to continue..."
            ;;
        12)
            view_containers
            read -p "Press Enter to continue..."
            ;;
        13)
            echo -e "${GREEN}Setting up Distributed Environment...${NC}"
            bash ./scripts/deploy/setup_distributed.sh
            read -p "Press Enter to continue..."
            ;;
        14)
            echo -e "${GREEN}Starting Client Remote...${NC}"
            bash ./scripts/deploy/start_client_remote.sh
            read -p "Press Enter to continue..."
            ;;
        15)
            echo -e "${GREEN}Starting Gateway Remote...${NC}"
            bash ./scripts/deploy/start_gateway_remote.sh
            read -p "Press Enter to continue..."
            ;;
        16)
            echo -e "${GREEN}Starting Server Remote...${NC}"
            bash ./scripts/deploy/start_server_remote.sh
            read -p "Press Enter to continue..."
            ;;
        17)
            echo -e "${GREEN}Testing Client WebSocket...${NC}"
            bash ./test_scripts/functional/test_client_websocket.sh
            read -p "Press Enter to continue..."
            ;;
        18)
            echo -e "${GREEN}Testing Dueling Protocol...${NC}"
            bash ./test_scripts/functional/test_dueling_protocol.sh
            read -p "Press Enter to continue..."
            ;;
        19)
            echo -e "${GREEN}Testing Purchase...${NC}"
            bash ./test_scripts/functional/test_purchase.sh
            read -p "Press Enter to continue..."
            ;;
        20)
            echo -e "${GREEN}Testing Matchmaking...${NC}"
            bash ./test_scripts/functional/test_matchmaking.sh
            read -p "Press Enter to continue..."
            ;;
        21)
            echo -e "${GREEN}Testing Trade Functionality...${NC}"
            bash ./test_scripts/functional/test_trade.sh
            read -p "Press Enter to continue..."
            ;;
        22)
            echo -e "${GREEN}Testing Cross-Server Trade...${NC}"
            bash ./test_scripts/functional/test_trade.sh
            read -p "Press Enter to continue..."
            ;;
        23)
            echo -e "${GREEN}Testing Cross-Server Match...${NC}"
            bash ./test_scripts/distributed/test_distributed_system.sh
            read -p "Press Enter to continue..."
            ;;
        24)
            echo -e "${GREEN}Testing Game State Consistency...${NC}"
            bash ./test_scripts/functional/test_game_state_consistency.sh
            read -p "Press Enter to continue..."
            ;;
        25)
            echo -e "${GREEN}Testing Mid-Game Disconnection...${NC}"
            bash ./test_scripts/functional/test_mid_game_disconnection.sh
            read -p "Press Enter to continue..."
            ;;
        26)
            echo -e "${GREEN}Testing S2S Communication...${NC}"
            bash ./test_scripts/functional/test_s2s_communication.sh
            read -p "Press Enter to continue..."
            ;;
        27)
            echo -e "${GREEN}Testing Redis Sentinel...${NC}"
            bash ./test_scripts/infrastructure/test_redis_sentinel.sh
            read -p "Press Enter to continue..."
            ;;
        28)
            echo -e "${GREEN}Testing Redis Failover...${NC}"
            bash ./test_scripts/infrastructure/test_redis_failover.sh
            read -p "Press Enter to continue..."
            ;;
        29)
            echo -e "${GREEN}Testing Stock Concurrency...${NC}"
            bash ./test_scripts/concurrency/test_stock_concurrency.sh
            read -p "Press Enter to continue..."
            ;;
        30)
            echo -e "${GREEN}Testing Cross Server Matchmaking...${NC}"
            bash ./test_scripts/distributed/test_distributed_matchmaking.sh
            read -p "Press Enter to continue..."
            ;;
        31)
            echo -e "${GREEN}Testing Distributed Matchmaking...${NC}"
            bash ./test_scripts/distributed/test_distributed_matchmaking.sh
            read -p "Press Enter to continue..."
            ;;
        32)
            echo -e "${GREEN}Testing Advanced Security...${NC}"
            bash ./test_scripts/security/test_advanced_security.sh
            read -p "Press Enter to continue..."
            ;;
        33)
            echo -e "${GREEN}Running All Tests...${NC}"
            bash ./test_scripts/run_all_tests.sh
            read -p "Press Enter to continue..."
            ;;
        34)
            echo -e "${GREEN}Monitoring All Logs...${NC}"
            bash ./scripts/monitor/monitor_logs.sh
            read -p "Press Enter to continue..."
            ;;
        35)
            echo -e "${GREEN}Checking WebSocket Status...${NC}"
            bash ./scripts/check_websocket_status.sh 2>/dev/null || echo "WebSocket status script not found"
            read -p "Press Enter to continue..."
            ;;
        36)
            echo -e "${GREEN}Resetting Card Stock...${NC}"
            bash ./scripts/reset_stock.sh 2>/dev/null || echo "Reset stock script not found"
            read -p "Press Enter to continue..."
            ;;
        37)
            echo -e "${GREEN}Viewing Logs...${NC}"
            bash ./test_scripts/functional/test_logs.sh 2>/dev/null || docker logs $(docker ps -q) 2>/dev/null || echo "No logs available"
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
            if [ ! -d "dueling-blockchain/node_modules" ]; then
                echo "📦 Installing blockchain dependencies first..."
                cd dueling-blockchain && npm install && cd ..
                echo ""
            fi
            
            # Create logs directory
            mkdir -p logs
            
            echo "Starting node... (logging to logs/blockchain-node.log)"
            cd dueling-blockchain && npm run node 2>&1 | tee ../logs/blockchain-node.log
            cd ..
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
                echo "  2. Run: ./menu.sh"
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
            mkdir -p logs
            
            cd dueling-blockchain && npm run deploy:local 2>&1 | tee ../logs/blockchain-deploy.log && cd ..
            
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
            echo -e "${GREEN}Simulating Blockchain Transactions...${NC}"
            
            # Check if blockchain node is running
            if ! lsof -i:8545 > /dev/null 2>&1; then
                echo -e "${RED}❌ Blockchain node is NOT running!${NC}"
                echo "Start it with option 39 first."
                read -p "Press Enter to continue..."
                continue
            fi
            
            echo -e "${YELLOW}This will simulate: Pack Purchase, Trades, and Matches${NC}"
            echo ""
            cd dueling-blockchain && npm run simulate && cd ..
            echo ""
            echo -e "${GREEN}✅ Simulation completed!${NC}"
            echo "You can now verify the data with options 42-46"
            read -p "Press Enter to continue..."
            ;;
        42)
            echo -e "${GREEN}Verifying Card Ownership...${NC}"
            
            # Check if blockchain node is running
            if ! lsof -i:8545 > /dev/null 2>&1; then
                echo -e "${RED}❌ Blockchain node is NOT running!${NC}"
                echo "Start it with: ./menu.sh → 39"
                read -p "Press Enter to continue..."
                continue
            fi
            
            echo ""
            read -p "Enter player address (or press Enter for default): " player_addr
            if [ -z "$player_addr" ]; then
                cd dueling-blockchain && npm run verify:ownership && cd ..
            else
                cd dueling-blockchain && PLAYER_ADDRESS="$player_addr" npm run verify:ownership && cd ..
            fi
            read -p "Press Enter to continue..."
            ;;
        43)
            echo -e "${GREEN}Verifying Card Uniqueness...${NC}"
            
            # Check if blockchain node is running
            if ! lsof -i:8545 > /dev/null 2>&1; then
                echo -e "${RED}❌ Blockchain node is NOT running!${NC}"
                echo "Start it with: ./menu.sh → 39"
                read -p "Press Enter to continue..."
                continue
            fi
            
            echo ""
            read -p "Enter Token ID to verify: " token_id
            if [ -z "$token_id" ]; then
                echo -e "${RED}Token ID is required!${NC}"
            else
                cd dueling-blockchain && TOKEN_ID="$token_id" npm run verify:card && cd ..
            fi
            read -p "Press Enter to continue..."
            ;;
        41)
            echo -e "${GREEN}Verifying Distributed Ledger...${NC}"
            bash ./scripts/verify_blockchain_ledger.sh
            read -p "Press Enter to continue..."
            ;;
        42)
            echo -e "${GREEN}Simulating Blockchain Transactions...${NC}"
            
            # Check if blockchain node is running
            if ! lsof -i:8545 > /dev/null 2>&1; then
                echo -e "${RED}❌ Blockchain node is NOT running!${NC}"
                echo "Start it with: ./menu.sh → 39"
                read -p "Press Enter to continue..."
                continue
            fi
            
            cd dueling-blockchain && npm run simulate && cd ..
            read -p "Press Enter to continue..."
            ;;
        43)
            echo -e "${GREEN}Verifying Card Ownership...${NC}"
            
            # Check if blockchain node is running
            if ! lsof -i:8545 > /dev/null 2>&1; then
                echo -e "${RED}❌ Blockchain node is NOT running!${NC}"
                echo "Start it with: ./menu.sh → 39"
                read -p "Press Enter to continue..."
                continue
            fi
            
            echo ""
            read -p "Enter player address (or press Enter for default): " player_addr
            if [ -z "$player_addr" ]; then
                cd dueling-blockchain && npm run verify:ownership && cd ..
            else
                cd dueling-blockchain && PLAYER_ADDRESS="$player_addr" npm run verify:ownership && cd ..
            fi
            read -p "Press Enter to continue..."
            ;;
        44)
            echo -e "${GREEN}Verifying Card Uniqueness...${NC}"
            
            # Check if blockchain node is running
            if ! lsof -i:8545 > /dev/null 2>&1; then
                echo -e "${RED}❌ Blockchain node is NOT running!${NC}"
                echo "Start it with: ./menu.sh → 39"
                read -p "Press Enter to continue..."
                continue
            fi
            
            cd dueling-blockchain && npm run verify:uniqueness && cd ..
            read -p "Press Enter to continue..."
            ;;
        45)
            echo -e "${GREEN}Viewing Purchase History...${NC}"
            
            # Check if blockchain node is running
            if ! lsof -i:8545 > /dev/null 2>&1; then
                echo -e "${RED}❌ Blockchain node is NOT running!${NC}"
                echo "Start it with: ./menu.sh → 39"
                read -p "Press Enter to continue..."
                continue
            fi
            
            echo ""
            read -p "Enter player address (or press Enter for default): " player_addr
            if [ -z "$player_addr" ]; then
                cd dueling-blockchain && npm run verify:purchases && cd ..
            else
                cd dueling-blockchain && PLAYER_ADDRESS="$player_addr" npm run verify:purchases && cd ..
            fi
            read -p "Press Enter to continue..."
            ;;
        46)
            echo -e "${GREEN}Viewing Match Results (Blockchain)...${NC}"
            
            # Check if blockchain node is running
            if ! lsof -i:8545 > /dev/null 2>&1; then
                echo -e "${RED}❌ Blockchain node is NOT running!${NC}"
                echo "Start it with: ./menu.sh → 39"
                read -p "Press Enter to continue..."
                continue
            fi
            
            echo ""
            read -p "Enter player address (or press Enter for default): " player_addr
            if [ -z "$player_addr" ]; then
                cd dueling-blockchain && npm run verify:matches && cd ..
            else
                cd dueling-blockchain && PLAYER_ADDRESS="$player_addr" npm run verify:matches && cd ..
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
                echo "   Start with: ./menu.sh → 39"
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
                cd dueling-blockchain && npm run verify:ownership && cd ..
            else
                cd dueling-blockchain && PLAYER_ADDRESS="$player_addr" npm run verify:ownership && cd ..
            fi
            echo ""
            echo -e "${BLUE}════════════════════════════════════════${NC}"
            echo -e "${BLUE}2/3: Viewing Purchase History${NC}"
            echo -e "${BLUE}════════════════════════════════════════${NC}"
            if [ -z "$player_addr" ]; then
                cd dueling-blockchain && npm run verify:purchases && cd ..
            else
                cd dueling-blockchain && PLAYER_ADDRESS="$player_addr" npm run verify:purchases && cd ..
            fi
            echo ""
            echo -e "${BLUE}════════════════════════════════════════${NC}"
            echo -e "${BLUE}3/3: Viewing Match Results${NC}"
            echo -e "${BLUE}════════════════════════════════════════${NC}"
            if [ -z "$player_addr" ]; then
                cd dueling-blockchain && npm run verify:matches && cd ..
            else
                cd dueling-blockchain && PLAYER_ADDRESS="$player_addr" npm run verify:matches && cd ..
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
            cd dueling-blockchain && npx hardhat console --network localhost && cd ..
            read -p "Press Enter to continue..."
            ;;
        49)
            echo -e "${GREEN}Running Blockchain Tests...${NC}"
            cd dueling-blockchain && npm test && cd ..
            read -p "Press Enter to continue..."
            ;;
        50)
            echo -e "${BLUE}╔════════════════════════════════════════════════════════════════╗${NC}"
            echo -e "${BLUE}║        🚀 FULL SYSTEM DEPLOY - Automated Workflow             ║${NC}"
            echo -e "${BLUE}╚════════════════════════════════════════════════════════════════╝${NC}"
            echo ""
            
            echo -e "${GREEN}Step 1/4: Building project...${NC}"
            mvn clean package -DskipTests
            
            echo ""
            echo -e "${GREEN}Step 2/4: Building Docker images...${NC}"
            cd docker && docker compose build
            
            echo ""
            echo -e "${GREEN}Step 3/4: Starting services...${NC}"
            docker compose down
            docker compose up -d
            
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
            echo "  • Run clients: ./menu.sh → 10"
            echo "  • Verify ledger: ./menu.sh → 54"
            echo ""
            cd ..
            read -p "Press Enter to continue..."
            ;;
        51)
            echo -e "${GREEN}📋 Copying Deployment Info from Blockchain Container...${NC}"
            echo ""
            
            if ! docker ps | grep -q "dueling-blockchain"; then
                echo -e "${RED}❌ Blockchain container is not running!${NC}"
                echo "Start system first: ./menu.sh → 5"
                read -p "Press Enter to continue..."
                continue
            fi
            
            docker exec dueling-blockchain cat /usr/src/app/deployment-info.json > \
                dueling-blockchain/deployment-info.json
            
            if [ $? -eq 0 ]; then
                echo -e "${GREEN}✅ Deployment info copied successfully!${NC}"
                echo ""
                echo "File location: dueling-blockchain/deployment-info.json"
                echo ""
                cat dueling-blockchain/deployment-info.json | python3 -m json.tool 2>/dev/null || \
                    cat dueling-blockchain/deployment-info.json
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
            cd docker && docker compose down
            
            echo ""
            echo -e "${GREEN}Step 2/3: Starting services...${NC}"
            docker compose up -d
            
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
            cd ..
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
            echo -e "${GREEN}Instructions saved in: FINAL_STATUS.md${NC}"
            echo ""
            echo -e "${BLUE}Opening guide...${NC}"
            cat FINAL_STATUS.md | less
            read -p "Press Enter to continue..."
            ;;
        54)
            echo -e "${BLUE}╔════════════════════════════════════════════════════════════════╗${NC}"
            echo -e "${BLUE}║          📊 QUICK LEDGER CHECK - Automated                     ║${NC}"
            echo -e "${BLUE}╚════════════════════════════════════════════════════════════════╝${NC}"
            echo ""
            
            if ! docker ps | grep -q "dueling-blockchain"; then
                echo -e "${RED}❌ Blockchain is not running!${NC}"
                echo "Start system: ./menu.sh → 5"
                read -p "Press Enter to continue..."
                continue
            fi
            
            echo -e "${GREEN}Step 1/2: Copying deployment info...${NC}"
            docker exec dueling-blockchain cat /usr/src/app/deployment-info.json > \
                dueling-blockchain/deployment-info.json 2>/dev/null
            
            if [ $? -eq 0 ]; then
                echo -e "${GREEN}✅ Deployment info updated${NC}"
            else
                echo -e "${RED}⚠️  Using cached deployment info${NC}"
            fi
            
            echo ""
            echo -e "${GREEN}Step 2/2: Verifying ledger...${NC}"
            echo ""
            bash scripts/verify_blockchain_ledger.sh
            
            echo ""
            read -p "Press Enter to continue..."
            ;;
        55)
            echo -e "${BLUE}╔════════════════════════════════════════════════════════════════╗${NC}"
            echo -e "${BLUE}║         🛠️  FIX & REBUILD - Complete Rebuild Workflow         ║${NC}"
            echo -e "${BLUE}╚════════════════════════════════════════════════════════════════╝${NC}"
            echo ""
            
            echo -e "${GREEN}Step 1/5: Maven clean compile...${NC}"
            mvn clean compile
            
            echo ""
            echo -e "${GREEN}Step 2/5: Maven package...${NC}"
            mvn package -DskipTests
            
            echo ""
            echo -e "${GREEN}Step 3/5: Docker build...${NC}"
            cd docker && docker compose build
            
            echo ""
            echo -e "${GREEN}Step 4/5: Restart system...${NC}"
            docker compose down
            docker compose up -d
            
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
            cd ..
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
