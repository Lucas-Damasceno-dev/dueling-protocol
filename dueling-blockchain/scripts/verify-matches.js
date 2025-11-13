/**
 * Script de Verificação de Resultados de Partidas
 * 
 * Este script mostra o histórico de partidas registradas na blockchain
 * para um jogador específico.
 * 
 * Uso:
 *   PLAYER_ADDRESS=0xSeuEndereço npm run verify:matches
 */

const hre = require("hardhat");

async function main() {
  console.log("🔍 VERIFICAÇÃO DE RESULTADOS DE PARTIDAS");
  console.log("=" + "=".repeat(69) + "\n");

  // ==========================================
  // CONFIGURE SEU ENDEREÇO AQUI
  // ==========================================
  const playerAddress = process.env.PLAYER_ADDRESS || "0x70997970C51812dc3A010C7d01b50e0d17dc79C8";
  
  // Load deployed addresses
  let matchAddress;
  try {
    const fs = require('fs');
    const deployment = JSON.parse(fs.readFileSync('./deployment-info.json', 'utf8'));
    matchAddress = process.env.MATCH_CONTRACT || deployment.contracts.MatchContract;
  } catch (error) {
    // Fallback to default address if file not found
    matchAddress = process.env.MATCH_CONTRACT || "0xDc64a140Aa3E981100a9becA4E685f962f0cF6C9";
  }
  
  console.log("📋 Configuração:");
  console.log("   Jogador:", playerAddress);
  console.log("   Contrato MatchContract:", matchAddress);
  console.log("   Rede:", hre.network.name);
  console.log();

  // Conectar ao contrato
  const MatchContract = await hre.ethers.getContractFactory("MatchContract");
  const match = MatchContract.attach(matchAddress);
  
  try {
    // Buscar estatísticas do jogador
    const stats = await match.getPlayerStats(playerAddress);
    
    console.log("📊 ESTATÍSTICAS DO JOGADOR\n");
    
    const totalMatches = Number(stats.wins) + Number(stats.losses) + Number(stats.draws);
    const winRate = totalMatches > 0 ? (Number(stats.winRate) / 100).toFixed(2) : "0.00";
    
    console.log("📈 Resumo:");
    console.log(`   ├─ Total de Partidas: ${totalMatches}`);
    console.log(`   ├─ Vitórias: ${stats.wins.toString()} ✅`);
    console.log(`   ├─ Derrotas: ${stats.losses.toString()} ❌`);
    console.log(`   ├─ Empates: ${stats.draws.toString()} 🤝`);
    console.log(`   └─ Win Rate: ${winRate}%`);
    console.log();
    
    if (totalMatches === 0) {
      console.log("⚠️  Nenhuma partida registrada na blockchain.");
      console.log();
      console.log("📌 IMPORTANTE: O jogo atual NÃO integra automaticamente com blockchain!");
      console.log("   - Partidas do jogo são registradas apenas no PostgreSQL");
      console.log("   - Para testar a blockchain, use: npm run simulate:match");
      console.log("   - Ou execute: node scripts/simulate-match.js");
      console.log();
      return;
    }
    
    // Criar gráfico de barras
    console.log("📊 Distribuição de Resultados:");
    const maxBar = 30;
    const maxValue = Math.max(Number(stats.wins), Number(stats.losses), Number(stats.draws));
    
    if (Number(stats.wins) > 0) {
      const barLength = Math.floor((Number(stats.wins) / maxValue) * maxBar);
      console.log(`   Vitórias  ${"█".repeat(barLength).padEnd(maxBar)} ${stats.wins}`);
    }
    if (Number(stats.losses) > 0) {
      const barLength = Math.floor((Number(stats.losses) / maxValue) * maxBar);
      console.log(`   Derrotas  ${"█".repeat(barLength).padEnd(maxBar)} ${stats.losses}`);
    }
    if (Number(stats.draws) > 0) {
      const barLength = Math.floor((Number(stats.draws) / maxValue) * maxBar);
      console.log(`   Empates   ${"█".repeat(barLength).padEnd(maxBar)} ${stats.draws}`);
    }
    console.log();
    
    // Performance
    console.log("🎯 Performance:");
    if (Number(stats.wins) > Number(stats.losses)) {
      console.log("   Status: ✅ POSITIVO - Mais vitórias que derrotas");
    } else if (Number(stats.losses) > Number(stats.wins)) {
      console.log("   Status: ⚠️  NEGATIVO - Mais derrotas que vitórias");
    } else {
      console.log("   Status: 🤝 EQUILIBRADO - Empate técnico");
    }
    
    const winRateNum = parseFloat(winRate);
    if (winRateNum >= 70) {
      console.log("   Nível: 🏆 EXCEPCIONAL");
    } else if (winRateNum >= 55) {
      console.log("   Nível: ⭐ BOM");
    } else if (winRateNum >= 45) {
      console.log("   Nível: 📊 MÉDIO");
    } else {
      console.log("   Nível: 📉 PRECISA MELHORAR");
    }
    console.log();
    
    console.log("─".repeat(70));
    console.log("✅ VERIFICAÇÃO CONCLUÍDA\n");
    
    console.log("🔗 Verificação Externa:");
    if (hre.network.name === "sepolia") {
      console.log(`   Contract: https://sepolia.etherscan.io/address/${matchAddress}`);
      console.log(`   Player: https://sepolia.etherscan.io/address/${playerAddress}`);
    } else {
      console.log("   Rede local - verifique os logs do Hardhat node");
    }
    console.log();
    
    console.log("💡 Transparência:");
    console.log("   - Estes resultados estão registrados permanentemente");
    console.log("   - Apenas o servidor autorizado pode registrar partidas");
    console.log("   - Qualquer pessoa pode verificar estes dados");
    console.log("   - Os resultados são imutáveis e auditáveis");
    
  } catch (error) {
    console.error("❌ Erro ao verificar partidas:", error.message);
    
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
