const hre = require("hardhat");

async function main() {
  console.log("🚀 Starting deployment of Dueling Protocol Smart Contracts...\n");

  // Get deployer account
  const [deployer] = await hre.ethers.getSigners();
  console.log("📝 Deploying contracts with account:", deployer.address);
  
  // Get balance using provider
  const balance = await hre.ethers.provider.getBalance(deployer.address);
  console.log("💰 Account balance:", hre.ethers.formatEther(balance), "ETH\n");

  // Deploy AssetContract
  console.log("📦 Deploying AssetContract...");
  const AssetContract = await hre.ethers.getContractFactory("AssetContract");
  const assetContract = await AssetContract.deploy();
  await assetContract.waitForDeployment();
  console.log("✅ AssetContract deployed to:", await assetContract.getAddress());

  // Deploy StoreContract
  console.log("\n📦 Deploying StoreContract...");
  const StoreContract = await hre.ethers.getContractFactory("StoreContract");
  const storeContract = await StoreContract.deploy(await assetContract.getAddress());
  await storeContract.waitForDeployment();
  console.log("✅ StoreContract deployed to:", await storeContract.getAddress());

  // Authorize StoreContract as minter (instead of transferring ownership)
  console.log("\n🔄 Authorizing StoreContract as minter...");
  await assetContract.authorizeMinter(await storeContract.getAddress());
  console.log("✅ StoreContract authorized to mint cards");
  console.log("ℹ️  Deployer remains owner and can also mint (for game server integration)");

  // Deploy TradeContract
  console.log("\n📦 Deploying TradeContract...");
  const TradeContract = await hre.ethers.getContractFactory("TradeContract");
  const tradeContract = await TradeContract.deploy(await assetContract.getAddress());
  await tradeContract.waitForDeployment();
  console.log("✅ TradeContract deployed to:", await tradeContract.getAddress());

  // Deploy MatchContract
  console.log("\n📦 Deploying MatchContract...");
  const MatchContract = await hre.ethers.getContractFactory("MatchContract");
  const matchContract = await MatchContract.deploy(deployer.address); // deployer is game server for now
  await matchContract.waitForDeployment();
  console.log("✅ MatchContract deployed to:", await matchContract.getAddress());

  // Summary
  console.log("\n" + "=".repeat(70));
  console.log("🎉 DEPLOYMENT COMPLETE!");
  console.log("=".repeat(70));
  console.log("\n📋 Contract Addresses:");
  console.log("   AssetContract  :", await assetContract.getAddress());
  console.log("   StoreContract  :", await storeContract.getAddress());
  console.log("   TradeContract  :", await tradeContract.getAddress());
  console.log("   MatchContract  :", await matchContract.getAddress());
  console.log("\n📝 Save these addresses for integration with the gateway!\n");

  // Save deployment info to file
  const fs = require("fs");
  const deploymentInfo = {
    network: hre.network.name,
    deployer: deployer.address,
    timestamp: new Date().toISOString(),
    contracts: {
      AssetContract: await assetContract.getAddress(),
      StoreContract: await storeContract.getAddress(),
      TradeContract: await tradeContract.getAddress(),
      MatchContract: await matchContract.getAddress(),
    },
  };

  fs.writeFileSync(
    "deployment-info.json",
    JSON.stringify(deploymentInfo, null, 2)
  );
  console.log("💾 Deployment info saved to deployment-info.json\n");
}

main()
  .then(() => process.exit(0))
  .catch((error) => {
    console.error(error);
    process.exit(1);
  });
