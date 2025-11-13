const { ethers } = require("hardhat");

async function main() {
    const [owner] = await ethers.getSigners();
    
    const deployedAddresses = {
        AssetContract: "0x5FbDB2315678afecb367f032d93F642f64180aa3",
        StoreContract: "0xe7f1725E7734CE288F8367e1Bb143E90bb3F0512",
        TradeContract: "0xCf7Ed3AccA5a467e9e704C703E8D87F634fB0Fc9",
        MatchContract: "0xDc64a140Aa3E981100a9becA4E685f962f0cF6C9"
    };
    
    const AssetContract = await ethers.getContractAt("AssetContract", deployedAddresses.AssetContract);
    const StoreContract = await ethers.getContractAt("StoreContract", deployedAddresses.StoreContract);
    const TradeContract = await ethers.getContractAt("TradeContract", deployedAddresses.TradeContract);
    
    console.log("=== BLOCKCHAIN LEDGER VERIFICATION ===\n");
    console.log("Contract Addresses:");
    console.log(`  AssetContract: ${deployedAddresses.AssetContract}`);
    console.log(`  StoreContract: ${deployedAddresses.StoreContract}`);
    console.log(`  TradeContract: ${deployedAddresses.TradeContract}`);
    console.log(`  MatchContract: ${deployedAddresses.MatchContract}\n`);
    
    // Check Card Minting
    console.log("🎴 CARD OWNERSHIP:");
    try {
        const totalCards = await AssetContract.getTotalCards();
        console.log(`Total cards minted: ${totalCards}\n`);
        
        if (totalCards > 0) {
            console.log("Cards details:");
            for (let i = 0; i < Math.min(totalCards, 20); i++) {
                try {
                    const owner = await AssetContract.ownerOf(i);
                    const card = await AssetContract.getCard(i);
                    console.log(`  Card #${i}: ${card.name} | Owner: ${owner.substring(0, 10)}...`);
                } catch (e) {
                    console.log(`  Card #${i}: Error - ${e.message}`);
                }
            }
        } else {
            console.log("  ⚠️  No cards have been minted yet.");
        }
    } catch (e) {
        console.log(`  ❌ Error accessing AssetContract: ${e.message}`);
    }
    
    // Check Purchase History for default account
    console.log("\n📦 PURCHASE HISTORY (Default Account):");
    try {
        const purchaseIds = await StoreContract.getPurchaseHistory(owner.address);
        console.log(`Total purchases by ${owner.address.substring(0, 10)}...: ${purchaseIds.length}`);
        
        for (let i = 0; i < Math.min(purchaseIds.length, 5); i++) {
            const purchase = await StoreContract.getPurchase(purchaseIds[i]);
            console.log(`\nPurchase #${purchaseIds[i]}:`);
            console.log(`  Pack Type: ${purchase.packType}`);
            console.log(`  Timestamp: ${new Date(Number(purchase.timestamp) * 1000).toISOString()}`);
            console.log(`  Cards: ${purchase.cardIds.join(", ")}`);
        }
        
        if (purchaseIds.length === 0) {
            console.log("  ⚠️  No purchases recorded yet.");
        }
    } catch (e) {
        console.log(`  ❌ Error accessing StoreContract: ${e.message}`);
    }
    
    // Check Trade History
    console.log("\n🔄 TRADE HISTORY:");
    try {
        const playerTrades = await TradeContract.getPlayerTrades(owner.address);
        console.log(`Total trades involving ${owner.address.substring(0, 10)}...: ${playerTrades.length}`);
        
        for (let i = 0; i < Math.min(playerTrades.length, 5); i++) {
            const trade = await TradeContract.getTrade(playerTrades[i]);
            console.log(`\nTrade #${playerTrades[i]}:`);
            console.log(`  Initiator: ${trade.initiator.substring(0, 10)}...`);
            console.log(`  Partner: ${trade.partner.substring(0, 10)}...`);
            console.log(`  Status: ${trade.status}`);
            console.log(`  Timestamp: ${new Date(Number(trade.timestamp) * 1000).toISOString()}`);
            console.log(`  Initiator Cards: ${trade.initiatorCardIds.join(", ")}`);
            console.log(`  Partner Cards: ${trade.partnerCardIds.join(", ")}`);
        }
        
        if (playerTrades.length === 0) {
            console.log("  ⚠️  No trades recorded yet.");
        }
    } catch (e) {
        console.log(`  ❌ Error accessing TradeContract: ${e.message}`);
    }
    
    console.log("\n=== VERIFICATION COMPLETE ===");
    console.log("\n💡 NOTE: If you haven't run the client yet (option 10 from menu.sh),");
    console.log("   the blockchain will not have any transactions registered.");
    console.log("   Run the client, make purchases and trades, then check again.");
}

main()
    .then(() => process.exit(0))
    .catch((error) => {
        console.error(error);
        process.exit(1);
    });
