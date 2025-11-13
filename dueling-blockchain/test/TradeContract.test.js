const { expect } = require("chai");
const { ethers } = require("hardhat");

describe("TradeContract", function () {
  let assetContract, tradeContract;
  let owner, player1, player2;

  beforeEach(async function () {
    [owner, player1, player2] = await ethers.getSigners();
    
    // Deploy AssetContract
    const AssetContract = await ethers.getContractFactory("AssetContract");
    assetContract = await AssetContract.deploy();
    await assetContract.waitForDeployment();
    
    // Deploy TradeContract
    const TradeContract = await ethers.getContractFactory("TradeContract");
    tradeContract = await TradeContract.deploy(await assetContract.getAddress());
    await tradeContract.waitForDeployment();
    
    // Mint cards for both players
    await assetContract.mintCard(player1.address, "Monster", 5, 3000, 2500); // tokenId 0
    await assetContract.mintCard(player1.address, "Spell", 3, 0, 0);         // tokenId 1
    await assetContract.mintCard(player2.address, "Trap", 4, 0, 2000);       // tokenId 2
    await assetContract.mintCard(player2.address, "Monster", 2, 1500, 1000); // tokenId 3
  });

  describe("Deployment", function () {
    it("Should link to asset contract", async function () {
      expect(await tradeContract.assetContract()).to.equal(await assetContract.getAddress());
    });
  });

  describe("Propose Trade", function () {
    it("Should propose a trade", async function () {
      await tradeContract.connect(player1).proposeTrade(
        player2.address,
        [0], // player1 offers card 0
        [2]  // player2 offers card 2
      );
      
      const trade = await tradeContract.getTrade(0);
      expect(trade.proposer).to.equal(player1.address);
      expect(trade.acceptor).to.equal(player2.address);
      expect(trade.isActive).to.be.true;
      expect(trade.isCompleted).to.be.false;
    });

    it("Should fail if proposer doesn't own card", async function () {
      await expect(
        tradeContract.connect(player1).proposeTrade(
          player2.address,
          [2], // player1 doesn't own card 2
          [0]
        )
      ).to.be.revertedWith("Proposer does not own card");
    });

    it("Should fail if acceptor doesn't own card", async function () {
      await expect(
        tradeContract.connect(player1).proposeTrade(
          player2.address,
          [0],
          [1] // player2 doesn't own card 1
        )
      ).to.be.revertedWith("Acceptor does not own card");
    });

    it("Should fail trading with self", async function () {
      await expect(
        tradeContract.connect(player1).proposeTrade(
          player1.address,
          [0],
          [1]
        )
      ).to.be.revertedWith("Cannot trade with yourself");
    });

    it("Should emit TradeProposed event", async function () {
      await expect(
        tradeContract.connect(player1).proposeTrade(
          player2.address,
          [0],
          [2]
        )
      )
        .to.emit(tradeContract, "TradeProposed")
        .withArgs(0, player1.address, player2.address, [0], [2]);
    });

    it("Should allow multi-card trades", async function () {
      await tradeContract.connect(player1).proposeTrade(
        player2.address,
        [0, 1], // player1 offers 2 cards
        [2, 3]  // player2 offers 2 cards
      );
      
      const trade = await tradeContract.getTrade(0);
      expect(trade.proposerCards.length).to.equal(2);
      expect(trade.acceptorCards.length).to.equal(2);
    });
  });

  describe("Accept Trade", function () {
    beforeEach(async function () {
      // Approve TradeContract to transfer cards
      await assetContract.connect(player1).setApprovalForAll(await tradeContract.getAddress(), true);
      await assetContract.connect(player2).setApprovalForAll(await tradeContract.getAddress(), true);
      
      // Propose a trade
      await tradeContract.connect(player1).proposeTrade(
        player2.address,
        [0],
        [2]
      );
    });

    it("Should execute trade atomically", async function () {
      await tradeContract.connect(player2).acceptTrade(0);
      
      // Verify ownership changed
      expect(await assetContract.ownerOf(0)).to.equal(player2.address);
      expect(await assetContract.ownerOf(2)).to.equal(player1.address);
    });

    it("Should mark trade as completed", async function () {
      await tradeContract.connect(player2).acceptTrade(0);
      
      const trade = await tradeContract.getTrade(0);
      expect(trade.isCompleted).to.be.true;
      expect(trade.isActive).to.be.false;
    });

    it("Should fail if non-acceptor tries to accept", async function () {
      await expect(
        tradeContract.connect(player1).acceptTrade(0)
      ).to.be.revertedWith("Not the acceptor");
    });

    it("Should fail if trade is not active", async function () {
      await tradeContract.connect(player1).cancelTrade(0);
      
      await expect(
        tradeContract.connect(player2).acceptTrade(0)
      ).to.be.revertedWith("Trade not active");
    });

    it("Should fail if already completed", async function () {
      await tradeContract.connect(player2).acceptTrade(0);
      
      await expect(
        tradeContract.connect(player2).acceptTrade(0)
      ).to.be.revertedWith("Trade not active");
    });

    it("Should emit TradeAccepted event", async function () {
      await expect(
        tradeContract.connect(player2).acceptTrade(0)
      )
        .to.emit(tradeContract, "TradeAccepted")
        .withArgs(0, player1.address, player2.address);
    });

    it("Should handle multi-card trades", async function () {
      // Propose multi-card trade
      await assetContract.connect(player1).setApprovalForAll(await tradeContract.getAddress(), true);
      await assetContract.connect(player2).setApprovalForAll(await tradeContract.getAddress(), true);
      
      await tradeContract.connect(player1).proposeTrade(
        player2.address,
        [0, 1],
        [2, 3]
      );
      
      await tradeContract.connect(player2).acceptTrade(1);
      
      // Verify all ownerships changed
      expect(await assetContract.ownerOf(0)).to.equal(player2.address);
      expect(await assetContract.ownerOf(1)).to.equal(player2.address);
      expect(await assetContract.ownerOf(2)).to.equal(player1.address);
      expect(await assetContract.ownerOf(3)).to.equal(player1.address);
    });
  });

  describe("Cancel Trade", function () {
    beforeEach(async function () {
      await tradeContract.connect(player1).proposeTrade(
        player2.address,
        [0],
        [2]
      );
    });

    it("Should cancel trade", async function () {
      await tradeContract.connect(player1).cancelTrade(0);
      
      const trade = await tradeContract.getTrade(0);
      expect(trade.isActive).to.be.false;
    });

    it("Should fail if non-proposer tries to cancel", async function () {
      await expect(
        tradeContract.connect(player2).cancelTrade(0)
      ).to.be.revertedWith("Not the proposer");
    });

    it("Should fail if already cancelled", async function () {
      await tradeContract.connect(player1).cancelTrade(0);
      
      await expect(
        tradeContract.connect(player1).cancelTrade(0)
      ).to.be.revertedWith("Trade not active");
    });

    it("Should emit TradeCancelled event", async function () {
      await expect(
        tradeContract.connect(player1).cancelTrade(0)
      )
        .to.emit(tradeContract, "TradeCancelled")
        .withArgs(0, player1.address);
    });
  });

  describe("Get Player Trades", function () {
    it("Should return all trades for a player", async function () {
      await tradeContract.connect(player1).proposeTrade(player2.address, [0], [2]);
      await tradeContract.connect(player2).proposeTrade(player1.address, [2], [1]);
      
      const player1Trades = await tradeContract.getPlayerTrades(player1.address);
      const player2Trades = await tradeContract.getPlayerTrades(player2.address);
      
      expect(player1Trades.length).to.equal(2);
      expect(player2Trades.length).to.equal(2);
    });
  });

  describe("Get Active Trades", function () {
    it("Should return only active trades", async function () {
      await tradeContract.connect(player1).proposeTrade(player2.address, [0], [2]);
      await tradeContract.connect(player1).proposeTrade(player2.address, [1], [3]);
      await tradeContract.connect(player1).cancelTrade(0);
      
      const activeTrades = await tradeContract.getActiveTrades(player1.address);
      expect(activeTrades.length).to.equal(1);
      expect(activeTrades[0]).to.equal(1);
    });
  });
});
