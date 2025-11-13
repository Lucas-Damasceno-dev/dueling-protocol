const { ethers } = require("hardhat");

async function main() {
    const [owner] = await ethers.getSigners();
    
    // Addresses actually used by the server
    const actualAddresses = {
        AssetContract: "0x5FC8d32690cc91D4c39d9d3abcBD16989F875707",
        StoreContract: "0xe7f1725E7734CE288F8367e1Bb143E90bb3F0512",
        TradeContract: "0x9fE46736679d2D9a65F0992F2272dE9f3c7fa6e0",
        MatchContract: "0xCf7Ed3AccA5a467e9e704C703E8D87F634fB0Fc9"
    };
    
    const AssetContract = await ethers.getContractAt("AssetContract", actualAddresses.AssetContract);
    const StoreContract = await ethers.getContractAt("StoreContract", actualAddresses.StoreContract);
    const TradeContract = await ethers.getContractAt("TradeContract", actualAddresses.TradeContract);
    
    console.log("=== BLOCKCHAIN VERIFICATION (ACTUAL SERVER ADDRESSES) ===\n");
    
    try {
        const totalCards = await AssetContract.getTotalCards();
        console.log(`🎴 Total cards minted: ${totalCards}`);
        
        if (totalCards > 0) {
            console.log("\nFirst 10 cards:");
            for (let i = 0; i < Math.min(10, totalCards); i++) {
                const card = await AssetContract.getCard(i);
                const cardOwner = await AssetContract.ownerOf(i);
                console.log(`  #${i}: ${card.name} (Owner: ${cardOwner.substring(0, 10)}...)`);
            }
        }
    } catch (e) {
        console.log(`❌ Error accessing AssetContract: ${e.message}`);
    }
    
    try {
        const purchases = await StoreContract.getPurchaseHistory(owner.address);
        console.log(`\n📦 Purchases by ${owner.address.substring(0, 10)}...: ${purchases.length}`);
        
        for (let i = 0; i < Math.min(3, purchases.length); i++) {
            const purchase = await StoreContract.getPurchase(purchases[i]);
            console.log(`\nPurchase #${purchases[i]}:`);
            console.log(`  Buyer: ${purchase.buyer.substring(0, 10)}...`);
            console.log(`  Pack Type: ${purchase.packType}`);
            console.log(`  Cards: ${purchase.cardIds.length} cards`);
        }
    } catch (e) {
        console.log(`\n❌ Error accessing StoreContract: ${e.message}`);
    }
    
    try {
        const trades = await TradeContract.getPlayerTrades(owner.address);
        console.log(`\n🔄 Trades involving ${owner.address.substring(0, 10)}...: ${trades.length}`);
        
        for (let i = 0; i < Math.min(3, trades.length); i++) {
            const trade = await TradeContract.getTrade(trades[i]);
            console.log(`\nTrade #${trades[i]}:`);
            console.log(`  Initiator: ${trade.initiator.substring(0, 10)}...`);
            console.log(`  Partner: ${trade.partner.substring(0, 10)}...`);
            console.log(`  Status: ${trade.status}`);
        }
    } catch (e) {
        console.log(`\n❌ Error accessing TradeContract: ${e.message}`);
    }
    
    console.log("\n✅ VERIFICATION COMPLETE");
}

main()
    .then(() => process.exit(0))
    .catch((error) => {
        console.error(error);
        process.exit(1);
    });
