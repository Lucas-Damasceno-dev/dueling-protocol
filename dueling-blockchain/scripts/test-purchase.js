const hre = require("hardhat");

async function main() {
  const deployment = JSON.parse(require('fs').readFileSync('./deployment-info.json', 'utf8'));
  const [owner, player1] = await hre.ethers.getSigners();
  
  console.log("Testing purchase with deployed contracts...");
  console.log("Player 1:", player1.address);
  console.log("StoreContract:", deployment.contracts.StoreContract);
  
  const StoreContract = await hre.ethers.getContractFactory("StoreContract");
  const store = StoreContract.attach(deployment.contracts.StoreContract);
  
  const packPrice = await store.packPrice();
  console.log("Pack price:", hre.ethers.formatEther(packPrice), "ETH");
  
  const tx = await store.connect(player1).purchasePack(3, { value: packPrice });
  await tx.wait();
  console.log("✅ Pack purchased!");
  
  const purchaseIds = await store.getPurchaseHistory(player1.address);
  console.log("Purchase IDs:", purchaseIds.toString());
}

main().then(() => process.exit(0)).catch(console.error);
