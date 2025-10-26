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
    echo -e "5.  Start Complete System (Docker + NGINX) ${GREEN}[Multi-PC Ready]${NC}"
    echo -e "6.  Start Game Local (Java processes) ${GREEN}[Single PC]${NC}"
    echo -e "7.  Stop All Services"
    echo -e "8.  Build Project"
    echo -e "9.  System Status Check"
    echo
    echo -e "${YELLOW}Run Client & Server (Direct):${NC}"
    echo "10. Run Client (Java)"
    echo "11. Run Server (Java)"
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
    echo -e "${RED}0. Exit${NC}"
    echo
    echo -n "Choose an option (0-38): "
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
            echo -e "${GREEN}Starting Complete System (Docker + NGINX)...${NC}"
            echo -e "${YELLOW}This may take 2-3 minutes...${NC}"
            bash ./scripts/deploy/start_complete_system.sh
            read -p "Press Enter to continue..."
            ;;
        6)
            echo -e "${GREEN}Starting Game Local (Java processes)...${NC}"
            bash ./scripts/deploy/stop_all_services.sh
            bash ./scripts/deploy/start_game_local.sh
            read -p "Press Enter to continue..."
            ;;
        7)
            echo -e "${GREEN}Stopping All Services...${NC}"
            bash ./scripts/deploy/stop_all_services.sh
            read -p "Press Enter to continue..."
            ;;
        8)
            echo -e "${GREEN}Building Project...${NC}"
            mvn clean package -DskipTests
            read -p "Press Enter to continue..."
            ;;
        9)
            system_status
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
            bash ./test_scripts/functional/test_cross_server_trade.sh
            read -p "Press Enter to continue..."
            ;;
        23)
            echo -e "${GREEN}Testing Cross-Server Match...${NC}"
            bash ./test_scripts/functional/test_cross_server_match.sh
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
            bash ./test_scripts/distributed/test_cross_server_matchmaking.sh
            read -p "Press Enter to continue..."
            ;;
        31)
            echo -e "${GREEN}Testing Distributed Matchmaking...${NC}"
            bash ./test_scripts/distributed/test_matchmaking_distributed.sh
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
