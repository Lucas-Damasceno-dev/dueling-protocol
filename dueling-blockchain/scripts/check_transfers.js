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
    
    console.log(`\n=== ALL TRADE TRANSFERS (${trades.length} total) ===\n`);
    for (const [i, trade] of trades.entries()) {
        console.log(`Transfer #${i+1}:`);
        console.log(`  From: ${trade.args.from}`);
        console.log(`  To: ${trade.args.to}`);
        console.log(`  TokenId: ${trade.args.tokenId.toString()}`);
        console.log(`  Block: ${trade.blockNumber}`);
        console.log(`  TX: ${trade.transactionHash.substring(0, 20)}...`);
        console.log();
    }
}

main().catch(console.error);
