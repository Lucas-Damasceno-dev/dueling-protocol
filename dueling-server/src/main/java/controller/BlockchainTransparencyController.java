package controller;

import config.BlockchainConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Uint16;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.abi.datatypes.generated.Uint8;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthCall;

import java.math.BigInteger;
import java.util.*;

/**
 * REST API for blockchain transparency and auditability.
 * Allows users to query blockchain data directly without trusting the server.
 */
@RestController
@RequestMapping("/api/blockchain")
@CrossOrigin(origins = "*")
public class BlockchainTransparencyController {
    
    private static final Logger logger = LoggerFactory.getLogger(BlockchainTransparencyController.class);
    
    private final BlockchainConfig config;
    private final Web3j web3j;
    
    @Autowired
    public BlockchainTransparencyController(BlockchainConfig config, Web3j web3j) {
        this.config = config;
        this.web3j = web3j;
    }
    
    /**
     * Get blockchain network information
     */
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getBlockchainInfo() {
        if (!config.isBlockchainEnabled() || web3j == null) {
            return ResponseEntity.ok(Map.of(
                "enabled", false,
                "message", "Blockchain integration is not enabled"
            ));
        }
        
        try {
            Map<String, Object> info = new HashMap<>();
            info.put("enabled", true);
            info.put("contracts", Map.of(
                "AssetContract", config.getAssetContractAddress(),
                "StoreContract", config.getStoreContractAddress(),
                "TradeContract", config.getTradeContractAddress(),
                "MatchContract", config.getMatchContractAddress(),
                "IntegrityContract", config.getIntegrityContractAddress(),
                "OracleRegistry", config.getOracleRegistryAddress()
            ));
            
            // Get current block number
            BigInteger blockNumber = web3j.ethBlockNumber().send().getBlockNumber();
            info.put("currentBlock", blockNumber.toString());
            
            // Get network ID
            String networkId = web3j.netVersion().send().getNetVersion();
            info.put("networkId", networkId);
            
            // Network name
            String networkName = getNetworkName(networkId);
            info.put("networkName", networkName);
            
            // Explorer URL
            String explorerUrl = getExplorerUrl(networkId);
            info.put("explorerUrl", explorerUrl);
            
            info.put("message", "Blockchain integration is active. All data is verifiable on-chain.");
            
            return ResponseEntity.ok(info);
            
        } catch (Exception e) {
            logger.error("Error getting blockchain info: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of(
                "error", "Failed to query blockchain: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Get player's card ownership from blockchain
     */
    @GetMapping("/cards/{address}")
    public ResponseEntity<Map<String, Object>> getPlayerCards(@PathVariable String address) {
        if (!config.isBlockchainEnabled() || web3j == null) {
            return ResponseEntity.ok(Map.of("enabled", false));
        }
        
        try {
            // Get balance (number of cards owned)
            BigInteger balance = getCardBalance(address);
            
            List<Map<String, Object>> cards = new ArrayList<>();
            
            // Get each token owned by the player
            for (int i = 0; i < balance.intValue() && i < 100; i++) { // Limit to 100 cards
                BigInteger tokenId = getTokenByIndex(address, BigInteger.valueOf(i));
                Map<String, Object> cardData = getCardData(tokenId);
                if (cardData != null) {
                    cards.add(cardData);
                }
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("address", address);
            result.put("totalCards", balance.toString());
            result.put("cards", cards);
            result.put("contractAddress", config.getAssetContractAddress());
            result.put("explorerUrl", getExplorerUrl(web3j.netVersion().send().getNetVersion()) + 
                                     "/address/" + address);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("Error getting player cards: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of(
                "error", "Failed to query blockchain: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Get specific card details from blockchain
     */
    @GetMapping("/card/{tokenId}")
    public ResponseEntity<Map<String, Object>> getCard(@PathVariable String tokenId) {
        if (!config.isBlockchainEnabled() || web3j == null) {
            return ResponseEntity.ok(Map.of("enabled", false));
        }
        
        try {
            BigInteger token = new BigInteger(tokenId);
            Map<String, Object> cardData = getCardData(token);
            
            if (cardData == null) {
                return ResponseEntity.status(404).body(Map.of(
                    "error", "Card not found or invalid token ID"
                ));
            }
            
            // Add explorer link
            String networkId = web3j.netVersion().send().getNetVersion();
            cardData.put("explorerUrl", getExplorerUrl(networkId) + 
                                       "/token/" + config.getAssetContractAddress() + 
                                       "?a=" + tokenId);
            
            return ResponseEntity.ok(cardData);
            
        } catch (Exception e) {
            logger.error("Error getting card data: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of(
                "error", "Failed to query blockchain: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Get match history from blockchain
     */
    @GetMapping("/matches/{address}")
    public ResponseEntity<Map<String, Object>> getPlayerMatches(
            @PathVariable String address,
            @RequestParam(defaultValue = "10") int limit) {
        if (!config.isBlockchainEnabled() || web3j == null) {
            return ResponseEntity.ok(Map.of("enabled", false));
        }
        
        try {
            // Get player stats
            Map<String, Object> stats = getPlayerStats(address);
            
            Map<String, Object> result = new HashMap<>();
            result.put("address", address);
            result.put("stats", stats);
            result.put("contractAddress", config.getMatchContractAddress());
            result.put("explorerUrl", getExplorerUrl(web3j.netVersion().send().getNetVersion()) + 
                                     "/address/" + config.getMatchContractAddress());
            result.put("message", "View full match history on the blockchain explorer");
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("Error getting player matches: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of(
                "error", "Failed to query blockchain: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Verify a specific transaction
     */
    @GetMapping("/tx/{txHash}")
    public ResponseEntity<Map<String, Object>> verifyTransaction(@PathVariable String txHash) {
        if (!config.isBlockchainEnabled() || web3j == null) {
            return ResponseEntity.ok(Map.of("enabled", false));
        }
        
        try {
            var tx = web3j.ethGetTransactionByHash(txHash).send();
            if (!tx.getTransaction().isPresent()) {
                return ResponseEntity.status(404).body(Map.of(
                    "error", "Transaction not found"
                ));
            }
            
            var transaction = tx.getTransaction().get();
            var receipt = web3j.ethGetTransactionReceipt(txHash).send();
            
            Map<String, Object> result = new HashMap<>();
            result.put("hash", txHash);
            result.put("from", transaction.getFrom());
            result.put("to", transaction.getTo());
            result.put("blockNumber", transaction.getBlockNumber().toString());
            result.put("status", receipt.getTransactionReceipt()
                .map(r -> r.isStatusOK() ? "success" : "failed")
                .orElse("pending"));
            
            String networkId = web3j.netVersion().send().getNetVersion();
            result.put("explorerUrl", getExplorerUrl(networkId) + "/tx/" + txHash);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("Error verifying transaction: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of(
                "error", "Failed to query blockchain: " + e.getMessage()
            ));
        }
    }
    
    // Helper methods
    
    private BigInteger getCardBalance(String address) throws Exception {
        Function function = new Function(
            "balanceOf",
            Arrays.asList(new Address(address)),
            Arrays.asList(new TypeReference<Uint256>() {})
        );
        
        String encodedFunction = FunctionEncoder.encode(function);
        EthCall response = web3j.ethCall(
            Transaction.createEthCallTransaction(null, config.getAssetContractAddress(), encodedFunction),
            DefaultBlockParameterName.LATEST
        ).send();
        
        List<Type> decoded = FunctionReturnDecoder.decode(response.getValue(), function.getOutputParameters());
        return (BigInteger) decoded.get(0).getValue();
    }
    
    private BigInteger getTokenByIndex(String address, BigInteger index) throws Exception {
        Function function = new Function(
            "tokenOfOwnerByIndex",
            Arrays.asList(new Address(address), new Uint256(index)),
            Arrays.asList(new TypeReference<Uint256>() {})
        );
        
        String encodedFunction = FunctionEncoder.encode(function);
        EthCall response = web3j.ethCall(
            Transaction.createEthCallTransaction(null, config.getAssetContractAddress(), encodedFunction),
            DefaultBlockParameterName.LATEST
        ).send();
        
        List<Type> decoded = FunctionReturnDecoder.decode(response.getValue(), function.getOutputParameters());
        return (BigInteger) decoded.get(0).getValue();
    }
    
    private Map<String, Object> getCardData(BigInteger tokenId) {
        try {
            // Get card metadata
            Function function = new Function(
                "getCardMetadata",
                Arrays.asList(new Uint256(tokenId)),
                Arrays.asList(
                    new TypeReference<Utf8String>() {},  // name
                    new TypeReference<Utf8String>() {},  // cardType
                    new TypeReference<Uint8>() {},       // rarity
                    new TypeReference<Uint16>() {},      // attack
                    new TypeReference<Uint16>() {}       // defense
                )
            );
            
            String encodedFunction = FunctionEncoder.encode(function);
            EthCall response = web3j.ethCall(
                Transaction.createEthCallTransaction(null, config.getAssetContractAddress(), encodedFunction),
                DefaultBlockParameterName.LATEST
            ).send();
            
            List<Type> decoded = FunctionReturnDecoder.decode(response.getValue(), function.getOutputParameters());
            
            if (decoded.isEmpty()) {
                return null;
            }
            
            // Get owner
            String owner = getTokenOwner(tokenId);
            
            Map<String, Object> cardData = new HashMap<>();
            cardData.put("tokenId", tokenId.toString());
            cardData.put("name", decoded.get(0).getValue().toString());
            cardData.put("type", decoded.get(1).getValue().toString());
            cardData.put("rarity", decoded.get(2).getValue().toString());
            cardData.put("attack", decoded.get(3).getValue().toString());
            cardData.put("defense", decoded.get(4).getValue().toString());
            cardData.put("owner", owner);
            
            return cardData;
            
        } catch (Exception e) {
            logger.error("Error getting card metadata for token {}: {}", tokenId, e.getMessage());
            return null;
        }
    }
    
    private String getTokenOwner(BigInteger tokenId) throws Exception {
        Function function = new Function(
            "ownerOf",
            Arrays.asList(new Uint256(tokenId)),
            Arrays.asList(new TypeReference<Address>() {})
        );
        
        String encodedFunction = FunctionEncoder.encode(function);
        EthCall response = web3j.ethCall(
            Transaction.createEthCallTransaction(null, config.getAssetContractAddress(), encodedFunction),
            DefaultBlockParameterName.LATEST
        ).send();
        
        List<Type> decoded = FunctionReturnDecoder.decode(response.getValue(), function.getOutputParameters());
        return decoded.get(0).getValue().toString();
    }
    
    private Map<String, Object> getPlayerStats(String address) throws Exception {
        Function function = new Function(
            "getPlayerStats",
            Arrays.asList(new Address(address)),
            Arrays.asList(
                new TypeReference<Uint256>() {},  // totalMatches
                new TypeReference<Uint256>() {},  // wins
                new TypeReference<Uint256>() {}   // losses
            )
        );
        
        String encodedFunction = FunctionEncoder.encode(function);
        EthCall response = web3j.ethCall(
            Transaction.createEthCallTransaction(null, config.getMatchContractAddress(), encodedFunction),
            DefaultBlockParameterName.LATEST
        ).send();
        
        List<Type> decoded = FunctionReturnDecoder.decode(response.getValue(), function.getOutputParameters());
        
        BigInteger totalMatches = (BigInteger) decoded.get(0).getValue();
        BigInteger wins = (BigInteger) decoded.get(1).getValue();
        BigInteger losses = (BigInteger) decoded.get(2).getValue();
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalMatches", totalMatches.toString());
        stats.put("wins", wins.toString());
        stats.put("losses", losses.toString());
        
        if (totalMatches.compareTo(BigInteger.ZERO) > 0) {
            double winRate = wins.doubleValue() / totalMatches.doubleValue() * 100.0;
            stats.put("winRate", String.format("%.2f%%", winRate));
        } else {
            stats.put("winRate", "0.00%");
        }
        
        return stats;
    }
    
    private String getNetworkName(String networkId) {
        return switch (networkId) {
            case "1" -> "Ethereum Mainnet";
            case "11155111" -> "Sepolia Testnet";
            case "5" -> "Goerli Testnet";
            case "137" -> "Polygon Mainnet";
            case "80001" -> "Mumbai Testnet";
            case "1337" -> "Local Hardhat Network";
            default -> "Unknown Network (ID: " + networkId + ")";
        };
    }
    
    private String getExplorerUrl(String networkId) {
        return switch (networkId) {
            case "1" -> "https://etherscan.io";
            case "11155111" -> "https://sepolia.etherscan.io";
            case "5" -> "https://goerli.etherscan.io";
            case "137" -> "https://polygonscan.com";
            case "80001" -> "https://mumbai.polygonscan.com";
            default -> "http://localhost:8545"; // Local network
        };
    }
}
