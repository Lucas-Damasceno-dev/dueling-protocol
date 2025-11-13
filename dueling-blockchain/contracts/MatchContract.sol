// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

/**
 * @title MatchContract
 * @dev Records immutable match results on blockchain
 */
contract MatchContract {
    uint256 private _matchIdCounter;
    
    address public gameServer; // Authorized server to record matches
    
    struct Match {
        uint256 matchId;
        address player1;
        address player2;
        address winner;
        uint256 timestamp;
        bytes32 gameStateHash;  // Proof of play
        uint8 player1Score;
        uint8 player2Score;
    }
    
    mapping(uint256 => Match) public matches;
    mapping(address => uint256[]) public playerMatches;
    mapping(address => uint256) public wins;
    mapping(address => uint256) public totalMatches;
    
    event MatchRecorded(
        uint256 indexed matchId,
        address indexed player1,
        address indexed player2,
        address winner,
        bytes32 gameStateHash,
        uint8 player1Score,
        uint8 player2Score
    );
    
    event GameServerUpdated(address indexed oldServer, address indexed newServer);
    
    modifier onlyGameServer() {
        require(msg.sender == gameServer, "Only game server can call");
        _;
    }
    
    constructor(address _gameServer) {
        require(_gameServer != address(0), "Invalid game server");
        gameServer = _gameServer;
    }
    
    /**
     * @dev Record a match result (only game server)
     */
    function recordMatch(
        address player1,
        address player2,
        address winner,
        bytes32 gameStateHash,
        uint8 player1Score,
        uint8 player2Score
    ) public onlyGameServer returns (uint256) {
        require(player1 != address(0), "Invalid player1");
        require(player2 != address(0), "Invalid player2");
        require(player1 != player2, "Players must be different");
        require(winner == player1 || winner == player2, "Invalid winner");
        require(gameStateHash != bytes32(0), "Invalid game state hash");
        
        uint256 matchId = _matchIdCounter;
        _matchIdCounter++;
        
        matches[matchId] = Match({
            matchId: matchId,
            player1: player1,
            player2: player2,
            winner: winner,
            timestamp: block.timestamp,
            gameStateHash: gameStateHash,
            player1Score: player1Score,
            player2Score: player2Score
        });
        
        playerMatches[player1].push(matchId);
        playerMatches[player2].push(matchId);
        
        totalMatches[player1]++;
        totalMatches[player2]++;
        wins[winner]++;
        
        emit MatchRecorded(
            matchId,
            player1,
            player2,
            winner,
            gameStateHash,
            player1Score,
            player2Score
        );
        
        return matchId;
    }
    
    /**
     * @dev Get match history for a player
     */
    function getPlayerMatchHistory(address player) public view returns (uint256[] memory) {
        return playerMatches[player];
    }
    
    /**
     * @dev Get match details
     */
    function getMatch(uint256 matchId) public view returns (Match memory) {
        require(matchId < _matchIdCounter, "Match does not exist");
        return matches[matchId];
    }
    
    /**
     * @dev Get player statistics
     */
    function getPlayerStats(address player) public view returns (
        uint256 totalGames,
        uint256 totalWins,
        uint256 totalLosses
    ) {
        totalGames = totalMatches[player];
        totalWins = wins[player];
        totalLosses = totalGames - totalWins;
        
        return (totalGames, totalWins, totalLosses);
    }
    
    /**
     * @dev Get win rate (in basis points, 10000 = 100%)
     */
    function getWinRate(address player) public view returns (uint256) {
        if (totalMatches[player] == 0) {
            return 0;
        }
        
        return (wins[player] * 10000) / totalMatches[player];
    }
    
    /**
     * @dev Get total matches recorded
     */
    function getTotalMatches() public view returns (uint256) {
        return _matchIdCounter;
    }
    
    /**
     * @dev Update game server address (only current game server)
     */
    function updateGameServer(address newGameServer) public onlyGameServer {
        require(newGameServer != address(0), "Invalid game server");
        
        address oldServer = gameServer;
        gameServer = newGameServer;
        
        emit GameServerUpdated(oldServer, newGameServer);
    }
    
    /**
     * @dev Verify game state hash
     */
    function verifyGameState(
        uint256 matchId,
        bytes32 providedHash
    ) public view returns (bool) {
        require(matchId < _matchIdCounter, "Match does not exist");
        return matches[matchId].gameStateHash == providedHash;
    }
}
