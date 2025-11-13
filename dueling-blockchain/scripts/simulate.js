const hre = require("hardhat");

async function main() {
  console.log("🎮 Simulating Dueling Protocol Blockchain Interactions\n");

  const [owner, player1, player2, gameServer] = await hre.ethers.getSigners();
  
  console.log("👥 Accounts:");
  console.log("   Owner   :", owner.address);
  console.log("   Player 1:", player1.address);
  console.log("   Player 2:", player2.address);
  console.log("   GameSrv :", gameServer.address, "\n");

  // Deploy contracts
  console.log("📦 Deploying contracts...");
  const AssetContract = await hre.ethers.getContractFactory("AssetContract");
  const assetContract = await AssetContract.deploy();
  await assetContract.waitForDeployment();

  const StoreContract = await hre.ethers.getContractFactory("StoreContract");
  const storeContract = await StoreContract.deploy(await assetContract.getAddress());
  await storeContract.waitForDeployment();

  await assetContract.transferOwnership(await storeContract.getAddress());

  const TradeContract = await hre.ethers.getContractFactory("TradeContract");
  const tradeContract = await TradeContract.deploy(await assetContract.getAddress());
  await tradeContract.waitForDeployment();

  const MatchContract = await hre.ethers.getContractFactory("MatchContract");
  const matchContract = await MatchContract.deploy(gameServer.address);
  await matchContract.waitForDeployment();

  console.log("✅ All contracts deployed\n");

  // Scenario 1: Player 1 buys a pack
  console.log("=" + "=".repeat(69));
  console.log("📦 SCENARIO 1: Player 1 buys a Gold pack");
  console.log("=".repeat(70));
  
  const packPrice = await storeContract.packPrice();
  console.log("💰 Pack price:", hre.ethers.formatEther(packPrice), "ETH");
  
  const tx1 = await storeContract.connect(player1).purchasePack(3, { value: packPrice });
  const receipt1 = await tx1.wait();
  console.log("✅ Pack purchased! Transaction:", receipt1.hash);
  
  const player1Cards = await assetContract.getPlayerCards(player1.address);
  console.log("🃏 Player 1 received", player1Cards.length, "cards");
  
  for (let i = 0; i < player1Cards.length; i++) {
    const card = await assetContract.getCard(player1Cards[i]);
    console.log(`   Card ${i + 1}: ${card.cardType} - Rarity ${card.rarity} - ATK ${card.attack} / DEF ${card.defense}`);
  }
  console.log();

  // Scenario 2: Player 2 buys a pack
  console.log("=" + "=".repeat(69));
  console.log("📦 SCENARIO 2: Player 2 buys a Silver pack");
  console.log("=".repeat(70));
  
  await storeContract.connect(player2).purchasePack(2, { value: packPrice });
  const player2Cards = await assetContract.getPlayerCards(player2.address);
  console.log("✅ Player 2 received", player2Cards.length, "cards\n");

  // Scenario 3: Trade between players
  console.log("=" + "=".repeat(69));
  console.log("🔄 SCENARIO 3: Player 1 proposes trade with Player 2");
  console.log("=".repeat(70));
  
  // Approve TradeContract to manage cards
  await assetContract.connect(player1).setApprovalForAll(await tradeContract.getAddress(), true);
  await assetContract.connect(player2).setApprovalForAll(await tradeContract.getAddress(), true);
  
  const player1OffersCard = player1Cards[0];
  const player2OffersCard = player2Cards[0];
  
  console.log(`📋 Player 1 offers card #${player1OffersCard}`);
  console.log(`📋 Player 2 offers card #${player2OffersCard}`);
  
  const tx2 = await tradeContract.connect(player1).proposeTrade(
    player2.address,
    [player1OffersCard],
    [player2OffersCard]
  );
  await tx2.wait();
  console.log("✅ Trade proposed!");
  
  const tx3 = await tradeContract.connect(player2).acceptTrade(0);
  await tx3.wait();
  console.log("✅ Trade accepted and executed!");
  
  console.log(`🔄 Card #${player1OffersCard} now belongs to Player 2`);
  console.log(`🔄 Card #${player2OffersCard} now belongs to Player 1\n`);

  // Scenario 4: Play a match
  console.log("=" + "=".repeat(69));
  console.log("⚔️  SCENARIO 4: Player 1 vs Player 2 match");
  console.log("=".repeat(70));
  
  const gameStateHash = hre.ethers.keccak256(
    hre.ethers.toUtf8Bytes("match_data_player1_vs_player2")
  );
  
  const tx4 = await matchContract.connect(gameServer).recordMatch(
    player1.address,
    player2.address,
    player1.address, // Player 1 wins
    gameStateHash,
    10, // player1 score
    8   // player2 score
  );
  await tx4.wait();
  console.log("✅ Match recorded on blockchain!");
  
  const match = await matchContract.getMatch(0);
  console.log(`🏆 Winner: Player 1`);
  console.log(`📊 Score: 10 - 8`);
  console.log(`🔐 Game State Hash: ${gameStateHash}\n`);

  // Scenario 5: Check statistics
  console.log("=" + "=".repeat(69));
  console.log("📊 SCENARIO 5: Player Statistics");
  console.log("=".repeat(70));
  
  const [p1Matches, p1Wins] = await matchContract.getPlayerStats(player1.address);
  const [p2Matches, p2Wins] = await matchContract.getPlayerStats(player2.address);
  const p1WinRate = await matchContract.getWinRate(player1.address);
  const p2WinRate = await matchContract.getWinRate(player2.address);
  
  console.log("Player 1 Statistics:");
  console.log(`   Total Matches: ${p1Matches}`);
  console.log(`   Wins: ${p1Wins}`);
  console.log(`   Win Rate: ${Number(p1WinRate) / 100}%`);
  console.log(`   Cards Owned: ${(await assetContract.getPlayerCards(player1.address)).length}`);
  
  console.log("\nPlayer 2 Statistics:");
  console.log(`   Total Matches: ${p2Matches}`);
  console.log(`   Wins: ${p2Wins}`);
  console.log(`   Win Rate: ${Number(p2WinRate) / 100}%`);
  console.log(`   Cards Owned: ${(await assetContract.getPlayerCards(player2.address)).length}`);
  
  console.log("\n" + "=".repeat(70));
  console.log("✅ SIMULATION COMPLETE!");
  console.log("=".repeat(70));
  console.log("\n🎉 All blockchain operations executed successfully!");
  console.log("📋 All data is immutable and transparent on the blockchain\n");
}

main()
  .then(() => process.exit(0))
  .catch((error) => {
    console.error(error);
    process.exit(1);
  });
