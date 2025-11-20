// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

import "@openzeppelin/contracts/token/ERC721/ERC721.sol";
import "@openzeppelin/contracts/token/ERC721/extensions/ERC721Enumerable.sol";
import "@openzeppelin/contracts/access/Ownable.sol";

/**
 * @title AssetContract
 * @dev NFT contract for managing unique dueling cards
 */
contract AssetContract is ERC721Enumerable, Ownable {
    uint256 private _tokenIdCounter;
    
    // Authorized minters mapping
    mapping(address => bool) public authorizedMinters;
    
    struct Card {
        string cardName;     // Unique card identifier (e.g., "basic-0", "dragon-flame")
        string cardType;     // "Monster", "Spell", "Trap"
        uint8 rarity;        // 1-5 (stars)
        uint16 attack;       // 0-3000
        uint16 defense;      // 0-2500
        uint256 mintedAt;    // Timestamp
    }
    
    mapping(uint256 => Card) public cards;
    
    event CardMinted(
        uint256 indexed tokenId,
        address indexed owner,
        string cardName,
        string cardType,
        uint8 rarity,
        uint16 attack,
        uint16 defense
    );
    
    event CardTransferred(
        uint256 indexed tokenId,
        address indexed from,
        address indexed to
    );
    
    constructor() ERC721("DuelingCard", "DCARD") Ownable(msg.sender) {
        // Deployer is automatically an authorized minter
        authorizedMinters[msg.sender] = true;
    }
    
    /**
     * @dev Authorize an address to mint cards
     */
    function authorizeMinter(address minter) public onlyOwner {
        authorizedMinters[minter] = true;
    }
    
    /**
     * @dev Revoke minting authorization
     */
    function revokeMinter(address minter) public onlyOwner {
        authorizedMinters[minter] = false;
    }
    
    /**
     * @dev Modifier to restrict function to authorized minters
     */
    modifier onlyMinter() {
        require(authorizedMinters[msg.sender] || msg.sender == owner(), "Not authorized to mint");
        _;
    }
    
    /**
     * @dev Mint a new card (only authorized minters can call)
     */
    function mintCard(
        address to,
        string memory cardName,
        string memory cardType,
        uint8 rarity,
        uint16 attack,
        uint16 defense
    ) public onlyMinter returns (uint256) {
        require(rarity >= 1 && rarity <= 5, "Invalid rarity");
        require(attack <= 3000, "Attack too high");
        require(defense <= 2500, "Defense too high");
        
        uint256 tokenId = _tokenIdCounter;
        _tokenIdCounter++;
        
        _safeMint(to, tokenId);
        
        cards[tokenId] = Card({
            cardName: cardName,
            cardType: cardType,
            rarity: rarity,
            attack: attack,
            defense: defense,
            mintedAt: block.timestamp
        });
        
        emit CardMinted(tokenId, to, cardName, cardType, rarity, attack, defense);
        
        return tokenId;
    }
    
    /**
     * @dev Transfer card to another address
     */
    function transferCard(address to, uint256 tokenId) public {
        require(ownerOf(tokenId) == msg.sender, "Not the owner");
        
        address from = msg.sender;
        safeTransferFrom(from, to, tokenId);
        
        emit CardTransferred(tokenId, from, to);
    }
    
    /**
     * @dev Get all cards owned by a player
     */
    function getPlayerCards(address player) public view returns (uint256[] memory) {
        uint256 balance = balanceOf(player);
        uint256[] memory result = new uint256[](balance);
        
        for (uint256 i = 0; i < balance; i++) {
            result[i] = tokenOfOwnerByIndex(player, i);
        }
        
        return result;
    }
    
    /**
     * @dev Get card details
     */
    function getCard(uint256 tokenId) public view returns (Card memory) {
        require(_ownerOf(tokenId) != address(0), "Card does not exist");
        return cards[tokenId];
    }
    
    /**
     * @dev Get total number of cards minted
     */
    function getTotalCards() public view returns (uint256) {
        return _tokenIdCounter;
    }
    
    /**
     * @dev Facilitate card transfers for trades (only authorized minters)
     * This allows the server to execute atomic trades without requiring individual approvals
     */
    function facilitateTransfer(address from, address to, uint256 tokenId) public onlyMinter {
        require(ownerOf(tokenId) == from, "From address does not own the token");
        _transfer(from, to, tokenId);
        emit CardTransferred(tokenId, from, to);
    }
}
