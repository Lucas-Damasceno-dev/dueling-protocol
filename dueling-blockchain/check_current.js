const { ethers } = require("hardhat");

async function main() {
    const deployedAddresses = {
        AssetContract: "0x5FbDB2315678afecb367f032d93F642f64180aa3",
        StoreContract: "0xe7f1725E7734CE288F8367e1Bb143E90bb3F0512",
        TradeContract: "0xCf7Ed3AccA5a467e9e704C703E8D87F634fB0Fc9"
    };
    
    const AssetContract = await ethers.getContractAt("AssetContract", deployedAddresses.AssetContract);
    const StoreContract = await ethers.getContractAt("StoreContract", deployedAddresses.StoreContract);
    const TradeContract = await ethers.getContractAt("TradeContract", deployedAddresses.TradeContract);
    
    console.log("=== VERIFICAÇÃO DO LEDGER DISTRIBUÍDO ===\n");
    
    try {
        const totalCards = await AssetContract.getTotalCards();
        console.log(`🎴 Total de cartas mintadas: ${totalCards}\n`);
        
        if (totalCards > 0) {
            console.log("Primeiras 15 cartas:");
            for (let i = 0; i < Math.min(15, totalCards); i++) {
                const card = await AssetContract.getCard(i);
                const owner = await AssetContract.ownerOf(i);
                console.log(`  #${i}: ${card.name} | Dono: ${owner.substring(0, 10)}...`);
            }
        }
    } catch (e) {
        console.log(`❌ Erro ao acessar AssetContract: ${e.message}`);
    }
    
    // Check recent purchases
    const [owner] = await ethers.getSigners();
    try {
        const purchases = await StoreContract.getPurchaseHistory(owner.address);
        console.log(`\n📦 Compras registradas: ${purchases.length}`);
        
        for (let i = 0; i < Math.min(5, purchases.length); i++) {
            const purchase = await StoreContract.getPurchase(purchases[i]);
            console.log(`\n  Compra #${purchases[i]}:`);
            console.log(`    Comprador: ${purchase.buyer.substring(0, 10)}...`);
            console.log(`    Tipo: ${purchase.packType}`);
            console.log(`    Cartas: ${purchase.cardIds.length}`);
        }
    } catch (e) {
        console.log(`\n❌ Erro ao acessar StoreContract: ${e.message}`);
    }
    
    // Check trades
    try {
        const trades = await TradeContract.getPlayerTrades(owner.address);
        console.log(`\n🔄 Trocas registradas: ${trades.length}`);
        
        for (let i = 0; i < Math.min(3, trades.length); i++) {
            const trade = await TradeContract.getTrade(trades[i]);
            console.log(`\n  Troca #${trades[i]}:`);
            console.log(`    Iniciador: ${trade.initiator.substring(0, 10)}...`);
            console.log(`    Parceiro: ${trade.partner.substring(0, 10)}...`);
            console.log(`    Status: ${trade.status}`);
        }
    } catch (e) {
        console.log(`\n❌ Erro ao acessar TradeContract: ${e.message}`);
    }
    
    console.log("\n✅ VERIFICAÇÃO CONCLUÍDA");
}

main()
    .then(() => process.exit(0))
    .catch((error) => {
        console.error(error);
        process.exit(1);
    });
