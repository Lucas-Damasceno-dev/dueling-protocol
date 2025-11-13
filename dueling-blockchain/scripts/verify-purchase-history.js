/**
 * Script de Verificação de Histórico de Compras
 * 
 * Este script mostra todas as compras de pacotes que um jogador fez,
 * permitindo verificar transparência e rastreabilidade.
 * 
 * Uso:
 *   npx hardhat run scripts/verify-purchase-history.js --network localhost
 *   npx hardhat run scripts/verify-purchase-history.js --network sepolia
 */

const hre = require("hardhat");

async function main() {
  console.log("🔍 VERIFICAÇÃO DE HISTÓRICO DE COMPRAS");
  console.log("=" + "=".repeat(69) + "\n");

  // ==========================================
  // CONFIGURE SEU ENDEREÇO AQUI
  // ==========================================
  const playerAddress = process.env.PLAYER_ADDRESS || "0x70997970C51812dc3A010C7d01b50e0d17dc79C8";
  
  // Load deployed addresses
  let storeAddress, assetAddress;
  try {
    const fs = require('fs');
    const deployment = JSON.parse(fs.readFileSync('./deployment-info.json', 'utf8'));
    storeAddress = process.env.STORE_CONTRACT || deployment.contracts.StoreContract;
    assetAddress = process.env.ASSET_CONTRACT || deployment.contracts.AssetContract;
  } catch (error) {
    // Fallback to default addresses if file not found
    storeAddress = process.env.STORE_CONTRACT || "0xe7f1725E7734CE288F8367e1Bb143E90bb3F0512";
    assetAddress = process.env.ASSET_CONTRACT || "0x5FbDB2315678afecb367f032d93F642f64180aa3";
  }
  
  console.log("📋 Configuração:");
  console.log("   Jogador:", playerAddress);
  console.log("   Contrato StoreContract:", storeAddress);
  console.log("   Contrato AssetContract:", assetAddress);
  console.log("   Rede:", hre.network.name);
  console.log();

  // Conectar aos contratos
  const StoreContract = await hre.ethers.getContractFactory("StoreContract");
  const store = StoreContract.attach(storeAddress);
  
  const AssetContract = await hre.ethers.getContractFactory("AssetContract");
  const asset = AssetContract.attach(assetAddress);
  
  try {
    // Buscar histórico de compras
    const purchases = await store.getPurchaseHistory(playerAddress);
    const packPrice = await store.packPrice();
    
    console.log("📊 Estatísticas:");
    console.log("   Total de compras:", purchases.length);
    console.log("   Preço por pacote:", hre.ethers.formatEther(packPrice), "ETH");
    console.log("   Gasto total:", hre.ethers.formatEther(packPrice * BigInt(purchases.length)), "ETH");
    console.log();
    
    if (purchases.length === 0) {
      console.log("⚠️  Nenhuma compra encontrada na blockchain.");
      console.log();
      console.log("📌 IMPORTANTE: O jogo atual NÃO integra automaticamente com blockchain!");
      console.log("   - Compras no jogo são registradas apenas no PostgreSQL");
      console.log("   - Para testar a blockchain, use: npm run simulate:purchase");
      console.log("   - Ou execute: node scripts/simulate-purchase.js");
      console.log();
      return;
    }
    
    console.log("📦 HISTÓRICO DE COMPRAS:");
    console.log("─".repeat(70) + "\n");
    
    let totalCards = 0;
    const rarityStats = {};
    const typeStats = {};
    
    // Listar todas as compras
    for (let i = 0; i < purchases.length; i++) {
      const purchaseId = purchases[i];
      const purchase = await store.getPurchase(purchaseId);
      
      const packTypeNames = {
        1: "Bronze 🥉",
        2: "Silver 🥈",
        3: "Gold 🥇"
      };
      
      console.log(`📦 Compra #${purchaseId} - ${formatTimestamp(purchase.timestamp)}`);
      console.log(`   ├─ Tipo de Pacote: ${packTypeNames[purchase.packType] || "Desconhecido"}`);
      console.log(`   ├─ Comprador: ${purchase.buyer}`);
      console.log(`   ├─ Valor Pago: ${hre.ethers.formatEther(packPrice)} ETH`);
      console.log(`   └─ Cartas Recebidas (${purchase.cardsReceived.length}):`);
      
      // Detalhar cada carta recebida
      for (let j = 0; j < purchase.cardsReceived.length; j++) {
        const tokenId = purchase.cardsReceived[j];
        
        try {
          const card = await asset.getCard(tokenId);
          const owner = await asset.ownerOf(tokenId);
          
          const isPrefix = j === purchase.cardsReceived.length - 1 ? "└─" : "├─";
          const stillOwns = owner.toLowerCase() === playerAddress.toLowerCase();
          const ownershipIcon = stillOwns ? "✅" : "🔄";
          
          console.log(`      ${isPrefix} Carta #${tokenId} ${ownershipIcon}`);
          console.log(`         ├─ ${card.cardType} ${"⭐".repeat(Number(card.rarity))}`);
          console.log(`         ├─ ATK: ${card.attack} / DEF: ${card.defense}`);
          console.log(`         └─ Status: ${stillOwns ? "Ainda possui" : "Negociada/Transferida"}`);
          
          totalCards++;
          
          // Estatísticas
          const rarity = card.rarity.toString();
          rarityStats[rarity] = (rarityStats[rarity] || 0) + 1;
          typeStats[card.cardType] = (typeStats[card.cardType] || 0) + 1;
          
        } catch (error) {
          console.log(`      ${isPrefix} Carta #${tokenId} ⚠️ Erro ao buscar detalhes`);
        }
      }
      
      console.log();
    }
    
    console.log("─".repeat(70));
    console.log("📈 ESTATÍSTICAS AGREGADAS\n");
    
    console.log("🎴 Total de Cartas Adquiridas:", totalCards);
    console.log();
    
    console.log("⭐ Distribuição por Raridade:");
    for (let rarity = 1; rarity <= 5; rarity++) {
      const count = rarityStats[rarity.toString()] || 0;
      const percentage = totalCards > 0 ? ((count / totalCards) * 100).toFixed(1) : 0;
      const stars = "⭐".repeat(rarity);
      const bar = "█".repeat(Math.floor(count / 2));
      console.log(`   ${stars.padEnd(10)} ${bar.padEnd(15)} ${count} (${percentage}%)`);
    }
    console.log();
    
    console.log("🃏 Distribuição por Tipo:");
    for (const [type, count] of Object.entries(typeStats)) {
      const percentage = totalCards > 0 ? ((count / totalCards) * 100).toFixed(1) : 0;
      const bar = "█".repeat(Math.floor(count / 2));
      console.log(`   ${type.padEnd(10)} ${bar.padEnd(15)} ${count} (${percentage}%)`);
    }
    console.log();
    
    console.log("─".repeat(70));
    console.log("✅ VERIFICAÇÃO CONCLUÍDA\n");
    
    console.log("🔗 Verificação Externa:");
    if (hre.network.name === "sepolia") {
      console.log(`   Transactions: https://sepolia.etherscan.io/address/${playerAddress}`);
      console.log(`   NFTs: https://sepolia.etherscan.io/token/${assetAddress}?a=${playerAddress}`);
    } else {
      console.log("   Rede local - verifique os logs do Hardhat node");
    }
    console.log();
    
    console.log("💡 Transparência:");
    console.log("   - Todas essas transações estão registradas permanentemente");
    console.log("   - Qualquer pessoa pode verificar este histórico");
    console.log("   - Os dados são imutáveis e auditáveis");
    
  } catch (error) {
    console.error("❌ Erro ao verificar histórico:", error.message);
  }
}

function formatTimestamp(timestamp) {
  const date = new Date(Number(timestamp) * 1000);
  return date.toLocaleString('pt-BR', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  });
}

main()
  .then(() => process.exit(0))
  .catch((error) => {
    console.error(error);
    process.exit(1);
  });
