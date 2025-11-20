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
    private final Map<String, String> addressToUsernameMapping;
    private final Map<String, String> playerIdToAddressMapping; // Persistent player → address
    private int nextAddressIndex = 0; // Track next available address

    @Autowired
    public BlockchainService(BlockchainConfig config, Web3j web3j, ContractGasProvider gasProvider) {
        this.config = config;
        this.web3j = web3j;
        this.gasProvider = gasProvider;
        this.cardTokenIdMapping = new java.util.concurrent.ConcurrentHashMap<>();
        this.addressToUsernameMapping = new java.util.concurrent.ConcurrentHashMap<>();
        this.playerIdToAddressMapping = new java.util.concurrent.ConcurrentHashMap<>(); // NEW
        
        // Use deployer account private key (Account #0 from Hardhat)
        String privateKey = "0xac0974bec39a17e36ba4a6b4d238ff944bacb478cbed5efcae784d7bf4f2ff80";
        this.credentials = Credentials.create(privateKey);
        
        if (config.isBlockchainEnabled() && web3j != null) {
            // Chain ID 1337 is configured in hardhat.config.js
            this.transactionManager = new RawTransactionManager(web3j, credentials, 1337L);
            logger.info("BlockchainService initialized - Blockchain integration ENABLED");
            logger.info("Using account: {}", credentials.getAddress());
            logger.info("AssetContract: {}", config.getAssetContractAddress());
            loadAddressMapping();
            loadCardTokenMapping();
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
        
        // Map CardType enum to blockchain card type string
        String cardType = mapCardTypeToBlockchain(card.getCardType());
        
        // Convert rarity string to number (e.g., "Comum" -> 1, "Raro" -> 2, etc.)
        int rarityValue = getRarityValue(card.getRarity());
        
        // Use card ID as the unique card name
        String cardName = card.getId();
        
        Function function = new Function(
            "mintCard",
            Arrays.asList(
                new org.web3j.abi.datatypes.Address(playerAddress),
                new Utf8String(cardName),
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
        // Retry mechanism to wait for transaction to be mined
        try {
            TransactionReceipt receipt = null;
            int maxAttempts = 30; // Wait up to 30 seconds
            int attempts = 0;
            
            while (receipt == null && attempts < maxAttempts) {
                attempts++;
                try {
                    var receiptResponse = web3j.ethGetTransactionReceipt(txHash).send();
                    if (receiptResponse.getTransactionReceipt().isPresent()) {
                        receipt = receiptResponse.getTransactionReceipt().get();
                        break;
                    }
                } catch (Exception e) {
                    logger.debug("Attempt {} to get receipt failed: {}", attempts, e.getMessage());
                }
                
                if (receipt == null) {
                    Thread.sleep(1000); // Wait 1 second before retry
                }
            }
            
            if (receipt == null) {
                logger.error("❌ Could not get transaction receipt after {} attempts for card {}", maxAttempts, card.getId());
                return;
            }
            
            if (receipt.getLogs() != null && !receipt.getLogs().isEmpty()) {
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
                        saveCardTokenMapping();
                        
                        logger.info("✅ Card {} minted with tokenId: {} after {} attempts", card.getId(), tokenId, attempts);
                        return; // Success!
                    }
                }
                logger.warn("⚠️  No Transfer event found in receipt for card {}", card.getId());
            } else {
                logger.warn("⚠️  Receipt has no logs for card {}", card.getId());
            }
        } catch (Exception e) {
            logger.error("❌ Error extracting tokenId from transaction receipt for card {}: {}", card.getId(), e.getMessage(), e);
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
    
    private String mapCardTypeToBlockchain(Card.CardType cardType) {
        // Map Java CardType enum to blockchain-compatible strings
        return switch (cardType) {
            case ATTACK -> "Monster";
            case DEFENSE -> "Defense";
            case MAGIC -> "Spell";
            case ATTRIBUTE -> "Attribute";
            case SCENARIO -> "Scenario";
            case EQUIPMENT -> "Equipment";
            case COMBO -> "Combo";
            case COUNTER_SPELL -> "Counter";
            default -> "Monster";
        };
    }

    /**
     * Records a trade on the blockchain. Returns a status object that can be used to notify users.
     * 
     * @return TradeBlockchainStatus indicating success, partial success, or failure
     */
    @Async
    public java.util.concurrent.CompletableFuture<TradeBlockchainStatus> recordTrade(
            Player player1, List<Card> cards1, Player player2, List<Card> cards2, String tradeId) {
        
        TradeBlockchainStatus status = new TradeBlockchainStatus();
        status.tradeId = tradeId;
        status.player1 = player1.getNickname();
        status.player2 = player2.getNickname();
        
        if (!config.isBlockchainEnabled() || web3j == null) {
            logger.warn("Blockchain not enabled, skipping trade recording");
            status.success = false;
            status.message = "Blockchain integration is not enabled";
            return java.util.concurrent.CompletableFuture.completedFuture(status);
        }

        try {
            String addr1 = getPlayerAddress(player1);
            String addr2 = getPlayerAddress(player2);
            
            logger.info("🔄 Recording trade on blockchain - {} ↔ {}", player1.getNickname(), player2.getNickname());
            logger.info("   Card mapping size: {}", cardTokenIdMapping.size());
            logger.info("   Player1 cards: {}", cards1.stream().map(Card::getId).collect(Collectors.toList()));
            logger.info("   Player2 cards: {}", cards2.stream().map(Card::getId).collect(Collectors.toList()));
            
            // Get token IDs for cards being traded
            // NOTE: If cards were minted outside this server instance or mapping file is missing,
            // the tokenId won't be found and cards won't be transferred on blockchain.
            // TODO: Implement blockchain query to find tokenId by cardId (card name) as fallback
            List<BigInteger> tokenIds1 = cards1.stream()
                .map(Card::getId)
                .map(cardId -> {
                    BigInteger tokenId = cardTokenIdMapping.get(cardId);
                    if (tokenId == null) {
                        logger.warn("   ⚠️  No token ID found in mapping for card: {} (player: {})", 
                            cardId, player1.getNickname());
                        logger.warn("   This card will NOT be transferred on blockchain");
                    }
                    return tokenId;
                })
                .filter(tokenId -> tokenId != null)
                .collect(Collectors.toList());
                
            List<BigInteger> tokenIds2 = cards2.stream()
                .map(Card::getId)
                .map(cardId -> {
                    BigInteger tokenId = cardTokenIdMapping.get(cardId);
                    if (tokenId == null) {
                        logger.warn("   ⚠️  No token ID found in mapping for card: {} (player: {})", 
                            cardId, player2.getNickname());
                        logger.warn("   This card will NOT be transferred on blockchain");
                    }
                    return tokenId;
                })
                .filter(tokenId -> tokenId != null)
                .collect(Collectors.toList());
            
            status.player1CardsWithTokens = tokenIds1.size();
            status.player2CardsWithTokens = tokenIds2.size();
            status.player1TotalCards = cards1.size();
            status.player2TotalCards = cards2.size();
            
            if (tokenIds1.isEmpty() && tokenIds2.isEmpty()) {
                logger.warn("❌ No blockchain token IDs found for trade cards - skipping blockchain recording");
                logger.warn("   Available card mappings: {}", cardTokenIdMapping.keySet());
                status.success = false;
                status.message = "None of the traded cards have blockchain tokens. Trade completed in database only.";
                return java.util.concurrent.CompletableFuture.completedFuture(status);
            }
            
            // Warn about one-way transfers
            if (tokenIds1.isEmpty() || tokenIds2.isEmpty()) {
                logger.warn("⚠️  One-way trade detected on blockchain:");
                logger.warn("   Player1 ({}) has {} token IDs: {}", player1.getNickname(), tokenIds1.size(), tokenIds1);
                logger.warn("   Player2 ({}) has {} token IDs: {}", player2.getNickname(), tokenIds2.size(), tokenIds2);
                logger.warn("   This may indicate cards were not minted on blockchain or mapping is missing");
                status.partialSuccess = true;
                status.message = String.format("Partial blockchain recording: %s transferred %d cards, %s transferred %d cards",
                    player1.getNickname(), tokenIds1.size(), player2.getNickname(), tokenIds2.size());
            }
            
            logger.info("   Player1 ({}) trading {} cards (tokenIds: {})", player1.getNickname(), tokenIds1.size(), tokenIds1);
            logger.info("   Player2 ({}) trading {} cards (tokenIds: {})", player2.getNickname(), tokenIds2.size(), tokenIds2);
            
            // Transfer cards on blockchain to match application state
            // Count actual successful transfers
            int player1Transferred = 0;
            int player2Transferred = 0;
            
            // Transfer player1's cards to player2
            for (BigInteger tokenId : tokenIds1) {
                if (transferCard(addr1, addr2, tokenId)) {
                    player1Transferred++;
                }
            }
            
            // Transfer player2's cards to player1
            for (BigInteger tokenId : tokenIds2) {
                if (transferCard(addr2, addr1, tokenId)) {
                    player2Transferred++;
                }
            }
            
            logger.info("✅ Trade {} recorded on blockchain - {} cards transferred (P1: {}, P2: {})", 
                tradeId, player1Transferred + player2Transferred, player1Transferred, player2Transferred);
            
            // Update status with actual transfer counts
            status.player1CardsWithTokens = player1Transferred;
            status.player2CardsWithTokens = player2Transferred;
            
            // Determine final status
            if (player1Transferred == 0 && player2Transferred == 0) {
                status.success = false;
                status.message = "No cards were transferred on blockchain";
            } else if (player1Transferred == 0 || player2Transferred == 0) {
                status.partialSuccess = true;
                status.message = String.format("Partial blockchain recording: %s transferred %d cards, %s transferred %d cards",
                    player1.getNickname(), player1Transferred, player2.getNickname(), player2Transferred);
            } else {
                status.success = true;
                status.message = "Trade fully recorded on blockchain";
            }
            
            return java.util.concurrent.CompletableFuture.completedFuture(status);
            
        } catch (Exception e) {
            logger.error("❌ Failed to record trade on blockchain: {}", e.getMessage(), e);
            status.success = false;
            status.message = "Blockchain error: " + e.getMessage();
            return java.util.concurrent.CompletableFuture.completedFuture(status);
        }
    }
    
    // Inner class to hold trade blockchain status
    public static class TradeBlockchainStatus {
        public String tradeId;
        public String player1;
        public String player2;
        public boolean success = false;
        public boolean partialSuccess = false;
        public String message;
        public int player1TotalCards;
        public int player2TotalCards;
        public int player1CardsWithTokens;
        public int player2CardsWithTokens;
    }
    
    private boolean transferCard(String from, String to, BigInteger tokenId) throws Exception {
        // First, verify actual ownership on blockchain
        String actualOwner = getTokenOwner(tokenId);
        
        if (!actualOwner.equalsIgnoreCase(from)) {
            logger.warn("⚠️  Token {} ownership mismatch! Expected: {}, Actual: {}", 
                tokenId, from, actualOwner);
            logger.warn("   Using actual owner address for transfer");
            from = actualOwner;
        }
        
        // Check if this would be a self-transfer (invalid)
        if (from.equalsIgnoreCase(to)) {
            logger.warn("⚠️  Skipping self-transfer: Token {} from {} to {} (same address)", 
                tokenId, from.substring(0, 10), to.substring(0, 10));
            logger.warn("   This indicates the token already belongs to the destination");
            return false; // Transfer was skipped
        }
        
        // Use facilitateTransfer - a special function for the authorized minter to execute trades
        // This avoids the need for individual user approvals
        Function function = new Function(
            "facilitateTransfer",
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
        
        return true; // Transfer was successful
    }
    
    private String getTokenOwner(BigInteger tokenId) throws Exception {
        // Query blockchain for actual token owner
        Function function = new Function(
            "ownerOf",
            Arrays.asList(new Uint256(tokenId)),
            Arrays.asList(new org.web3j.abi.TypeReference<org.web3j.abi.datatypes.Address>() {})
        );
        
        String encodedFunction = FunctionEncoder.encode(function);
        
        org.web3j.protocol.core.methods.request.Transaction transaction = 
            org.web3j.protocol.core.methods.request.Transaction.createEthCallTransaction(
                credentials.getAddress(),
                config.getAssetContractAddress(),
                encodedFunction
            );
        
        org.web3j.protocol.core.methods.response.EthCall response = 
            web3j.ethCall(transaction, org.web3j.protocol.core.DefaultBlockParameterName.LATEST).send();
        
        if (response.hasError()) {
            throw new RuntimeException("Failed to query token owner: " + response.getError().getMessage());
        }
        
        String result = response.getValue();
        // Decode address from response (remove 0x and leading zeros, keep last 40 chars)
        if (result != null && result.length() >= 66) {
            return "0x" + result.substring(26); // Address is last 40 hex chars
        }
        
        throw new RuntimeException("Invalid ownerOf response for token " + tokenId);
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
        // Check if player already has an address assigned
        String existingAddress = playerIdToAddressMapping.get(player.getId());
        if (existingAddress != null) {
            logger.debug("Player {} already has address: {}", player.getId(), existingAddress);
            return existingAddress;
        }
        
        // Available Hardhat accounts
        String[] accounts = {
            "0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266",
            "0x70997970C51812dc3A010C7d01b50e0d17dc79C8",
            "0x3C44CdDdB6a900fa2b585dd299e03d12FA4293BC",
            "0x90F79bf6EB2c4f870365E785982E1f101E93b906",
            "0x15d34AAf54267DB7D7c367839AAf71A00a2C6A65",
            "0x9965507D1a55bcC2695C58ba16FB37d819B0A4dc",
            "0x976EA74026E726554dB657fA54763abd0C3a0aa9"
        };
        
        // Assign next available address sequentially
        synchronized (this) {
            if (nextAddressIndex >= accounts.length) {
                logger.warn("All addresses exhausted! Wrapping around...");
                nextAddressIndex = 0;
            }
            
            String address = accounts[nextAddressIndex];
            nextAddressIndex++;
            
            // Store mappings
            playerIdToAddressMapping.put(player.getId(), address);
            registerPlayerAddress(address, player.getNickname());
            
            logger.info("Assigned NEW address {} to player {} (nickname: {})", 
                address, player.getId(), player.getNickname());
            
            return address;
        }
    }
    
    private void registerPlayerAddress(String address, String username) {
        addressToUsernameMapping.put(address, username);
        saveAddressMapping();
    }
    
    private void loadAddressMapping() {
        try {
            String basePath = getBlockchainBasePath();
            java.io.File mappingFile = new java.io.File(basePath, "address-mapping.json");
            logger.info("Loading address mapping from: {}", mappingFile.getAbsolutePath());
            
            if (mappingFile.exists()) {
                String content = new String(java.nio.file.Files.readAllBytes(mappingFile.toPath()));
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                java.util.Map<String, String> loaded = mapper.readValue(content, 
                    new com.fasterxml.jackson.core.type.TypeReference<java.util.Map<String, String>>() {});
                addressToUsernameMapping.putAll(loaded);
                logger.info("Loaded {} address-to-username mappings", loaded.size());
            } else {
                logger.info("No address mapping file found, will create on first use");
            }
        } catch (Exception e) {
            logger.warn("Could not load address mapping: {}", e.getMessage());
        }
    }
    
    private void saveAddressMapping() {
        try {
            String basePath = getBlockchainBasePath();
            java.io.File mappingFile = new java.io.File(basePath, "address-mapping.json");
            mappingFile.getParentFile().mkdirs();
            
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            mapper.writerWithDefaultPrettyPrinter().writeValue(mappingFile, addressToUsernameMapping);
            logger.debug("Saved address mapping to: {}", mappingFile.getAbsolutePath());
        } catch (Exception e) {
            logger.error("Could not save address mapping: {}", e.getMessage(), e);
        }
    }
    
    private void loadCardTokenMapping() {
        try {
            String basePath = getBlockchainBasePath();
            java.io.File mappingFile = new java.io.File(basePath, "card-token-mapping.json");
            logger.info("Loading card-token mapping from: {}", mappingFile.getAbsolutePath());
            
            if (mappingFile.exists()) {
                String content = new String(java.nio.file.Files.readAllBytes(mappingFile.toPath()));
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                java.util.Map<String, Long> loaded = mapper.readValue(content, 
                    new com.fasterxml.jackson.core.type.TypeReference<java.util.Map<String, Long>>() {});
                loaded.forEach((cardId, tokenIdLong) -> 
                    cardTokenIdMapping.put(cardId, BigInteger.valueOf(tokenIdLong)));
                logger.info("Loaded {} card-to-token mappings", loaded.size());
            } else {
                logger.info("No card-token mapping file found, will create on first use");
            }
        } catch (Exception e) {
            logger.warn("Could not load card-token mapping: {}", e.getMessage());
        }
    }
    
    private void saveCardTokenMapping() {
        try {
            String basePath = getBlockchainBasePath();
            java.io.File mappingFile = new java.io.File(basePath, "card-token-mapping.json");
            mappingFile.getParentFile().mkdirs();
            
            // Convert BigInteger to Long for JSON serialization
            java.util.Map<String, Long> toSave = new java.util.HashMap<>();
            cardTokenIdMapping.forEach((cardId, tokenId) -> 
                toSave.put(cardId, tokenId.longValue()));
            
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            mapper.writerWithDefaultPrettyPrinter().writeValue(mappingFile, toSave);
            logger.debug("Saved card-token mapping to: {} ({} entries)", mappingFile.getAbsolutePath(), toSave.size());
        } catch (Exception e) {
            logger.error("Could not save card-token mapping: {}", e.getMessage(), e);
        }
    }
    
    private String getBlockchainBasePath() {
        // Try to find dueling-blockchain directory
        java.io.File currentDir = new java.io.File(System.getProperty("user.dir"));
        java.io.File blockchainDir = new java.io.File(currentDir, "dueling-blockchain");
        
        // If not found in current dir, try parent dir (when running from dueling-server)
        if (!blockchainDir.exists()) {
            blockchainDir = new java.io.File(currentDir.getParentFile(), "dueling-blockchain");
        }
        
        // Create if doesn't exist
        if (!blockchainDir.exists()) {
            blockchainDir.mkdirs();
        }
        
        return blockchainDir.getAbsolutePath();
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
