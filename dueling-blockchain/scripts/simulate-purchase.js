/**
 * Script de Simulação de Compra
 * 
 * Este script simula uma compra de pacote na blockchain para fins de teste.
 * Use este script para popular a blockchain com dados de teste.
 */

const hre = require("hardhat");

async function main() {
  console.log("🎮 SIMULAÇÃO DE COMPRA DE PACOTE\n");
  
  const [deployer, player1] = await hre.ethers.getSigners();
  
  // Load deployed addresses
  const fs = require('fs');
  const deployment = JSON.parse(fs.readFileSync('./deployment-info.json', 'utf8'));
  
  const assetAddress = deployment.contracts.AssetContract;
  const storeAddress = deployment.contracts.StoreContract;
  
  console.log("📋 Configuração:");
  console.log("   Comprador:", player1.address);
  console.log("   Saldo:", hre.ethers.formatEther(await hre.ethers.provider.getBalance(player1.address)), "ETH");
  console.log();
  
  // Connect to contracts
  const StoreContract = await hre.ethers.getContractFactory("StoreContract");
  const store = StoreContract.attach(storeAddress);
  
  const AssetContract = await hre.ethers.getContractFactory("AssetContract");
  const asset = AssetContract.attach(assetAddress);
  
  // Get pack price
  const packPrice = await store.packPrice();
  console.log("💰 Preço do pacote:", hre.ethers.formatEther(packPrice), "ETH");
  console.log();
  
  console.log("🛒 Comprando pacote...");
  const tx = await store.connect(player1).purchasePack(1, { value: packPrice });
  const receipt = await tx.wait();
  
  // Find CardMinted events
  const mintEvents = receipt.logs.filter(log => {
    try {
      const parsed = asset.interface.parseLog(log);
      return parsed && parsed.name === 'CardMinted';
    } catch {
      return false;
    }
  }).map(log => asset.interface.parseLog(log));
  
  console.log("✅ Pacote comprado com sucesso!");
  console.log("   Transaction Hash:", receipt.hash);
  console.log("   Gas Used:", receipt.gasUsed.toString());
  console.log();
  
  console.log("🎴 Cartas recebidas:");
  for (const event of mintEvents) {
    const tokenId = event.args.tokenId;
    const card = await asset.getCard(tokenId);
    console.log(`   ├─ Carta #${tokenId}`);
    console.log(`   │  ├─ Tipo: ${card.cardType}`);
    console.log(`   │  ├─ Raridade: ${"⭐".repeat(Number(card.rarity))}`);
    console.log(`   │  └─ ATK: ${card.attack} / DEF: ${card.defense}`);
  }
  
  console.log();
  console.log("📊 Estatísticas atualizadas:");
  const playerCards = await asset.getPlayerCards(player1.address);
  const purchases = await store.getPurchaseHistory(player1.address);
  console.log("   Total de cartas:", playerCards.length);
  console.log("   Total de compras:", purchases.length);
  console.log();
  console.log("✅ Simulação completa! Agora execute:");
  console.log(`   PLAYER_ADDRESS=${player1.address} npm run verify:ownership`);
}

main()
  .then(() => process.exit(0))
  .catch((error) => {
    console.error(error);
    process.exit(1);
  });
