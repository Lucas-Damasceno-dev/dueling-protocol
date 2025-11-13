const hre = require("hardhat");

async function main() {
  const [owner] = await hre.ethers.getSigners();
  console.log("Testing with account:", owner.address);
  
  const matchContract = await hre.ethers.getContractAt("MatchContract", "0xDc64a140Aa3E981100a9becA4E685f962f0cF6C9");
  
  const gameServer = await matchContract.gameServer();
  console.log("Contract game server:", gameServer);

  const player1 = "0x70997970C51812dc3A010C7d01b50e0d17dc79C8";
  const player2 = "0x3C44CdDdB6a900fa2b585dd299e03d12FA4293BC";
  const winner = player2;
  const gameStateHash = hre.ethers.keccak256(hre.ethers.toUtf8Bytes("test-match-123"));
  
  try {
    console.log("\nCalling recordMatch...");
    const tx = await matchContract.recordMatch(player1, player2, winner, gameStateHash, 0, 100);
    const receipt = await tx.wait();
    console.log("✅ Match recorded successfully!");
    console.log("TX Hash:", tx.hash);
    console.log("Block:", receipt.blockNumber);
  } catch (error) {
    console.log("❌ Error:", error.shortMessage || error.message);
    if (error.data) {
      console.log("Error data:", error.data);
    }
  }
}

main().catch(console.error);
