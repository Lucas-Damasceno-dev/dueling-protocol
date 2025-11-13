// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

import "./AssetContract.sol";

/**
 * @title TradeContract
 * @dev Manages atomic card trades between players
 */
contract TradeContract {
    AssetContract public assetContract;
    
    uint256 private _tradeIdCounter;
    
    struct Trade {
        uint256 tradeId;
        address proposer;
        address acceptor;
        uint256[] proposerCards;
        uint256[] acceptorCards;
        bool isActive;
        bool isCompleted;
        uint256 createdAt;
        uint256 completedAt;
    }
    
    mapping(uint256 => Trade) public trades;
    mapping(address => uint256[]) public playerTrades;
    
    event TradeProposed(
        uint256 indexed tradeId,
        address indexed proposer,
        address indexed acceptor,
        uint256[] proposerCards,
        uint256[] acceptorCards
    );
    
    event TradeAccepted(
        uint256 indexed tradeId,
        address indexed proposer,
        address indexed acceptor
    );
    
    event TradeCancelled(
        uint256 indexed tradeId,
        address indexed proposer
    );
    
    constructor(address _assetContract) {
        assetContract = AssetContract(_assetContract);
    }
    
    /**
     * @dev Propose a trade
     */
    function proposeTrade(
        address acceptor,
        uint256[] memory proposerCards,
        uint256[] memory acceptorCards
    ) public returns (uint256) {
        require(acceptor != address(0), "Invalid acceptor");
        require(acceptor != msg.sender, "Cannot trade with yourself");
        require(proposerCards.length > 0 || acceptorCards.length > 0, "Empty trade");
        
        // Validate proposer owns all their cards
        for (uint256 i = 0; i < proposerCards.length; i++) {
            require(
                assetContract.ownerOf(proposerCards[i]) == msg.sender,
                "Proposer does not own card"
            );
        }
        
        // Validate acceptor owns all their cards
        for (uint256 i = 0; i < acceptorCards.length; i++) {
            require(
                assetContract.ownerOf(acceptorCards[i]) == acceptor,
                "Acceptor does not own card"
            );
        }
        
        uint256 tradeId = _tradeIdCounter;
        _tradeIdCounter++;
        
        trades[tradeId] = Trade({
            tradeId: tradeId,
            proposer: msg.sender,
            acceptor: acceptor,
            proposerCards: proposerCards,
            acceptorCards: acceptorCards,
            isActive: true,
            isCompleted: false,
            createdAt: block.timestamp,
            completedAt: 0
        });
        
        playerTrades[msg.sender].push(tradeId);
        playerTrades[acceptor].push(tradeId);
        
        emit TradeProposed(tradeId, msg.sender, acceptor, proposerCards, acceptorCards);
        
        return tradeId;
    }
    
    /**
     * @dev Accept and execute a trade atomically
     */
    function acceptTrade(uint256 tradeId) public {
        Trade storage trade = trades[tradeId];
        
        require(trade.isActive, "Trade not active");
        require(trade.acceptor == msg.sender, "Not the acceptor");
        require(!trade.isCompleted, "Already completed");
        
        // Validate ownership hasn't changed
        for (uint256 i = 0; i < trade.proposerCards.length; i++) {
            require(
                assetContract.ownerOf(trade.proposerCards[i]) == trade.proposer,
                "Proposer no longer owns card"
            );
        }
        
        for (uint256 i = 0; i < trade.acceptorCards.length; i++) {
            require(
                assetContract.ownerOf(trade.acceptorCards[i]) == trade.acceptor,
                "Acceptor no longer owns card"
            );
        }
        
        // Execute atomic swap
        // Transfer proposer's cards to acceptor
        for (uint256 i = 0; i < trade.proposerCards.length; i++) {
            assetContract.safeTransferFrom(
                trade.proposer,
                trade.acceptor,
                trade.proposerCards[i]
            );
        }
        
        // Transfer acceptor's cards to proposer
        for (uint256 i = 0; i < trade.acceptorCards.length; i++) {
            assetContract.safeTransferFrom(
                trade.acceptor,
                trade.proposer,
                trade.acceptorCards[i]
            );
        }
        
        trade.isCompleted = true;
        trade.isActive = false;
        trade.completedAt = block.timestamp;
        
        emit TradeAccepted(tradeId, trade.proposer, trade.acceptor);
    }
    
    /**
     * @dev Cancel a trade proposal
     */
    function cancelTrade(uint256 tradeId) public {
        Trade storage trade = trades[tradeId];
        
        require(trade.proposer == msg.sender, "Not the proposer");
        require(trade.isActive, "Trade not active");
        require(!trade.isCompleted, "Already completed");
        
        trade.isActive = false;
        
        emit TradeCancelled(tradeId, msg.sender);
    }
    
    /**
     * @dev Get all trades for a player
     */
    function getPlayerTrades(address player) public view returns (uint256[] memory) {
        return playerTrades[player];
    }
    
    /**
     * @dev Get trade details
     */
    function getTrade(uint256 tradeId) public view returns (Trade memory) {
        return trades[tradeId];
    }
    
    /**
     * @dev Get active trades for a player
     */
    function getActiveTrades(address player) public view returns (uint256[] memory) {
        uint256[] memory allTrades = playerTrades[player];
        uint256 activeCount = 0;
        
        // Count active trades
        for (uint256 i = 0; i < allTrades.length; i++) {
            if (trades[allTrades[i]].isActive) {
                activeCount++;
            }
        }
        
        // Build result array
        uint256[] memory activeTrades = new uint256[](activeCount);
        uint256 index = 0;
        
        for (uint256 i = 0; i < allTrades.length; i++) {
            if (trades[allTrades[i]].isActive) {
                activeTrades[index] = allTrades[i];
                index++;
            }
        }
        
        return activeTrades;
    }
}
