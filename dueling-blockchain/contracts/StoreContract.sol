// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

import "./AssetContract.sol";
import "@openzeppelin/contracts/utils/ReentrancyGuard.sol";
import "@openzeppelin/contracts/access/Ownable.sol";

/**
 * @title StoreContract
 * @dev Manages pack purchases and prevents double-spending
 */
contract StoreContract is ReentrancyGuard, Ownable {
    AssetContract public assetContract;
    
    uint256 public packPrice = 0.001 ether;
    uint256 private _purchaseIdCounter;
    
    uint8 public constant CARDS_PER_PACK = 5;
    
    struct PackPurchase {
        uint256 purchaseId;
        address buyer;
        uint8 packType;         // 1=Bronze, 2=Silver, 3=Gold
        uint256 timestamp;
        uint256[] cardsReceived;
    }
    
    mapping(uint256 => PackPurchase) public purchases;
    mapping(address => uint256[]) public playerPurchases;
    
    event PackPurchased(
        uint256 indexed purchaseId,
        address indexed buyer,
        uint8 packType,
        uint256[] cardsReceived
    );
    
    constructor(address _assetContract) Ownable(msg.sender) {
        assetContract = AssetContract(_assetContract);
    }
    
    /**
     * @dev Purchase a pack and receive cards
     * @param packType 1=Bronze, 2=Silver, 3=Gold
     */
    function purchasePack(uint8 packType) public payable nonReentrant returns (uint256) {
        require(msg.value >= packPrice, "Insufficient payment");
        require(packType >= 1 && packType <= 3, "Invalid pack type");
        
        uint256 purchaseId = _purchaseIdCounter;
        _purchaseIdCounter++;
        
        uint256[] memory cardsReceived = new uint256[](CARDS_PER_PACK);
        
        // Generate cards based on pack type (simplified randomness)
        for (uint8 i = 0; i < CARDS_PER_PACK; i++) {
            (string memory cardType, uint8 rarity, uint16 attack, uint16 defense) = 
                _generateCard(packType, i, purchaseId);
            
            uint256 tokenId = assetContract.mintCard(
                msg.sender,
                cardType,
                rarity,
                attack,
                defense
            );
            
            cardsReceived[i] = tokenId;
        }
        
        purchases[purchaseId] = PackPurchase({
            purchaseId: purchaseId,
            buyer: msg.sender,
            packType: packType,
            timestamp: block.timestamp,
            cardsReceived: cardsReceived
        });
        
        playerPurchases[msg.sender].push(purchaseId);
        
        emit PackPurchased(purchaseId, msg.sender, packType, cardsReceived);
        
        // Refund excess payment
        if (msg.value > packPrice) {
            payable(msg.sender).transfer(msg.value - packPrice);
        }
        
        return purchaseId;
    }
    
    /**
     * @dev Generate card attributes (simplified pseudo-random)
     * NOTE: This is NOT cryptographically secure randomness
     * For production, use Chainlink VRF or similar oracle
     */
    function _generateCard(
        uint8 packType,
        uint8 cardIndex,
        uint256 purchaseId
    ) private view returns (
        string memory cardType,
        uint8 rarity,
        uint16 attack,
        uint16 defense
    ) {
        uint256 seed = uint256(keccak256(abi.encodePacked(
            block.timestamp,
            msg.sender,
            purchaseId,
            cardIndex
        )));
        
        // Determine card type (60% Monster, 25% Spell, 15% Trap)
        uint8 typeRoll = uint8(seed % 100);
        if (typeRoll < 60) {
            cardType = "Monster";
        } else if (typeRoll < 85) {
            cardType = "Spell";
        } else {
            cardType = "Trap";
        }
        
        // Rarity based on pack type (Gold > Silver > Bronze)
        uint8 rarityRoll = uint8((seed / 100) % 100);
        if (packType == 3) { // Gold
            if (rarityRoll < 50) rarity = 5;
            else if (rarityRoll < 80) rarity = 4;
            else rarity = 3;
        } else if (packType == 2) { // Silver
            if (rarityRoll < 30) rarity = 4;
            else if (rarityRoll < 70) rarity = 3;
            else rarity = 2;
        } else { // Bronze
            if (rarityRoll < 20) rarity = 3;
            else if (rarityRoll < 60) rarity = 2;
            else rarity = 1;
        }
        
        // Attack and defense scaled by rarity
        attack = uint16((seed / 10000) % (rarity * 600));
        defense = uint16((seed / 100000) % (rarity * 500));
        
        return (cardType, rarity, attack, defense);
    }
    
    /**
     * @dev Get purchase history for a player
     */
    function getPurchaseHistory(address player) public view returns (uint256[] memory) {
        return playerPurchases[player];
    }
    
    /**
     * @dev Get purchase details
     */
    function getPurchase(uint256 purchaseId) public view returns (PackPurchase memory) {
        return purchases[purchaseId];
    }
    
    /**
     * @dev Update pack price (only owner)
     */
    function setPackPrice(uint256 newPrice) public onlyOwner {
        packPrice = newPrice;
    }
    
    /**
     * @dev Withdraw funds (only owner)
     */
    function withdraw() public onlyOwner {
        payable(msg.sender).transfer(address(this).balance);
    }
}
