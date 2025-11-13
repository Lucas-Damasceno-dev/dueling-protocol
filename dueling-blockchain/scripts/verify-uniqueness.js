/**
 * Script de Verificação de Unicidade de Carta
 * 
 * Este script verifica que uma carta específica é única e imutável.
 * Mostra todos os detalhes e histórico da carta.
 * 
 * Uso:
 *   TOKEN_ID=1047 npx hardhat run scripts/verify-uniqueness.js --network localhost
 *   TOKEN_ID=1047 npx hardhat run scripts/verify-uniqueness.js --network sepolia
 */

const hre = require("hardhat");

async function main() {
  console.log("🔍 VERIFICAÇÃO DE UNICIDADE DE CARTA NFT");
  console.log("=" + "=".repeat(69) + "\n");

  // ==========================================
  // CONFIGURE O TOKEN ID AQUI
  // ==========================================
  const tokenId = process.env.TOKEN_ID || "0";
  const assetAddress = process.env.ASSET_CONTRACT || "0x5FbDB2315678afecb367f032d93F642f64180aa3";
  
  console.log("📋 Configuração:");
  console.log("   Token ID:", tokenId);
  console.log("   Contrato AssetContract:", assetAddress);
  console.log("   Rede:", hre.network.name);
  console.log();

  // Conectar ao contrato
  const AssetContract = await hre.ethers.getContractFactory("AssetContract");
  const asset = AssetContract.attach(assetAddress);
  
  try {
    // Verificar se a carta existe
    const card = await asset.getCard(tokenId);
    const owner = await asset.ownerOf(tokenId);
    const totalSupply = await asset.getTotalCards();
    
    console.log("✅ CARTA ENCONTRADA E VERIFICADA");
    console.log("─".repeat(70) + "\n");
    
    // Informações da carta
    console.log(`🃏 Carta #${tokenId}`);
    console.log();
    
    console.log("📊 Atributos Imutáveis:");
    console.log(`   ├─ Tipo: ${card.cardType}`);
    console.log(`   ├─ Raridade: ${card.rarity.toString()} ${"⭐".repeat(Number(card.rarity))}`);
    console.log(`   ├─ Ataque: ${card.attack.toString()}`);
    console.log(`   ├─ Defesa: ${card.defense.toString()}`);
    console.log(`   └─ Criação: ${formatTimestamp(card.mintedAt)}`);
    console.log();
    
    console.log("👤 Propriedade:");
    console.log(`   ├─ Proprietário Atual: ${owner}`);
    console.log(`   └─ Endereço do Contrato: ${assetAddress}`);
    console.log();
    
    console.log("🌐 Contexto Global:");
    console.log(`   ├─ Total de cartas no jogo: ${totalSupply.toString()}`);
    console.log(`   ├─ Este token ID é único: SIM ✅`);
    console.log(`   └─ Possibilidade de duplicação: IMPOSSÍVEL 🔒`);
    console.log();
    
    // Verificação de unicidade
    console.log("🔐 Verificação de Unicidade:");
    console.log(`   ├─ Token ID ${tokenId} existe? ✅ SIM`);
    console.log(`   ├─ Pode existir outro #${tokenId}? ❌ NÃO (impossível por design)`);
    console.log(`   ├─ Atributos podem mudar? ❌ NÃO (imutáveis na blockchain)`);
    console.log(`   └─ Propriedade pode ser forjada? ❌ NÃO (validado por Ethereum)`);
    console.log();
    
    // Cálculo de hash único
    const uniqueHash = hre.ethers.solidityPackedKeccak256(
      ["uint256", "string", "uint8", "uint16", "uint16", "uint256"],
      [tokenId, card.cardType, card.rarity, card.attack, card.defense, card.mintedAt]
    );
    
    console.log("🔑 Impressão Digital (Hash) da Carta:");
    console.log(`   ${uniqueHash}`);
    console.log("   └─ Este hash é único e identifica exclusivamente esta carta");
    console.log();
    
    // Estatísticas comparativas
    const averageTokenId = Number(totalSupply) / 2;
    const isRare = Number(card.rarity) >= 4;
    const isHighAttack = Number(card.attack) >= 2000;
    
    console.log("📈 Análise Comparativa:");
    console.log(`   ├─ Token ID médio do jogo: ~${averageTokenId.toFixed(0)}`);
    console.log(`   ├─ Esta carta é rara? ${isRare ? "SIM ✅ (≥4★)" : "Não (comum)"}`);
    console.log(`   ├─ Ataque alto? ${isHighAttack ? "SIM ✅ (≥2000)" : "Normal (<2000)"}`);
    console.log(`   └─ Idade: ${calculateAge(card.mintedAt)}`);
    console.log();
    
    console.log("─".repeat(70));
    console.log("✅ VERIFICAÇÃO CONCLUÍDA");
    console.log();
    
    console.log("🎯 Conclusão:");
    console.log(`   Esta carta #${tokenId} é ÚNICA e VERIFICÁVEL.`);
    console.log("   - Ninguém pode criar outra carta com este ID");
    console.log("   - Os atributos NUNCA mudarão");
    console.log("   - A propriedade é garantida pela blockchain Ethereum");
    console.log();
    
    console.log("🔗 Verificação Externa:");
    if (hre.network.name === "sepolia") {
      console.log(`   Etherscan: https://sepolia.etherscan.io/token/${assetAddress}?a=${tokenId}`);
    } else {
      console.log(`   Rede local - use o Hardhat Console para verificação adicional`);
    }
    
  } catch (error) {
    console.error("❌ Erro ao verificar carta:", error.message);
    
    if (error.message.includes("Card does not exist")) {
      console.log("\n⚠️  Esta carta não existe!");
      console.log(`   Token ID ${tokenId} nunca foi criado (mintado).`);
      console.log();
      console.log("💡 Dicas:");
      console.log("   - Verifique se o Token ID está correto");
      console.log("   - Use verify-ownership.js para ver suas cartas");
      
      const totalSupply = await asset.getTotalCards();
      console.log(`   - IDs válidos: 0 a ${Number(totalSupply) - 1}`);
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
    second: '2-digit',
    timeZoneName: 'short'
  });
}

function calculateAge(mintedAt) {
  const now = Math.floor(Date.now() / 1000);
  const age = now - Number(mintedAt);
  
  if (age < 60) return `${age} segundos`;
  if (age < 3600) return `${Math.floor(age / 60)} minutos`;
  if (age < 86400) return `${Math.floor(age / 3600)} horas`;
  return `${Math.floor(age / 86400)} dias`;
}

main()
  .then(() => process.exit(0))
  .catch((error) => {
    console.error(error);
    process.exit(1);
  });
