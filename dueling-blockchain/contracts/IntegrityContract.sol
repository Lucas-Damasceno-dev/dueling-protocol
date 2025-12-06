// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

import "@openzeppelin/contracts/access/Ownable.sol";

/**
 * @title IntegrityContract
 * @dev Oracle pattern: Stores cryptographic proofs of all operations
 * Backend acts as oracle, blockchain provides tamper-evident audit log
 */
contract IntegrityContract is Ownable {
    
    enum OperationType {
        PURCHASE,
        TRADE,
        MATCH,
        USER_REGISTRATION
    }
    
    struct OperationProof {
        bytes32 dataHash;           // SHA256 hash of operation data
        address oracle;              // Oracle that submitted the proof
        uint256 timestamp;           // Block timestamp
        OperationType opType;        // Type of operation
        string operationId;          // External ID (for correlation with DB)
        bool verified;               // Additional verification flag
    }
    
    // Mapping: operationId => OperationProof
    mapping(string => OperationProof) public proofs;
    
    // Mapping: oracle address => authorized
    mapping(address => bool) public authorizedOracles;
    
    // Sequential proof counter for enumeration
    uint256 public proofCount;
    mapping(uint256 => string) public proofIds;
    
    // Events
    event ProofRecorded(
        string indexed operationId,
        bytes32 indexed dataHash,
        OperationType opType,
        address oracle,
        uint256 timestamp
    );
    
    event OracleAuthorized(address indexed oracle);
    event OracleRevoked(address indexed oracle);
    event ProofVerified(string indexed operationId, address verifier);
    
    constructor() Ownable(msg.sender) {
        // Owner (deployer) is automatically authorized
        authorizedOracles[msg.sender] = true;
        emit OracleAuthorized(msg.sender);
    }
    
    modifier onlyAuthorizedOracle() {
        require(authorizedOracles[msg.sender], "Not an authorized oracle");
        _;
    }
    
    /**
     * @dev Authorize a new oracle
     */
    function authorizeOracle(address oracle) external onlyOwner {
        require(oracle != address(0), "Invalid oracle address");
        authorizedOracles[oracle] = true;
        emit OracleAuthorized(oracle);
    }
    
    /**
     * @dev Revoke oracle authorization
     */
    function revokeOracle(address oracle) external onlyOwner {
        authorizedOracles[oracle] = false;
        emit OracleRevoked(oracle);
    }
    
    /**
     * @dev Record a proof for a purchase operation
     * @param operationId Unique identifier (e.g., "purchase-123")
     * @param dataHash SHA256 hash of operation data
     * @param playerAddress Player's blockchain address
     */
    function recordPurchaseProof(
        string memory operationId,
        bytes32 dataHash,
        address playerAddress
    ) external onlyAuthorizedOracle {
        require(proofs[operationId].timestamp == 0, "Proof already exists");
        require(dataHash != bytes32(0), "Invalid hash");
        
        proofs[operationId] = OperationProof({
            dataHash: dataHash,
            oracle: msg.sender,
            timestamp: block.timestamp,
            opType: OperationType.PURCHASE,
            operationId: operationId,
            verified: false
        });
        
        proofIds[proofCount] = operationId;
        proofCount++;
        
        emit ProofRecorded(operationId, dataHash, OperationType.PURCHASE, msg.sender, block.timestamp);
    }
    
    /**
     * @dev Record a proof for a trade operation
     */
    function recordTradeProof(
        string memory operationId,
        bytes32 dataHash,
        address player1,
        address player2
    ) external onlyAuthorizedOracle {
        require(proofs[operationId].timestamp == 0, "Proof already exists");
        require(dataHash != bytes32(0), "Invalid hash");
        
        proofs[operationId] = OperationProof({
            dataHash: dataHash,
            oracle: msg.sender,
            timestamp: block.timestamp,
            opType: OperationType.TRADE,
            operationId: operationId,
            verified: false
        });
        
        proofIds[proofCount] = operationId;
        proofCount++;
        
        emit ProofRecorded(operationId, dataHash, OperationType.TRADE, msg.sender, block.timestamp);
    }
    
    /**
     * @dev Record a proof for a match operation
     */
    function recordMatchProof(
        string memory operationId,
        bytes32 dataHash,
        address winner,
        address loser
    ) external onlyAuthorizedOracle {
        require(proofs[operationId].timestamp == 0, "Proof already exists");
        require(dataHash != bytes32(0), "Invalid hash");
        
        proofs[operationId] = OperationProof({
            dataHash: dataHash,
            oracle: msg.sender,
            timestamp: block.timestamp,
            opType: OperationType.MATCH,
            operationId: operationId,
            verified: false
        });
        
        proofIds[proofCount] = operationId;
        proofCount++;
        
        emit ProofRecorded(operationId, dataHash, OperationType.MATCH, msg.sender, block.timestamp);
    }
    
    /**
     * @dev Verify an operation proof by comparing hashes
     * @param operationId The operation to verify
     * @param providedHash The hash to compare against
     * @return matches True if hashes match
     */
    function verifyProof(
        string memory operationId,
        bytes32 providedHash
    ) external view returns (bool matches) {
        OperationProof memory proof = proofs[operationId];
        require(proof.timestamp != 0, "Proof does not exist");
        
        return proof.dataHash == providedHash;
    }
    
    /**
     * @dev Mark a proof as verified (e.g., after external audit)
     */
    function markAsVerified(string memory operationId) external onlyOwner {
        require(proofs[operationId].timestamp != 0, "Proof does not exist");
        proofs[operationId].verified = true;
        emit ProofVerified(operationId, msg.sender);
    }
    
    /**
     * @dev Get proof details
     */
    function getProof(string memory operationId) 
        external 
        view 
        returns (
            bytes32 dataHash,
            address oracle,
            uint256 timestamp,
            OperationType opType,
            bool verified
        ) 
    {
        OperationProof memory proof = proofs[operationId];
        require(proof.timestamp != 0, "Proof does not exist");
        
        return (
            proof.dataHash,
            proof.oracle,
            proof.timestamp,
            proof.opType,
            proof.verified
        );
    }
    
    /**
     * @dev Get all proofs (paginated)
     */
    function getProofsByRange(uint256 start, uint256 end) 
        external 
        view 
        returns (string[] memory) 
    {
        require(start < end, "Invalid range");
        require(end <= proofCount, "End exceeds proof count");
        
        uint256 length = end - start;
        string[] memory ids = new string[](length);
        
        for (uint256 i = 0; i < length; i++) {
            ids[i] = proofIds[start + i];
        }
        
        return ids;
    }
}
