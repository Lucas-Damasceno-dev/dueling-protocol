const hre = require("hardhat");
const fs = require('fs');
const path = require('path');

async function main() {
    const deploymentPath = path.join(__dirname, '..', 'deployment-info.json');
    const deployment = JSON.parse(fs.readFileSync(deploymentPath, 'utf8'));
    const assetAddress = deployment.contracts.AssetContract;
    
    const AssetContract = await hre.ethers.getContractFactory("AssetContract");
    const assetContract = AssetContract.attach(assetAddress);
    
    // Get all mints
    const mintFilter = assetContract.filters.Transfer(
        "0x0000000000000000000000000000000000000000"
    );
    const mintEvents = await assetContract.queryFilter(mintFilter, 0, 'latest');
    
    console.log("\n=== CARDS MINTED ===");
    console.log(`Total: ${mintEvents.length} cards\n`);
    
    const playerCards = {};
    for (const event of mintEvents) {
        const owner = event.args.to;
        const tokenId = event.args.tokenId.toString();
        
        if (!playerCards[owner]) {
            playerCards[owner] = [];
        }
        
        try {
            const cardData = await assetContract.getCard(tokenId);
            playerCards[owner].push({
                tokenId,
                cardName: cardData.cardName,
                currentOwner: await assetContract.ownerOf(tokenId)
            });
        } catch (e) {
            playerCards[owner].push({ tokenId, error: e.message });
        }
    }
    
    for (const [owner, cards] of Object.entries(playerCards)) {
        console.log(`${owner.substring(0, 10)}... originally minted ${cards.length} cards:`);
        for (const card of cards) {
            if (card.error) {
                console.log(`  Token #${card.tokenId} - ERROR: ${card.error}`);
            } else {
                const currentOwner = card.currentOwner.substring(0, 10);
                const originalOwner = owner.substring(0, 10);
                const moved = currentOwner !== originalOwner;
                console.log(`  Token #${card.tokenId} - ${card.cardName} | Current owner: ${currentOwner}... ${moved ? '⚠️ MOVED' : '✓'}`);
            }
        }
        console.log();
    }
    
    // Get all transfers (trades)
    const transferFilter = assetContract.filters.Transfer();
    const allTransfers = await assetContract.queryFilter(transferFilter, 0, 'latest');
    const trades = allTransfers.filter(t => 
        t.args.from !== "0x0000000000000000000000000000000000000000"
    );
    
    console.log("\n=== TRADES (NON-MINT TRANSFERS) ===");
    console.log(`Total: ${trades.length} transfers\n`);
    
    for (const [i, trade] of trades.entries()) {
        const from = trade.args.from.substring(0, 10);
        const to = trade.args.to.substring(0, 10);
        const tokenId = trade.args.tokenId.toString();
        const isSelf = trade.args.from.toLowerCase() === trade.args.to.toLowerCase();
        
        console.log(`Transfer #${i+1}:`);
        console.log(`  Token #${tokenId}: ${from}... → ${to}...${isSelf ? ' ⚠️ SELF-TRANSFER' : ''}`);
        console.log(`  Block: ${trade.blockNumber} | TX: ${trade.transactionHash.substring(0, 20)}...`);
    }
}

main().catch(console.error);
