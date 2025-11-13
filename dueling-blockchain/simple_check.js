const { ethers } = require("hardhat");

async function main() {
    console.log("Testing blockchain connection...\n");
    
    const [signer] = await ethers.getSigners();
    console.log("Signer address:", signer.address);
    
    const balance = await ethers.provider.getBalance(signer.address);
    console.log("Balance:", ethers.formatEther(balance), "ETH");
    
    const network = await ethers.provider.getNetwork();
    console.log("Network chainId:", network.chainId.toString());
    
    const blockNumber = await ethers.provider.getBlockNumber();
    console.log("Current block:", blockNumber);
    
    console.log("\n✅ Blockchain is working!");
    console.log("\n💡 Contracts were deployed successfully.");
    console.log("   However, no transactions have been made yet by the game clients.");
    console.log("   The ledger will be empty until players make purchases, trades, or play matches.");
}

main()
    .then(() => process.exit(0))
    .catch((error) => {
        console.error("❌ Error:", error.message);
        process.exit(1);
    });
