const hre = require("hardhat");
const fs = require('fs');
const path = require('path');

async function main() {
    // Read deployment info
    const deploymentPath = path.join(__dirname, '..', 'deployment-info.json');
    
    let assetAddress, matchAddress;
    
    if (fs.existsSync(deploymentPath)) {
        const deployment = JSON.parse(fs.readFileSync(deploymentPath, 'utf8'));
        assetAddress = deployment.contracts.AssetContract;
        matchAddress = deployment.contracts.MatchContract;
        console.log("\n✅ Loaded addresses from deployment-info.json");
    } else {
        console.log("\n⚠️  deployment-info.json not found, using fallback addresses");
        assetAddress = "0x5FbDB2315678afecb367f032d93F642f64180aa3";
        matchAddress = "0xDc64a140Aa3E981100a9becA4E685f962f0cF6C9";
    }

    console.log("\n=== BLOCKCHAIN LEDGER VERIFICATION ===\n");
    console.log("Contract Addresses:");
    console.log(`  AssetContract: ${assetAddress}`);
    console.log(`  MatchContract: ${matchAddress}`);
    console.log();
    
    const AssetContract = await hre.ethers.getContractFactory("AssetContract");
    const assetContract = AssetContract.attach(assetAddress);
    
    const MatchContract = await hre.ethers.getContractFactory("MatchContract");
    const matchContract = MatchContract.attach(matchAddress);
    
    // Get card minting events (from purchases)
    const mintFilter = assetContract.filters.Transfer(
        "0x0000000000000000000000000000000000000000" // from zero address = mint
    );
    const mintEvents = await assetContract.queryFilter(mintFilter, 0, 'latest');
    
    console.log(`💳 CARDS MINTED (from pack purchases): ${mintEvents.length} cards\n`);
    const playerCards = {};
    
    // Load player names from address-mapping.json
    let playerNames = {};
    const addressMappingPath = path.join(__dirname, '..', 'address-mapping.json');
    if (fs.existsSync(addressMappingPath)) {
        const addressMapping = JSON.parse(fs.readFileSync(addressMappingPath, 'utf8'));
        playerNames = addressMapping;
        console.log(`✅ Loaded ${Object.keys(playerNames).length} player name mappings from address-mapping.json\n`);
    } else {
        console.log(`⚠️  address-mapping.json not found, using default player names\n`);
        playerNames = {
            "0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266": "Player1",
            "0x70997970C51812dc3A010C7d01b50e0d17dc79C8": "Player2",
            "0x3C44CdDdB6a900fa2b585dd299e03d12FA4293BC": "Player3",
            "0x90F79bf6EB2c4f870365E785982E1f101E93b906": "Player4",
            "0x15d34AAf54267DB7D7c367839AAf71A00a2C6A65": "Player5",
            "0x9965507D1a55bcC2695C58ba16FB37d819B0A4dc": "Player6",
            "0x976EA74026E726554dB657fA54763abd0C3a0aa9": "Player7"
        };
    }
    
    mintEvents.forEach((event, i) => {
        const owner = event.args.to;
        const tokenId = event.args.tokenId.toString();
        
        if (!playerCards[owner]) {
            playerCards[owner] = [];
        }
        playerCards[owner].push(tokenId);
    });
    
    // Get card details for each player
    for (const [player, tokenIds] of Object.entries(playerCards)) {
        const playerName = playerNames[player] || `Player (${player.substring(0, 8)}...)`;
        console.log(`  📦 ${playerName} cards (${tokenIds.length} total):`);
        
        for (const tokenId of tokenIds) {
            try {
                const cardData = await assetContract.getCard(tokenId);
                const rarityMap = { 1: "Common", 2: "Uncommon", 3: "Rare", 4: "Epic", 5: "Legendary" };
                const rarity = rarityMap[cardData.rarity] || `Rarity${cardData.rarity}`;
                console.log(`     • Token #${tokenId} - ${cardData.cardName} | ${cardData.cardType} (${rarity}) [ATK:${cardData.attack} DEF:${cardData.defense}]`);
            } catch (e) {
                console.log(`     • Token #${tokenId}`);
            }
        }
        console.log();
    }
    
    // Get card transfer events (from trades)
    const transferFilter = assetContract.filters.Transfer();
    const allTransfers = await assetContract.queryFilter(transferFilter, 0, 'latest');
    const trades = allTransfers.filter(t => 
        t.args.from !== "0x0000000000000000000000000000000000000000" // not a mint
    );
    
    console.log(`🔄 CARD TRANSFERS (trades): ${trades.length} transfers\n`);
    
    // Filter out invalid self-transfers (from == to) and duplicates
    const seenTransfers = new Set();
    const validTrades = trades.filter(t => {
        const key = `${t.args.from}-${t.args.to}-${t.args.tokenId}`;
        if (t.args.from.toLowerCase() === t.args.to.toLowerCase()) {
            return false; // Skip self-transfers
        }
        if (seenTransfers.has(key)) {
            return false; // Skip duplicates
        }
        seenTransfers.add(key);
        return true;
    });
    
    if (validTrades.length === 0) {
        console.log("  No valid trades recorded.\n");
    } else {
        // Group trades by pairs of players and time window
        const tradeGroups = [];
        
        for (const event of validTrades) {
            const from = event.args.from;
            const to = event.args.to;
            const blockNumber = event.blockNumber;
            
            // Try to find existing group with same players within 5 blocks
            let foundGroup = false;
            for (const group of tradeGroups) {
                const players = new Set([...group.players]);
                if (players.has(from) && players.has(to) && Math.abs(group.blockNumber - blockNumber) <= 5) {
                    group.transfers.push(event);
                    foundGroup = true;
                    break;
                }
            }
            
            if (!foundGroup) {
                tradeGroups.push({
                    players: [from, to],
                    transfers: [event],
                    blockNumber: blockNumber
                });
            }
        }
        
        let tradeNum = 1;
        for (const group of tradeGroups) {
            const transfers = group.transfers;
            
            // Group by direction
            const fromPlayers = {};
            const toPlayers = {};
            
            for (const transfer of transfers) {
                const from = transfer.args.from;
                const to = transfer.args.to;
                const tokenId = transfer.args.tokenId.toString();
                
                if (!fromPlayers[from]) fromPlayers[from] = [];
                if (!toPlayers[to]) toPlayers[to] = [];
                
                fromPlayers[from].push({ tokenId, to });
                toPlayers[to].push({ tokenId, from });
            }
            
            // Display trade in a nice format
            const players = [...new Set([...Object.keys(fromPlayers), ...Object.keys(toPlayers)])];
            
            if (players.length === 2) {
                const [player1, player2] = players;
                const p1Name = playerNames[player1] || `${player1.substring(0, 10)}...`;
                const p2Name = playerNames[player2] || `${player2.substring(0, 10)}...`;
                
                const p1Cards = fromPlayers[player1] || [];
                const p2Cards = fromPlayers[player2] || [];
                
                console.log(`  🤝 Trade #${tradeNum}:`);
                console.log(`     ${p1Name} gave:`);
                if (p1Cards.length === 0) {
                    console.log(`       → VAZIO (nenhuma carta transferida na blockchain)`);
                } else {
                    for (const card of p1Cards) {
                        try {
                            const cardData = await assetContract.getCard(card.tokenId);
                            const rarityMap = { 1: "Common", 2: "Uncommon", 3: "Rare", 4: "Epic", 5: "Legendary" };
                            const rarity = rarityMap[cardData.rarity] || `Rarity${cardData.rarity}`;
                            console.log(`       → Token #${card.tokenId} - ${cardData.cardName} | ${cardData.cardType} (${rarity}) [ATK:${cardData.attack} DEF:${cardData.defense}]`);
                        } catch (e) {
                            console.log(`       → Token #${card.tokenId}`);
                        }
                    }
                }
                
                console.log(`              ⇅ TRADE ⇅`);
                
                console.log(`     ${p2Name} gave:`);
                if (p2Cards.length === 0) {
                    console.log(`       → VAZIO (nenhuma carta transferida na blockchain)`);
                } else {
                    for (const card of p2Cards) {
                        try {
                            const cardData = await assetContract.getCard(card.tokenId);
                            const rarityMap = { 1: "Common", 2: "Uncommon", 3: "Rare", 4: "Epic", 5: "Legendary" };
                            const rarity = rarityMap[cardData.rarity] || `Rarity${cardData.rarity}`;
                            console.log(`       → Token #${card.tokenId} - ${cardData.cardName} | ${cardData.cardType} (${rarity}) [ATK:${cardData.attack} DEF:${cardData.defense}]`);
                        } catch (e) {
                            console.log(`       → Token #${card.tokenId}`);
                        }
                    }
                }
                
                // Warning if only one-way transfer
                if (p1Cards.length > 0 && p2Cards.length === 0) {
                    console.log(`     ⚠️  WARNING: One-way transfer detected! ${p2Name} didn't transfer any cards on blockchain.`);
                } else if (p1Cards.length === 0 && p2Cards.length > 0) {
                    console.log(`     ⚠️  WARNING: One-way transfer detected! ${p1Name} didn't transfer any cards on blockchain.`);
                }
                console.log();
                tradeNum++;
            }
        }
    }
    
    // Get match events
    const matchFilter = matchContract.filters.MatchRecorded();
    const matchEvents = await matchContract.queryFilter(matchFilter, 0, 'latest');
    
    console.log(`⚔️  MATCHES: ${matchEvents.length} recorded\n`);
    matchEvents.forEach((event, i) => {
        const player1 = event.args.player1;
        const player2 = event.args.player2;
        const winner = event.args.winner;
        const player1Score = event.args.player1Score.toString();
        const player2Score = event.args.player2Score.toString();
        
        const p1Name = playerNames[player1] || `${player1.substring(0, 10)}...`;
        const p2Name = playerNames[player2] || `${player2.substring(0, 10)}...`;
        
        const p1Status = (winner === player1) ? "🏆 VICTORY" : "💀 DEFEAT";
        const p2Status = (winner === player2) ? "🏆 VICTORY" : "💀 DEFEAT";
        
        console.log(`  ⚔️  Match #${i+1}:`);
        console.log(`     ${p1Name} (Score: ${player1Score}) ${p1Status}`);
        console.log(`              ⚔️  VS  ⚔️`);
        console.log(`     ${p2Name} (Score: ${player2Score}) ${p2Status}`);
        console.log(`     Winner: ${winner === player1 ? p1Name : p2Name}`);
        console.log(`     Block: ${event.blockNumber} | TX: ${event.transactionHash.substring(0, 20)}...`);
        console.log();
    });
    
    console.log("\n=== SUMMARY ===");
    console.log(`Total Cards Minted: ${mintEvents.length}`);
    console.log(`Total Card Transfers: ${trades.length}`);
    console.log(`Total Matches Recorded: ${matchEvents.length}`);
    console.log(`Total Blockchain Transactions: ${mintEvents.length + trades.length + matchEvents.length}`);
    console.log("\n=== VERIFICATION COMPLETE ===\n");
}

main().catch((error) => {
    console.error(error);
    process.exit(1);
});
