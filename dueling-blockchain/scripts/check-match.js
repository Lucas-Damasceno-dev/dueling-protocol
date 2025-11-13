const hre = require("hardhat");

async function main() {
  const matchContract = await hre.ethers.getContractAt("MatchContract", "0xDc64a140Aa3E981100a9becA4E685f962f0cF6C9");
  
  const totalMatches = await matchContract.getTotalMatches();
  console.log("Current total matches:", totalMatches.toString());
  
  if (totalMatches > 0) {
    const match0 = await matchContract.getMatch(0);
    console.log("Match 0:", match0);
  }
}

main().then(() => process.exit(0)).catch(error => { console.error(error); process.exit(1); });
