const hre = require("hardhat");

async function main() {
  const addresses = JSON.parse(require('fs').readFileSync('deployment-info.json', 'utf8'));
  console.log("Match contract:", addresses.matchContract);
  
  const MatchContract = await hre.ethers.getContractFactory("MatchContract");
  const contract = MatchContract.attach(addresses.matchContract);
  
  const [signer] = await hre.ethers.getSigners();
  console.log("Signer:", signer.address);
  console.log("Game server:", await contract.gameServer());
  
  // Test recordMatch
  const player1 = "0x70997970C51812dc3A010C7d01b50e0d17dc79C8";
  const player2 = "0x3C44CdDdB6a900fa2b585dd299e03d12FA4293BC";
  const gameHash = hre.ethers.id("test-match-123");
  
  try {
    const tx = await contract.recordMatch(player1, player2, player1, gameHash, 10, 5);
    console.log("✅ Transaction sent:", tx.hash);
    await tx.wait();
    console.log("✅ Match recorded!");
  } catch (error) {
    console.error("❌ Error:", error.message);
  }
}

main().catch(console.error);
