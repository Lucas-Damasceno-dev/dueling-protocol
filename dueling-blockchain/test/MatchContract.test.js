const { expect } = require("chai");
const { ethers } = require("hardhat");

describe("MatchContract", function () {
  let matchContract;
  let owner, gameServer, player1, player2;

  beforeEach(async function () {
    [owner, gameServer, player1, player2] = await ethers.getSigners();
    
    const MatchContract = await ethers.getContractFactory("MatchContract");
    matchContract = await MatchContract.deploy(gameServer.address);
    await matchContract.waitForDeployment();
  });

  describe("Deployment", function () {
    it("Should set the right game server", async function () {
      expect(await matchContract.gameServer()).to.equal(gameServer.address);
    });
  });

  describe("Record Match", function () {
    const gameStateHash = ethers.keccak256(ethers.toUtf8Bytes("game_state_data"));

    it("Should record a match", async function () {
      await matchContract.connect(gameServer).recordMatch(
        player1.address,
        player2.address,
        player1.address, // winner
        gameStateHash,
        10, // player1Score
        8   // player2Score
      );
      
      const match = await matchContract.getMatch(0);
      expect(match.player1).to.equal(player1.address);
      expect(match.player2).to.equal(player2.address);
      expect(match.winner).to.equal(player1.address);
      expect(match.gameStateHash).to.equal(gameStateHash);
      expect(match.player1Score).to.equal(10);
      expect(match.player2Score).to.equal(8);
    });

    it("Should fail if non-game-server tries to record", async function () {
      await expect(
        matchContract.connect(player1).recordMatch(
          player1.address,
          player2.address,
          player1.address,
          gameStateHash,
          10,
          8
        )
      ).to.be.revertedWith("Only game server can call");
    });

    it("Should fail with invalid winner", async function () {
      await expect(
        matchContract.connect(gameServer).recordMatch(
          player1.address,
          player2.address,
          owner.address, // invalid winner
          gameStateHash,
          10,
          8
        )
      ).to.be.revertedWith("Invalid winner");
    });

    it("Should fail with same player", async function () {
      await expect(
        matchContract.connect(gameServer).recordMatch(
          player1.address,
          player1.address,
          player1.address,
          gameStateHash,
          10,
          8
        )
      ).to.be.revertedWith("Players must be different");
    });

    it("Should fail with zero hash", async function () {
      await expect(
        matchContract.connect(gameServer).recordMatch(
          player1.address,
          player2.address,
          player1.address,
          ethers.ZeroHash,
          10,
          8
        )
      ).to.be.revertedWith("Invalid game state hash");
    });

    it("Should emit MatchRecorded event", async function () {
      await expect(
        matchContract.connect(gameServer).recordMatch(
          player1.address,
          player2.address,
          player1.address,
          gameStateHash,
          10,
          8
        )
      )
        .to.emit(matchContract, "MatchRecorded")
        .withArgs(0, player1.address, player2.address, player1.address, gameStateHash, 10, 8);
    });

    it("Should update player statistics", async function () {
      await matchContract.connect(gameServer).recordMatch(
        player1.address,
        player2.address,
        player1.address,
        gameStateHash,
        10,
        8
      );
      
      expect(await matchContract.totalMatches(player1.address)).to.equal(1);
      expect(await matchContract.totalMatches(player2.address)).to.equal(1);
      expect(await matchContract.wins(player1.address)).to.equal(1);
      expect(await matchContract.wins(player2.address)).to.equal(0);
    });
  });

  describe("Match History", function () {
    const gameStateHash = ethers.keccak256(ethers.toUtf8Bytes("game_state_data"));

    beforeEach(async function () {
      // Record multiple matches
      await matchContract.connect(gameServer).recordMatch(
        player1.address, player2.address, player1.address, gameStateHash, 10, 8
      );
      await matchContract.connect(gameServer).recordMatch(
        player1.address, player2.address, player2.address, gameStateHash, 8, 10
      );
      await matchContract.connect(gameServer).recordMatch(
        player1.address, player2.address, player1.address, gameStateHash, 12, 6
      );
    });

    it("Should return player match history", async function () {
      const player1History = await matchContract.getPlayerMatchHistory(player1.address);
      const player2History = await matchContract.getPlayerMatchHistory(player2.address);
      
      expect(player1History.length).to.equal(3);
      expect(player2History.length).to.equal(3);
    });

    it("Should return correct player stats", async function () {
      const [totalMatches, wins] = await matchContract.getPlayerStats(player1.address);
      
      expect(totalMatches).to.equal(3);
      expect(wins).to.equal(2);
    });

    it("Should calculate win rate correctly", async function () {
      const winRate = await matchContract.getWinRate(player1.address);
      // 2 wins out of 3 matches = 66.66% = 6666 basis points
      expect(winRate).to.equal(6666);
    });

    it("Should return 0 win rate for player with no matches", async function () {
      const winRate = await matchContract.getWinRate(owner.address);
      expect(winRate).to.equal(0);
    });
  });

  describe("Get Total Matches", function () {
    it("Should return total matches recorded", async function () {
      const gameStateHash = ethers.keccak256(ethers.toUtf8Bytes("game_state_data"));
      
      await matchContract.connect(gameServer).recordMatch(
        player1.address, player2.address, player1.address, gameStateHash, 10, 8
      );
      await matchContract.connect(gameServer).recordMatch(
        player1.address, player2.address, player2.address, gameStateHash, 8, 10
      );
      
      expect(await matchContract.getTotalMatches()).to.equal(2);
    });
  });

  describe("Update Game Server", function () {
    it("Should update game server", async function () {
      const newGameServer = player1.address;
      
      await matchContract.connect(gameServer).updateGameServer(newGameServer);
      
      expect(await matchContract.gameServer()).to.equal(newGameServer);
    });

    it("Should fail if non-game-server tries to update", async function () {
      await expect(
        matchContract.connect(player1).updateGameServer(player1.address)
      ).to.be.revertedWith("Only game server can call");
    });

    it("Should emit GameServerUpdated event", async function () {
      const newGameServer = player1.address;
      
      await expect(
        matchContract.connect(gameServer).updateGameServer(newGameServer)
      )
        .to.emit(matchContract, "GameServerUpdated")
        .withArgs(gameServer.address, newGameServer);
    });
  });

  describe("Verify Game State", function () {
    const gameStateHash = ethers.keccak256(ethers.toUtf8Bytes("game_state_data"));

    beforeEach(async function () {
      await matchContract.connect(gameServer).recordMatch(
        player1.address,
        player2.address,
        player1.address,
        gameStateHash,
        10,
        8
      );
    });

    it("Should verify correct game state hash", async function () {
      const isValid = await matchContract.verifyGameState(0, gameStateHash);
      expect(isValid).to.be.true;
    });

    it("Should reject incorrect game state hash", async function () {
      const wrongHash = ethers.keccak256(ethers.toUtf8Bytes("wrong_data"));
      const isValid = await matchContract.verifyGameState(0, wrongHash);
      expect(isValid).to.be.false;
    });

    it("Should fail for non-existent match", async function () {
      await expect(
        matchContract.verifyGameState(999, gameStateHash)
      ).to.be.revertedWith("Match does not exist");
    });
  });

  describe("Immutability", function () {
    const gameStateHash = ethers.keccak256(ethers.toUtf8Bytes("game_state_data"));

    it("Match data should be immutable once recorded", async function () {
      await matchContract.connect(gameServer).recordMatch(
        player1.address,
        player2.address,
        player1.address,
        gameStateHash,
        10,
        8
      );
      
      const matchBefore = await matchContract.getMatch(0);
      
      // Try to record another match (shouldn't affect the first)
      await matchContract.connect(gameServer).recordMatch(
        player1.address,
        player2.address,
        player2.address,
        gameStateHash,
        8,
        10
      );
      
      const matchAfter = await matchContract.getMatch(0);
      
      // First match should remain unchanged
      expect(matchAfter.winner).to.equal(matchBefore.winner);
      expect(matchAfter.player1Score).to.equal(matchBefore.player1Score);
      expect(matchAfter.gameStateHash).to.equal(matchBefore.gameStateHash);
    });
  });
});
