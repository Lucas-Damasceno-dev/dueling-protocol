#!/bin/bash

# Blockchain Quick Verification Script
# Provides easy access to blockchain verification tools

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${BLUE}🔗 Blockchain Verification Tool${NC}"
echo ""

# Check if we're in the right directory
if [ ! -d "dueling-blockchain" ]; then
    echo -e "${YELLOW}Error: dueling-blockchain directory not found${NC}"
    echo "Please run this script from the project root directory"
    exit 1
fi

# Get player address
if [ -z "$PLAYER_ADDRESS" ]; then
    echo "Enter player Ethereum address (or press Enter for default):"
    read -r player_addr
    if [ ! -z "$player_addr" ]; then
        export PLAYER_ADDRESS="$player_addr"
    fi
fi

echo ""
echo -e "${GREEN}Select Verification:${NC}"
echo "1. Card Ownership"
echo "2. Purchase History"
echo "3. Match Results"
echo "4. All Three (Complete Verification)"
echo "5. Verify Specific Card (by Token ID)"
echo ""
echo -n "Choose option (1-5): "
read -r choice

cd dueling-blockchain || exit 1

case $choice in
    1)
        echo -e "${GREEN}Verifying Card Ownership...${NC}"
        npm run verify:ownership
        ;;
    2)
        echo -e "${GREEN}Viewing Purchase History...${NC}"
        npm run verify:purchases
        ;;
    3)
        echo -e "${GREEN}Viewing Match Results...${NC}"
        npm run verify:matches
        ;;
    4)
        echo -e "${GREEN}Running Complete Verification...${NC}"
        echo ""
        echo -e "${BLUE}════════════════════════════════════════${NC}"
        echo -e "${BLUE}1/3: Card Ownership${NC}"
        echo -e "${BLUE}════════════════════════════════════════${NC}"
        npm run verify:ownership
        echo ""
        echo -e "${BLUE}════════════════════════════════════════${NC}"
        echo -e "${BLUE}2/3: Purchase History${NC}"
        echo -e "${BLUE}════════════════════════════════════════${NC}"
        npm run verify:purchases
        echo ""
        echo -e "${BLUE}════════════════════════════════════════${NC}"
        echo -e "${BLUE}3/3: Match Results${NC}"
        echo -e "${BLUE}════════════════════════════════════════${NC}"
        npm run verify:matches
        echo ""
        echo -e "${GREEN}════════════════════════════════════════${NC}"
        echo -e "${GREEN}Complete Verification Finished!${NC}"
        echo -e "${GREEN}════════════════════════════════════════${NC}"
        ;;
    5)
        echo ""
        echo -n "Enter Token ID: "
        read -r token_id
        if [ ! -z "$token_id" ]; then
            echo -e "${GREEN}Verifying Card #${token_id}...${NC}"
            TOKEN_ID="$token_id" npm run verify:card
        else
            echo -e "${YELLOW}Token ID required!${NC}"
        fi
        ;;
    *)
        echo -e "${YELLOW}Invalid option${NC}"
        exit 1
        ;;
esac

cd ..
