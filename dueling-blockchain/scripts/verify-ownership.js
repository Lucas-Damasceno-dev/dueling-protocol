/**
 * Script de Verificação de Propriedade de Cartas
 * 
 * Este script permite que qualquer jogador verifique independentemente
 * quais cartas NFT possui na blockchain.
 * 
 * Uso:
 *   npx hardhat run scripts/verify-ownership.js --network localhost
 *   npx hardhat run scripts/verify-ownership.js --network sepolia
 * 
 * Configure seu endereço na linha 22
 */

const hre = require("hardhat");

async function main() {
  console.log("🔍 VERIFICAÇÃO DE PROPRIEDADE DE CARTAS NFT");
  console.log("=" + "=".repeat(69) + "\n");

  // ==========================================
  // CONFIGURE SEU ENDEREÇO AQUI
  // ==========================================
  const playerAddress = process.env.PLAYER_ADDRESS || "0x70997970C51812dc3A010C7d01b50e0d17dc79C8";
  
  // Load deployed addresses
  let assetAddress;
  try {
    const fs = require('fs');
    const deployment = JSON.parse(fs.readFileSync('./deployment-info.json', 'utf8'));
    assetAddress = process.env.ASSET_CONTRACT || deployment.contracts.AssetContract;
  } catch (error) {
    // Fallback to default address if file not found
    assetAddress = process.env.ASSET_CONTRACT || "0x5FbDB2315678afecb367f032d93F642f64180aa3";
  }
  
  console.log("📋 Configuração:");
  console.log("   Jogador:", playerAddress);
  console.log("   Contrato AssetContract:", assetAddress);
  console.log("   Rede:", hre.network.name);
  console.log();

  // Conectar ao contrato
  const AssetContract = await hre.ethers.getContractFactory("AssetContract");
  const asset = AssetContract.attach(assetAddress);
  
  try {
    // Verificar quantas cartas o jogador possui
    const cards = await asset.getPlayerCards(playerAddress);
    const totalSupply = await asset.getTotalCards();
    
    console.log("📊 Estatísticas:");
    console.log("   Total de cartas no jogo:", totalSupply.toString());
    console.log("   Suas cartas:", cards.length);
    console.log();
    
    if (cards.length === 0) {
      console.log("⚠️  Você não possui nenhuma carta na blockchain.");
      console.log();
      console.log("📌 IMPORTANTE: O jogo atual NÃO integra automaticamente com blockchain!");
      console.log("   - Compras no jogo são registradas apenas no PostgreSQL");
      console.log("   - Para testar a blockchain, use: npm run simulate:purchase");
      console.log("   - Ou crie transações manualmente via scripts/simulate-*.js");
      console.log();
      return;
    }
    
    console.log("🃏 SUAS CARTAS:");
    console.log("─".repeat(70) + "\n");
    
    // Listar todas as cartas
    for (let i = 0; i < cards.length; i++) {
      const tokenId = cards[i];
      const card = await asset.getCard(tokenId);
      const owner = await asset.ownerOf(tokenId);
      
      // Verificar se o jogador realmente é o dono
      const isOwner = owner.toLowerCase() === playerAddress.toLowerCase();
      const statusIcon = isOwner ? "✅" : "⚠️";
      
      console.log(`${statusIcon} Carta #${tokenId}`);
      console.log(`   ├─ Tipo: ${card.cardType}`);
      console.log(`   ├─ Raridade: ${card.rarity.toString()} ${"⭐".repeat(Number(card.rarity))}`);
      console.log(`   ├─ ATK: ${card.attack.toString().padEnd(4)} / DEF: ${card.defense.toString()}`);
      console.log(`   ├─ Proprietário: ${owner}`);
      console.log(`   └─ Criado em: ${formatTimestamp(card.mintedAt)}`);
      
      if (!isOwner) {
        console.log(`   ⚠️  AVISO: A propriedade não corresponde!`);
        console.log(`       Esperado: ${playerAddress}`);
        console.log(`       Atual: ${owner}`);
      }
      
      console.log();
    }
    
    // Resumo
    console.log("─".repeat(70));
    console.log("✅ Verificação concluída com sucesso!");
    console.log();
    
    // Análise de raridade
    const rarityCount = {};
    for (let tokenId of cards) {
      const card = await asset.getCard(tokenId);
      const rarity = card.rarity.toString();
      rarityCount[rarity] = (rarityCount[rarity] || 0) + 1;
    }
    
    console.log("📈 Distribuição de Raridade:");
    for (let rarity = 1; rarity <= 5; rarity++) {
      const count = rarityCount[rarity.toString()] || 0;
      const stars = "⭐".repeat(rarity);
      const bar = "█".repeat(count);
      console.log(`   ${stars.padEnd(10)} ${bar} ${count}`);
    }
    
    console.log();
    console.log("🔗 Próximos Passos:");
    console.log("   1. Veja suas cartas no Etherscan:");
    console.log(`      https://sepolia.etherscan.io/token/${assetAddress}?a=${playerAddress}`);
    console.log("   2. Adicione no MetaMask para visualizar como NFTs");
    console.log("   3. Use scripts/verify-uniqueness.js para verificar uma carta específica");
    
  } catch (error) {
    console.error("❌ Erro ao verificar propriedade:", error.message);
    
    if (error.message.includes("could not decode result data")) {
      console.log("\n💡 Dica: Verifique se o endereço do contrato está correto.");
    }
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
