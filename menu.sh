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
    echo -e "${YELLOW}System Management:${NC}"
    echo -e "1.  Start Complete System (Docker + NGINX) ${GREEN}[Multi-PC Ready]${NC}"
    echo -e "2.  Start Game Local (Java processes) ${GREEN}[Single PC]${NC}"
    echo -e "3.  Start Game Local Simple (No Sentinel) ${GREEN}[Single PC]${NC}"
    echo -e "4.  Start Game Local Debug ${GREEN}[Single PC]${NC}"
    echo
    echo -e "${YELLOW}Remote Deployment:${NC}"
    echo -e "5.  Start Client Remote"
    echo "6.  Start Gateway Remote"
    echo "7.  Start Server Remote"
    echo "8.  Setup Distributed Environment"
    echo
    echo -e "${YELLOW}Testing & Monitoring:${NC}"
    echo "9.  Run All Tests"
    echo "10. Run Client"
    echo "11. Run Server"
    echo "12. Monitor All Logs"
    echo "13. Test Client WebSocket"
    echo "14. Test Dueling Protocol"
    echo "15. Test Redis Sentinel"
    echo "16. Test S2S Communication"
    echo "17. Test Game State Consistency"
    echo "18. Test Mid-Game Disconnection"
    echo "19. Test Persistence Race Condition"
    echo "20. Test Queue Disconnection"
    echo "21. Test Simultaneous Play"
    echo "22. Test Stock Concurrency"
    echo "23. Test Cross Server Matchmaking"
    echo "24. Test Global Coordination"
    echo "25. Test Distributed Matchmaking"
    echo "26. Test Purchase Global"
    echo "27. Test Redis Failover"
    echo "28. Test Advanced Security"
    echo
    echo -e "${YELLOW}System Control:${NC}"
    echo "29. Stop All Services"
    echo "30. Build Project"
    echo "31. View Running Containers"
    echo
    echo -e "${YELLOW}Help & Info:${NC}"
    echo "32. View Documentation"
    echo "33. System Status Check"
    echo
    echo -e "${RED}0. Exit${NC}"
    echo
    echo -n "Choose an option (0-33): "
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
            echo -e "${GREEN}Starting Complete System (Docker + NGINX)...${NC}"
            echo -e "${YELLOW}This may take 2-3 minutes...${NC}"
            bash ./scripts/deploy/start_complete_system.sh
            read -p "Press Enter to continue..."
            ;;
        2)
            echo -e "${GREEN}Starting Game Local (Java processes)...${NC}"
            bash ./scripts/deploy/stop_all_services.sh
            bash ./scripts/deploy/start_game_local.sh
            read -p "Press Enter to continue..."
            ;;
        3)
            echo -e "${GREEN}Starting Game Local Simple (No Sentinel)...${NC}"
            bash ./scripts/deploy/stop_all_services.sh
            bash ./scripts/deploy/start_game_local_simple.sh
            read -p "Press Enter to continue..."
            ;;
        4)
            echo -e "${GREEN}Starting Game Local Debug...${NC}"
            bash ./scripts/deploy/stop_all_services.sh
            bash ./scripts/deploy/start_game_local_debug.sh
            read -p "Press Enter to continue..."
            ;;
        5)
            echo -e "${GREEN}Starting Client Remote...${NC}"
            bash ./scripts/deploy/start_client_remote.sh
            read -p "Press Enter to continue..."
            ;;
        6)
            echo -e "${GREEN}Starting Gateway Remote...${NC}"
            bash ./scripts/deploy/start_gateway_remote.sh
            read -p "Press Enter to continue..."
            ;;
        7)
            echo -e "${GREEN}Starting Server Remote...${NC}"
            bash ./scripts/deploy/start_server_remote.sh
            read -p "Press Enter to continue..."
            ;;
        8)
            echo -e "${GREEN}Setting up Distributed Environment...${NC}"
            bash ./scripts/deploy/setup_distributed.sh
            read -p "Press Enter to continue..."
            ;;
        9)
            echo -e "${GREEN}Running All Tests...${NC}"
            bash ./scripts/build/run_all_tests.sh
            read -p "Press Enter to continue..."
            ;;
        10)
            echo -e "${GREEN}Running Client...${NC}"
            bash ./scripts/run/run_client.sh
            read -p "Press Enter to continue..."
            ;;
        11)
            echo -e "${GREEN}Running Server...${NC}"
            bash ./scripts/run/run_server.sh
            read -p "Press Enter to continue..."
            ;;
        12)
            echo -e "${GREEN}Monitoring All Logs...${NC}"
            bash ./scripts/monitor/monitor_logs.sh
            read -p "Press Enter to continue..."
            ;;
        13)
            echo -e "${GREEN}Testing Client WebSocket...${NC}"
            bash ./test_scripts/functional/test_client_websocket.sh
            read -p "Press Enter to continue..."
            ;;
        14)
            echo -e "${GREEN}Testing Dueling Protocol...${NC}"
            bash ./test_scripts/functional/test_dueling_protocol.sh
            read -p "Press Enter to continue..."
            ;;
        15)
            echo -e "${GREEN}Testing Redis Sentinel...${NC}"
            bash ./test_scripts/infrastructure/test_redis_sentinel.sh
            read -p "Press Enter to continue..."
            ;;
        16)
            echo -e "${GREEN}Testing S2S Communication...${NC}"
            bash ./test_scripts/functional/test_s2s_communication.sh
            read -p "Press Enter to continue..."
            ;;
        17)
            echo -e "${GREEN}Testing Game State Consistency...${NC}"
            bash ./test_scripts/functional/test_game_state_consistency.sh
            read -p "Press Enter to continue..."
            ;;
        18)
            echo -e "${GREEN}Testing Mid-Game Disconnection...${NC}"
            bash ./test_scripts/functional/test_mid_game_disconnection.sh
            read -p "Press Enter to continue..."
            ;;
        19)
            echo -e "${GREEN}Testing Persistence Race Condition...${NC}"
            bash ./test_scripts/functional/test_persistence_race_condition.sh
            read -p "Press Enter to continue..."
            ;;
        20)
            echo -e "${GREEN}Testing Queue Disconnection...${NC}"
            bash ./test_scripts/functional/test_queue_disconnection.sh
            read -p "Press Enter to continue..."
            ;;
        21)
            echo -e "${GREEN}Testing Simultaneous Play...${NC}"
            bash ./test_scripts/functional/test_simultaneous_play.sh
            read -p "Press Enter to continue..."
            ;;
        22)
            echo -e "${GREEN}Testing Stock Concurrency...${NC}"
            bash ./test_scripts/concurrency/test_stock_concurrency.sh
            read -p "Press Enter to continue..."
            ;;
        23)
            echo -e "${GREEN}Testing Cross Server Matchmaking...${NC}"
            bash ./test_scripts/distributed/test_cross_server_matchmaking.sh
            read -p "Press Enter to continue..."
            ;;
        24)
            echo -e "${GREEN}Testing Global Coordination...${NC}"
            bash ./test_scripts/distributed/test_global_coordination.sh
            read -p "Press Enter to continue..."
            ;;
        25)
            echo -e "${GREEN}Testing Distributed Matchmaking...${NC}"
            bash ./test_scripts/distributed/test_matchmaking_distributed.sh
            read -p "Press Enter to continue..."
            ;;
        26)
            echo -e "${GREEN}Testing Purchase Global...${NC}"
            bash ./test_scripts/distributed/test_purchase_global.sh
            read -p "Press Enter to continue..."
            ;;
        27)
            echo -e "${GREEN}Testing Redis Failover...${NC}"
            bash ./test_scripts/infrastructure/test_redis_failover.sh
            read -p "Press Enter to continue..."
            ;;
        28)
            echo -e "${GREEN}Testing Advanced Security...${NC}"
            bash ./test_scripts/security/test_advanced_security.sh
            read -p "Press Enter to continue..."
            ;;
        29)
            echo -e "${GREEN}Stopping All Services...${NC}"
            bash ./scripts/deploy/stop_all_services.sh
            read -p "Press Enter to continue..."
            ;;
        30)
            echo -e "${GREEN}Building Project...${NC}"
            bash ./scripts/build/build.sh
            read -p "Press Enter to continue..."
            ;;
        31)
            view_containers
            read -p "Press Enter to continue..."
            ;;
        32)
            view_docs
            read -p "Press Enter to continue..."
            ;;
        33)
            system_status
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
