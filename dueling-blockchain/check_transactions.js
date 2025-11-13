const { ethers } = require("hardhat");

async function main() {
    const [owner] = await ethers.getSigners();
    
    const deployedAddresses = {
        AssetContract: "0x5FbDB2315678afecb367f032d93F642f64180aa3",
        StoreContract: "0xe7f1725E7734CE288F8367e1Bb143E90bb3F0512",
        TradeContract: "0xCf7Ed3AccA5a467e9e704C703E8D87F634fB0Fc9"
    };
    
    const AssetContract = await ethers.getContractAt("AssetContract", deployedAddresses.AssetContract);
    const StoreContract = await ethers.getContractAt("StoreContract", deployedAddresses.StoreContract);
    const TradeContract = await ethers.getContractAt("TradeContract", deployedAddresses.TradeContract);
    
    console.log("=== BLOCKCHAIN TRANSACTION VERIFICATION ===\n");
    
    // Get current block to check if contracts were deployed
    const blockNumber = await ethers.provider.getBlockNumber();
    console.log(`Current block number: ${blockNumber}\n`);
    
    // Check if there are any transactions
    if (blockNumber > 5) {
        console.log("✅ Contracts have been deployed and transactions have occurred\n");
        
        // Try to get contract code
        const assetCode = await ethers.provider.getCode(deployedAddresses.AssetContract);
        const storeCode = await ethers.provider.getCode(deployedAddresses.StoreContract);
        const tradeCode = await ethers.provider.getCode(deployedAddresses.TradeContract);
        
        console.log("Contract Code Status:");
        console.log(`  AssetContract: ${assetCode.length > 2 ? '✅ Deployed' : '❌ Not deployed'}`);
        console.log(`  StoreContract: ${storeCode.length > 2 ? '✅ Deployed' : '❌ Not deployed'}`);
        console.log(`  TradeContract: ${tradeCode.length > 2 ? '✅ Deployed' : '❌ Not deployed'}\n`);
        
        // Try to call contract functions
        try {
            const totalCards = await AssetContract.getTotalCards();
            console.log(`🎴 Total cards minted: ${totalCards}`);
            
            if (totalCards > 0) {
                console.log("\nFirst 5 cards:");
                for (let i = 0; i < Math.min(5, totalCards); i++) {
                    const card = await AssetContract.getCard(i);
                    const owner = await AssetContract.ownerOf(i);
                    console.log(`  #${i}: ${card.name} (Owner: ${owner.substring(0, 10)}...)`);
                }
            }
        } catch (e) {
            console.log(`❌ Error reading AssetContract: ${e.message}`);
        }
        
        // Check purchase history
        try {
            const purchases = await StoreContract.getPurchaseHistory(owner.address);
            console.log(`\n📦 Purchases by ${owner.address.substring(0, 10)}...: ${purchases.length}`);
        } catch (e) {
            console.log(`\n❌ Error reading StoreContract: ${e.message}`);
        }
        
        // Check trades
        try {
            const trades = await TradeContract.getPlayerTrades(owner.address);
            console.log(`🔄 Trades involving ${owner.address.substring(0, 10)}...: ${trades.length}`);
        } catch (e) {
            console.log(`❌ Error reading TradeContract: ${e.message}`);
        }
        
    } else {
        console.log("⚠️  Only deployment transactions found. No game transactions yet.");
        console.log("   Players need to make purchases, trades, or play matches.");
    }
}

main()
    .then(() => process.exit(0))
    .catch((error) => {
        console.error(error);
        process.exit(1);
    });
