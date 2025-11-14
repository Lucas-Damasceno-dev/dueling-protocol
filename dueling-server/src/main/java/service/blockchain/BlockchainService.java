package service.blockchain;

import config.BlockchainConfig;
import model.Card;
import model.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Uint16;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.abi.datatypes.generated.Uint8;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.ContractGasProvider;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class BlockchainService {
    
    private static final Logger logger = LoggerFactory.getLogger(BlockchainService.class);
    
    private final BlockchainConfig config;
    private final Web3j web3j;
    private final ContractGasProvider gasProvider;
    private final Credentials credentials;
    private final TransactionManager transactionManager;
    private final Map<String, BigInteger> cardTokenIdMapping;

    @Autowired
    public BlockchainService(BlockchainConfig config, Web3j web3j, ContractGasProvider gasProvider) {
        this.config = config;
        this.web3j = web3j;
        this.gasProvider = gasProvider;
        this.cardTokenIdMapping = new java.util.concurrent.ConcurrentHashMap<>();
        
        // Use deployer account private key (Account #0 from Hardhat)
        String privateKey = "0xac0974bec39a17e36ba4a6b4d238ff944bacb478cbed5efcae784d7bf4f2ff80";
        this.credentials = Credentials.create(privateKey);
        
        if (config.isBlockchainEnabled() && web3j != null) {
            // Chain ID 1337 is configured in hardhat.config.js
            this.transactionManager = new RawTransactionManager(web3j, credentials, 1337L);
            logger.info("BlockchainService initialized - Blockchain integration ENABLED");
            logger.info("Using account: {}", credentials.getAddress());
            logger.info("AssetContract: {}", config.getAssetContractAddress());
        } else {
            this.transactionManager = null;
            logger.warn("BlockchainService initialized - Blockchain integration DISABLED");
        }
    }

    @Async
    public void recordPurchase(Player player, List<Card> cards, String packType) {
        if (!config.isBlockchainEnabled() || web3j == null) {
            return;
        }

        try {
            logger.info("Recording {} cards on blockchain for {}", cards.size(), player.getNickname());
            
            for (Card card : cards) {
                mintCardOnBlockchain(player, card);
            }
            
            logger.info("Purchase recorded on blockchain for {}", player.getNickname());
            
        } catch (Exception e) {
            logger.error("Failed to record purchase on blockchain: {}", e.getMessage());
        }
    }

    private void mintCardOnBlockchain(Player player, Card card) throws Exception {
        String playerAddress = getPlayerAddress(player);
        
        // Card type can be derived from the name or a separate field
        String cardType = card.getName().contains("Monstro") ? "Monster" : 
                         card.getName().contains("Magia") ? "Spell" : "Trap";
        
        // Convert rarity string to number (e.g., "Comum" -> 1, "Raro" -> 2, etc.)
        int rarityValue = getRarityValue(card.getRarity());
        
        Function function = new Function(
            "mintCard",
            Arrays.asList(
                new org.web3j.abi.datatypes.Address(playerAddress),
                new Utf8String(cardType),
                new Uint8(rarityValue),
                new Uint16(card.getAttack()),
                new Uint16(card.getDefense())
            ),
            Collections.emptyList()
        );

        String encodedFunction = FunctionEncoder.encode(function);
        
        org.web3j.protocol.core.methods.response.EthSendTransaction response = 
            transactionManager.sendTransaction(
                gasProvider.getGasPrice(),
                gasProvider.getGasLimit(),
                config.getAssetContractAddress(),
                encodedFunction,
                BigInteger.ZERO
            );

        if (response.hasError()) {
            throw new RuntimeException("Blockchain error: " + response.getError().getMessage());
        }

        String txHash = response.getTransactionHash();
        logger.debug("Card minted - Tx: {}", txHash);
        
        // Wait for transaction receipt and extract token ID
        try {
            TransactionReceipt receipt = web3j.ethGetTransactionReceipt(txHash)
                .send()
                .getTransactionReceipt()
                .orElse(null);
            
            if (receipt != null && receipt.getLogs() != null && !receipt.getLogs().isEmpty()) {
                // The Transfer event is emitted by ERC721 when minting
                // Event Transfer(address indexed from, address indexed to, uint256 indexed tokenId)
                // The tokenId is the third topic (topics[3]) in the log
                var logs = receipt.getLogs();
                for (var log : logs) {
                    if (log.getTopics() != null && log.getTopics().size() >= 4) {
                        // Extract tokenId from the third indexed parameter (topics[3])
                        String tokenIdHex = log.getTopics().get(3);
                        BigInteger tokenId = new BigInteger(tokenIdHex.substring(2), 16);
                        
                        // Save tokenId mapping
                        cardTokenIdMapping.put(card.getId(), tokenId);
                        
                        logger.info("✅ Card {} minted with tokenId: {}", card.getId(), tokenId);
                        break;
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Could not extract tokenId from transaction receipt: {}", e.getMessage());
        }
    }
    
    private int getRarityValue(String rarity) {
        // Map Portuguese rarity names to numbers
        return switch (rarity.toLowerCase()) {
            case "comum" -> 1;
            case "incomum" -> 2;
            case "raro" -> 3;
            case "épico" -> 4;
            case "lendário", "lendario" -> 5;
            default -> 1;
        };
    }

    @Async
    public void recordTrade(Player player1, List<Card> cards1, Player player2, List<Card> cards2, String tradeId) {
        if (!config.isBlockchainEnabled() || web3j == null) {
            return;
        }

        try {
            String addr1 = getPlayerAddress(player1);
            String addr2 = getPlayerAddress(player2);
            
            logger.info("🔄 Recording trade on blockchain - {} ↔ {}", player1.getNickname(), player2.getNickname());
            
            // Get token IDs for cards being traded
            List<BigInteger> tokenIds1 = cards1.stream()
                .map(Card::getId)
                .map(cardTokenIdMapping::get)
                .filter(tokenId -> tokenId != null)
                .collect(Collectors.toList());
                
            List<BigInteger> tokenIds2 = cards2.stream()
                .map(Card::getId)
                .map(cardTokenIdMapping::get)
                .filter(tokenId -> tokenId != null)
                .collect(Collectors.toList());
            
            if (tokenIds1.isEmpty() && tokenIds2.isEmpty()) {
                logger.warn("❌ No blockchain token IDs found for trade cards - skipping blockchain recording");
                return;
            }
            
            logger.info("   Player1 ({}) trading {} cards (tokenIds: {})", player1.getNickname(), tokenIds1.size(), tokenIds1);
            logger.info("   Player2 ({}) trading {} cards (tokenIds: {})", player2.getNickname(), tokenIds2.size(), tokenIds2);
            
            // Transfer cards on blockchain to match application state
            // Transfer player1's cards to player2
            for (BigInteger tokenId : tokenIds1) {
                transferCard(addr1, addr2, tokenId);
            }
            
            // Transfer player2's cards to player1
            for (BigInteger tokenId : tokenIds2) {
                transferCard(addr2, addr1, tokenId);
            }
            
            logger.info("✅ Trade {} recorded on blockchain - {} cards transferred", tradeId, tokenIds1.size() + tokenIds2.size());
        } catch (Exception e) {
            logger.error("❌ Failed to record trade on blockchain: {}", e.getMessage(), e);
        }
    }
    
    private void transferCard(String from, String to, BigInteger tokenId) throws Exception {
        // ERC721 safeTransferFrom(address from, address to, uint256 tokenId)
        Function function = new Function(
            "safeTransferFrom",
            Arrays.asList(
                new org.web3j.abi.datatypes.Address(from),
                new org.web3j.abi.datatypes.Address(to),
                new Uint256(tokenId)
            ),
            Collections.emptyList()
        );

        String encodedFunction = FunctionEncoder.encode(function);
        
        org.web3j.protocol.core.methods.response.EthSendTransaction response = 
            transactionManager.sendTransaction(
                gasProvider.getGasPrice(),
                gasProvider.getGasLimit(),
                config.getAssetContractAddress(),
                encodedFunction,
                BigInteger.ZERO
            );

        if (response.hasError()) {
            throw new RuntimeException("Blockchain transfer error: " + response.getError().getMessage());
        }
        
        logger.debug("   Transfer recorded: {} -> {} (tokenId: {}), Tx: {}", 
            from.substring(0, 6), to.substring(0, 6), tokenId, response.getTransactionHash());
    }

    @Async
    public void recordMatch(String matchId, Player winner, Player loser) {
        logger.info("🔄 recordMatch called - matchId: {}, winner: {}, loser: {}", 
            matchId, winner != null ? winner.getNickname() : "null", loser != null ? loser.getNickname() : "null");
            
        if (!config.isBlockchainEnabled() || web3j == null) {
            logger.warn("❌ Blockchain disabled - skipping match recording");
            return;
        }

        try {
            String winnerAddr = winner != null ? getPlayerAddress(winner) : "0x0000000000000000000000000000000000000000";
            String loserAddr = loser != null ? getPlayerAddress(loser) : "0x0000000000000000000000000000000000000000";
            
            // Convert matchId to bytes32 using hash
            byte[] matchIdHash = hashToBytes32(matchId);
            
            logger.debug("Calling blockchain with player1: {}, player2: {}, winner: {}", loserAddr, winnerAddr, winnerAddr);
            
            // MatchContract signature: recordMatch(address player1, address player2, address winner, bytes32 gameStateHash, uint8 player1Score, uint8 player2Score)
            Function function = new Function(
                "recordMatch",
                Arrays.asList(
                    new org.web3j.abi.datatypes.Address(loserAddr),    // player1 (loser)
                    new org.web3j.abi.datatypes.Address(winnerAddr),   // player2 (winner)
                    new org.web3j.abi.datatypes.Address(winnerAddr),   // winner
                    new org.web3j.abi.datatypes.generated.Bytes32(matchIdHash), // gameStateHash
                    new org.web3j.abi.datatypes.generated.Uint8(0),    // player1Score (loser)
                    new org.web3j.abi.datatypes.generated.Uint8(100)   // player2Score (winner)
                ),
                Collections.emptyList()
            );

            String encodedFunction = FunctionEncoder.encode(function);
            
            logger.debug("🔍 Encoded function data: {}", encodedFunction);
            logger.debug("🔍 Function selector: {}", encodedFunction.substring(0, 10));
            logger.debug("🔍 Target contract: {}", config.getMatchContractAddress());
            
            org.web3j.protocol.core.methods.response.EthSendTransaction response = 
                transactionManager.sendTransaction(
                    gasProvider.getGasPrice(),
                    gasProvider.getGasLimit(),
                    config.getMatchContractAddress(),
                    encodedFunction,
                    BigInteger.ZERO
                );

            if (!response.hasError()) {
                logger.info("✅ Match {} recorded on blockchain - Tx: {}", matchId, response.getTransactionHash());
            } else {
                logger.error("❌ Blockchain error: {}", response.getError().getMessage());
            }
            
        } catch (Exception e) {
            logger.error("Failed to record match: {}", e.getMessage(), e);
        }
    }

    private String getPlayerAddress(Player player) {
        String[] accounts = {
            "0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266",
            "0x70997970C51812dc3A010C7d01b50e0d17dc79C8",
            "0x3C44CdDdB6a900fa2b585dd299e03d12FA4293BC",
            "0x90F79bf6EB2c4f870365E785982E1f101E93b906",
            "0x15d34AAf54267DB7D7c367839AAf71A00a2C6A65",
            "0x9965507D1a55bcC2695C58ba16FB37d819B0A4dc",
            "0x976EA74026E726554dB657fA54763abd0C3a0aa9"
        };
        
        int index = Math.abs(player.getId().hashCode() % accounts.length);
        return accounts[index];
    }
    
    private byte[] hashToBytes32(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(input.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            logger.error("Failed to hash string: {}", e.getMessage());
            return new byte[32];
        }
    }
}
