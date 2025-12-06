// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

import "@openzeppelin/contracts/access/Ownable.sol";

/**
 * @title OracleRegistry
 * @dev Manages oracle nodes and their reputation
 */
contract OracleRegistry is Ownable {
    
    struct Oracle {
        address oracleAddress;
        string serverUrl;
        string name;
        uint256 registeredAt;
        uint256 proofsSubmitted;
        uint256 proofsVerified;
        uint256 proofsRejected;
        bool active;
    }
    
    mapping(address => Oracle) public oracles;
    address[] public oracleAddresses;
    
    event OracleRegistered(address indexed oracle, string name, string serverUrl);
    event OracleDeactivated(address indexed oracle);
    event OracleReactivated(address indexed oracle);
    event ProofSubmitted(address indexed oracle);
    event ProofVerified(address indexed oracle);
    event ProofRejected(address indexed oracle);
    
    constructor() Ownable(msg.sender) {}
    
    /**
     * @dev Register a new oracle
     */
    function registerOracle(
        address oracleAddress,
        string memory name,
        string memory serverUrl
    ) external onlyOwner {
        require(oracleAddress != address(0), "Invalid address");
        require(oracles[oracleAddress].registeredAt == 0, "Oracle already registered");
        
        oracles[oracleAddress] = Oracle({
            oracleAddress: oracleAddress,
            serverUrl: serverUrl,
            name: name,
            registeredAt: block.timestamp,
            proofsSubmitted: 0,
            proofsVerified: 0,
            proofsRejected: 0,
            active: true
        });
        
        oracleAddresses.push(oracleAddress);
        
        emit OracleRegistered(oracleAddress, name, serverUrl);
    }
    
    /**
     * @dev Deactivate an oracle
     */
    function deactivateOracle(address oracleAddress) external onlyOwner {
        require(oracles[oracleAddress].registeredAt != 0, "Oracle not registered");
        oracles[oracleAddress].active = false;
        emit OracleDeactivated(oracleAddress);
    }
    
    /**
     * @dev Reactivate an oracle
     */
    function reactivateOracle(address oracleAddress) external onlyOwner {
        require(oracles[oracleAddress].registeredAt != 0, "Oracle not registered");
        oracles[oracleAddress].active = true;
        emit OracleReactivated(oracleAddress);
    }
    
    /**
     * @dev Record proof submission
     */
    function recordProofSubmission(address oracleAddress) external onlyOwner {
        require(oracles[oracleAddress].active, "Oracle not active");
        oracles[oracleAddress].proofsSubmitted++;
        emit ProofSubmitted(oracleAddress);
    }
    
    /**
     * @dev Record proof verification
     */
    function recordProofVerification(address oracleAddress) external onlyOwner {
        require(oracles[oracleAddress].active, "Oracle not active");
        oracles[oracleAddress].proofsVerified++;
        emit ProofVerified(oracleAddress);
    }
    
    /**
     * @dev Record proof rejection
     */
    function recordProofRejection(address oracleAddress) external onlyOwner {
        require(oracles[oracleAddress].active, "Oracle not active");
        oracles[oracleAddress].proofsRejected++;
        emit ProofRejected(oracleAddress);
    }
    
    /**
     * @dev Get oracle count
     */
    function getOracleCount() external view returns (uint256) {
        return oracleAddresses.length;
    }
    
    /**
     * @dev Get oracle reputation
     */
    function getReputation(address oracleAddress) 
        external 
        view 
        returns (
            uint256 submitted,
            uint256 verified,
            uint256 rejected,
            uint256 successRate
        ) 
    {
        Oracle memory oracle = oracles[oracleAddress];
        require(oracle.registeredAt != 0, "Oracle not registered");
        
        submitted = oracle.proofsSubmitted;
        verified = oracle.proofsVerified;
        rejected = oracle.proofsRejected;
        
        if (submitted > 0) {
            successRate = (verified * 100) / submitted;
        } else {
            successRate = 0;
        }
        
        return (submitted, verified, rejected, successRate);
    }
    
    /**
     * @dev Check if oracle is active
     */
    function isActiveOracle(address oracleAddress) external view returns (bool) {
        return oracles[oracleAddress].active && oracles[oracleAddress].registeredAt != 0;
    }
}
