const hre = require("hardhat");
const fs = require('fs');
const path = require('path');

async function main() {
    const deploymentPath = path.join(__dirname, '..', 'deployment-info.json');
    const deployment = JSON.parse(fs.readFileSync(deploymentPath, 'utf8'));
    const assetAddress = deployment.contracts.AssetContract;
    
    const AssetContract = await hre.ethers.getContractFactory("AssetContract");
    const assetContract = AssetContract.attach(assetAddress);
    
    const transferFilter = assetContract.filters.Transfer();
    const allTransfers = await assetContract.queryFilter(transferFilter, 0, 'latest');
    const trades = allTransfers.filter(t => 
        t.args.from !== "0x0000000000000000000000000000000000000000"
    );
    
    console.log(`\n=== DEBUGGING TRANSFERS (${trades.length} total) ===\n`);
    
    for (const [i, trade] of trades.entries()) {
        const tokenId = trade.args.tokenId.toString();
        const from = trade.args.from;
        const to = trade.args.to;
        
        // Get current owner
        let currentOwner = "ERROR";
        try {
            currentOwner = await assetContract.ownerOf(tokenId);
        } catch (e) {
            currentOwner = `ERROR: ${e.message}`;
        }
        
        const isSelfTransfer = from.toLowerCase() === to.toLowerCase();
        
        console.log(`Transfer #${i+1}:`);
        console.log(`  TokenId: ${tokenId}`);
        console.log(`  From: ${from.substring(0, 10)}...`);
        console.log(`  To: ${to.substring(0, 10)}...`);
        console.log(`  Self-transfer: ${isSelfTransfer ? 'YES ⚠️' : 'NO'}`);
        console.log(`  Current owner: ${currentOwner.substring(0, 10)}...`);
        console.log(`  Block: ${trade.blockNumber}`);
        console.log(`  TX: ${trade.transactionHash.substring(0, 20)}...`);
        console.log();
    }
}

main().catch(console.error);
