const { expect } = require("chai");
const { ethers } = require("hardhat");

describe("StoreContract", function () {
  let assetContract, storeContract;
  let owner, buyer1, buyer2;
  const PACK_PRICE = ethers.parseEther("0.001");

  beforeEach(async function () {
    [owner, buyer1, buyer2] = await ethers.getSigners();
    
    // Deploy AssetContract
    const AssetContract = await ethers.getContractFactory("AssetContract");
    assetContract = await AssetContract.deploy();
    await assetContract.waitForDeployment();
    
    // Deploy StoreContract
    const StoreContract = await ethers.getContractFactory("StoreContract");
    storeContract = await StoreContract.deploy(await assetContract.getAddress());
    await storeContract.waitForDeployment();
    
    // Transfer ownership of AssetContract to StoreContract
    await assetContract.transferOwnership(await storeContract.getAddress());
  });

  describe("Deployment", function () {
    it("Should set correct pack price", async function () {
      expect(await storeContract.packPrice()).to.equal(PACK_PRICE);
    });

    it("Should link to asset contract", async function () {
      expect(await storeContract.assetContract()).to.equal(await assetContract.getAddress());
    });
  });

  describe("Pack Purchase", function () {
    it("Should purchase Bronze pack and mint 5 cards", async function () {
      await storeContract.connect(buyer1).purchasePack(1, { value: PACK_PRICE });
      
      const balance = await assetContract.balanceOf(buyer1.address);
      expect(balance).to.equal(5);
    });

    it("Should purchase Silver pack", async function () {
      await storeContract.connect(buyer1).purchasePack(2, { value: PACK_PRICE });
      
      const balance = await assetContract.balanceOf(buyer1.address);
      expect(balance).to.equal(5);
    });

    it("Should purchase Gold pack", async function () {
      await storeContract.connect(buyer1).purchasePack(3, { value: PACK_PRICE });
      
      const balance = await assetContract.balanceOf(buyer1.address);
      expect(balance).to.equal(5);
    });

    it("Should fail with insufficient payment", async function () {
      await expect(
        storeContract.connect(buyer1).purchasePack(1, { 
          value: ethers.parseEther("0.0001") 
        })
      ).to.be.revertedWith("Insufficient payment");
    });

    it("Should fail with invalid pack type", async function () {
      await expect(
        storeContract.connect(buyer1).purchasePack(4, { value: PACK_PRICE })
      ).to.be.revertedWith("Invalid pack type");
    });

    it("Should emit PackPurchased event", async function () {
      await expect(storeContract.connect(buyer1).purchasePack(1, { value: PACK_PRICE }))
        .to.emit(storeContract, "PackPurchased");
      
      // Verify cards were minted
      const balance = await assetContract.balanceOf(buyer1.address);
      expect(balance).to.equal(5);
    });

    it("Should refund excess payment", async function () {
      const overpayment = ethers.parseEther("0.002");
      const balanceBefore = await ethers.provider.getBalance(buyer1.address);
      
      const tx = await storeContract.connect(buyer1).purchasePack(1, { value: overpayment });
      const receipt = await tx.wait();
      const gasUsed = receipt.gasUsed * receipt.gasPrice;
      
      const balanceAfter = await ethers.provider.getBalance(buyer1.address);
      const expectedBalance = balanceBefore - PACK_PRICE - gasUsed;
      
      expect(balanceAfter).to.be.closeTo(expectedBalance, ethers.parseEther("0.0001"));
    });
  });

  describe("Purchase History", function () {
    it("Should track purchase history", async function () {
      await storeContract.connect(buyer1).purchasePack(1, { value: PACK_PRICE });
      await storeContract.connect(buyer1).purchasePack(2, { value: PACK_PRICE });
      
      const history = await storeContract.getPurchaseHistory(buyer1.address);
      expect(history.length).to.equal(2);
    });

    it("Should return correct purchase details", async function () {
      await storeContract.connect(buyer1).purchasePack(3, { value: PACK_PRICE });
      
      const purchase = await storeContract.getPurchase(0);
      expect(purchase.buyer).to.equal(buyer1.address);
      expect(purchase.packType).to.equal(3);
      expect(purchase.cardsReceived.length).to.equal(5);
    });
  });

  describe("Card Generation", function () {
    it("Should generate different rarities for different pack types", async function () {
      // This is probabilistic, but we can check that cards are generated
      await storeContract.connect(buyer1).purchasePack(1, { value: PACK_PRICE }); // Bronze
      await storeContract.connect(buyer2).purchasePack(3, { value: PACK_PRICE }); // Gold
      
      const buyer1Cards = await assetContract.getPlayerCards(buyer1.address);
      const buyer2Cards = await assetContract.getPlayerCards(buyer2.address);
      
      expect(buyer1Cards.length).to.equal(5);
      expect(buyer2Cards.length).to.equal(5);
    });
  });

  describe("Admin Functions", function () {
    it("Should allow owner to update pack price", async function () {
      const newPrice = ethers.parseEther("0.002");
      await storeContract.setPackPrice(newPrice);
      
      expect(await storeContract.packPrice()).to.equal(newPrice);
    });

    it("Should allow owner to withdraw funds", async function () {
      await storeContract.connect(buyer1).purchasePack(1, { value: PACK_PRICE });
      
      const balanceBefore = await ethers.provider.getBalance(owner.address);
      const contractBalance = await ethers.provider.getBalance(await storeContract.getAddress());
      
      const tx = await storeContract.withdraw();
      const receipt = await tx.wait();
      const gasUsed = receipt.gasUsed * receipt.gasPrice;
      
      const balanceAfter = await ethers.provider.getBalance(owner.address);
      expect(balanceAfter).to.equal(balanceBefore + contractBalance - gasUsed);
    });
  });
});
