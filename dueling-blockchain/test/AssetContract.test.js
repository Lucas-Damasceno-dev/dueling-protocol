const { expect } = require("chai");
const { ethers } = require("hardhat");

describe("AssetContract", function () {
  let assetContract;
  let owner, addr1, addr2;

  beforeEach(async function () {
    [owner, addr1, addr2] = await ethers.getSigners();
    
    const AssetContract = await ethers.getContractFactory("AssetContract");
    assetContract = await AssetContract.deploy();
    await assetContract.waitForDeployment();
  });

  describe("Deployment", function () {
    it("Should set the right owner", async function () {
      expect(await assetContract.owner()).to.equal(owner.address);
    });

    it("Should have correct name and symbol", async function () {
      expect(await assetContract.name()).to.equal("DuelingCard");
      expect(await assetContract.symbol()).to.equal("DCARD");
    });
  });

  describe("Minting", function () {
    it("Should mint a card", async function () {
      await assetContract.mintCard(
        addr1.address,
        "Monster",
        5, // rarity
        3000, // attack
        2500  // defense
      );

      expect(await assetContract.balanceOf(addr1.address)).to.equal(1);
      expect(await assetContract.ownerOf(0)).to.equal(addr1.address);
    });

    it("Should fail if non-owner tries to mint", async function () {
      await expect(
        assetContract.connect(addr1).mintCard(
          addr2.address,
          "Monster",
          5,
          3000,
          2500
        )
      ).to.be.reverted;
    });

    it("Should not mint with invalid rarity", async function () {
      await expect(
        assetContract.mintCard(addr1.address, "Monster", 6, 3000, 2500)
      ).to.be.revertedWith("Invalid rarity");
    });

    it("Should not mint with attack too high", async function () {
      await expect(
        assetContract.mintCard(addr1.address, "Monster", 5, 3001, 2500)
      ).to.be.revertedWith("Attack too high");
    });

    it("Should emit CardMinted event", async function () {
      await expect(
        assetContract.mintCard(addr1.address, "Spell", 3, 0, 0)
      )
        .to.emit(assetContract, "CardMinted")
        .withArgs(0, addr1.address, "Spell", 3, 0, 0);
    });
  });

  describe("Transfers", function () {
    beforeEach(async function () {
      await assetContract.mintCard(addr1.address, "Monster", 5, 3000, 2500);
    });

    it("Should transfer card", async function () {
      await assetContract.connect(addr1).transferCard(addr2.address, 0);
      
      expect(await assetContract.ownerOf(0)).to.equal(addr2.address);
      expect(await assetContract.balanceOf(addr1.address)).to.equal(0);
      expect(await assetContract.balanceOf(addr2.address)).to.equal(1);
    });

    it("Should fail if non-owner tries to transfer", async function () {
      await expect(
        assetContract.connect(addr2).transferCard(addr2.address, 0)
      ).to.be.revertedWith("Not the owner");
    });

    it("Should emit CardTransferred event", async function () {
      await expect(
        assetContract.connect(addr1).transferCard(addr2.address, 0)
      )
        .to.emit(assetContract, "CardTransferred")
        .withArgs(0, addr1.address, addr2.address);
    });
  });

  describe("Get Player Cards", function () {
    it("Should return all player cards", async function () {
      await assetContract.mintCard(addr1.address, "Monster", 5, 3000, 2500);
      await assetContract.mintCard(addr1.address, "Spell", 3, 0, 0);
      await assetContract.mintCard(addr1.address, "Trap", 2, 0, 1000);

      const cards = await assetContract.getPlayerCards(addr1.address);
      expect(cards.length).to.equal(3);
      expect(cards[0]).to.equal(0);
      expect(cards[1]).to.equal(1);
      expect(cards[2]).to.equal(2);
    });

    it("Should return empty array for player with no cards", async function () {
      const cards = await assetContract.getPlayerCards(addr1.address);
      expect(cards.length).to.equal(0);
    });
  });

  describe("Get Card Details", function () {
    it("Should return card details", async function () {
      await assetContract.mintCard(addr1.address, "Monster", 5, 3000, 2500);
      
      const card = await assetContract.getCard(0);
      expect(card.cardType).to.equal("Monster");
      expect(card.rarity).to.equal(5);
      expect(card.attack).to.equal(3000);
      expect(card.defense).to.equal(2500);
    });

    it("Should fail for non-existent card", async function () {
      await expect(
        assetContract.getCard(999)
      ).to.be.revertedWith("Card does not exist");
    });
  });
});
