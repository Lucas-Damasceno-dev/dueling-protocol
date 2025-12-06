package service.oracle;

import com.google.gson.Gson;
import model.Card;
import model.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Oracle Service - Generates cryptographic proofs of operations
 * Acts as trusted oracle between PostgreSQL (data source) and Blockchain (truth source)
 */
@Service
public class OracleService {
    
    private static final Logger logger = LoggerFactory.getLogger(OracleService.class);
    
    private final IntegrityContractService integrityService;
    private final Gson gson;
    
    @Autowired
    public OracleService(IntegrityContractService integrityService) {
        this.integrityService = integrityService;
        this.gson = new Gson();
    }
    
    /**
     * Generate and record proof for a purchase operation
     */
    @Async
    public void recordPurchaseProof(
        String purchaseId,
        Player player,
        List<Card> cards,
        String packType,
        int coinsCost,
        long timestamp
    ) {
        try {
            // Build canonical data structure for hashing
            Map<String, Object> data = new HashMap<>();
            data.put("operationType", "PURCHASE");
            data.put("purchaseId", purchaseId);
            data.put("playerId", player.getId());
            data.put("playerNickname", player.getNickname());
            data.put("packType", packType);
            data.put("coinsCost", coinsCost);
            data.put("timestamp", timestamp);
            data.put("cardsReceived", cards.stream()
                .map(card -> Map.of(
                    "id", card.getId(),
                    "name", card.getName(),
                    "type", card.getCardType().toString(),
                    "rarity", card.getRarity(),
                    "attack", card.getAttack(),
                    "defense", card.getDefense()
                ))
                .collect(Collectors.toList())
            );
            
            // Generate SHA-256 hash
            String dataJson = gson.toJson(data);
            byte[] hash = generateSHA256(dataJson);
            String hashHex = bytesToHex(hash);
            
            logger.info("🔐 Generated purchase proof - ID: {}, Hash: 0x{}", purchaseId, hashHex);
            
            // Record on blockchain
            integrityService.recordPurchaseProof(purchaseId, hash, player);
            
            logger.info("✅ Purchase proof recorded on blockchain - ID: {}", purchaseId);
            
        } catch (Exception e) {
            logger.error("❌ Failed to record purchase proof for {}: {}", purchaseId, e.getMessage(), e);
        }
    }
    
    /**
     * Generate and record proof for a trade operation
     */
    @Async
    public void recordTradeProof(
        String tradeId,
        Player player1,
        List<Card> player1Cards,
        Player player2,
        List<Card> player2Cards,
        long timestamp
    ) {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("operationType", "TRADE");
            data.put("tradeId", tradeId);
            data.put("player1Id", player1.getId());
            data.put("player1Nickname", player1.getNickname());
            data.put("player2Id", player2.getId());
            data.put("player2Nickname", player2.getNickname());
            data.put("timestamp", timestamp);
            data.put("player1OfferedCards", player1Cards.stream()
                .map(card -> card.getId())
                .collect(Collectors.toList())
            );
            data.put("player2OfferedCards", player2Cards.stream()
                .map(card -> card.getId())
                .collect(Collectors.toList())
            );
            
            String dataJson = gson.toJson(data);
            byte[] hash = generateSHA256(dataJson);
            String hashHex = bytesToHex(hash);
            
            logger.info("🔐 Generated trade proof - ID: {}, Hash: 0x{}", tradeId, hashHex);
            
            integrityService.recordTradeProof(tradeId, hash, player1, player2);
            
            logger.info("✅ Trade proof recorded on blockchain - ID: {}", tradeId);
            
        } catch (Exception e) {
            logger.error("❌ Failed to record trade proof for {}: {}", tradeId, e.getMessage(), e);
        }
    }
    
    /**
     * Generate and record proof for a match operation
     */
    @Async
    public void recordMatchProof(
        String matchId,
        Player winner,
        Player loser,
        int winnerScore,
        int loserScore,
        long timestamp
    ) {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("operationType", "MATCH");
            data.put("matchId", matchId);
            data.put("winnerId", winner.getId());
            data.put("winnerNickname", winner.getNickname());
            data.put("loserId", loser.getId());
            data.put("loserNickname", loser.getNickname());
            data.put("winnerScore", winnerScore);
            data.put("loserScore", loserScore);
            data.put("timestamp", timestamp);
            
            String dataJson = gson.toJson(data);
            byte[] hash = generateSHA256(dataJson);
            String hashHex = bytesToHex(hash);
            
            logger.info("🔐 Generated match proof - ID: {}, Hash: 0x{}", matchId, hashHex);
            
            integrityService.recordMatchProof(matchId, hash, winner, loser);
            
            logger.info("✅ Match proof recorded on blockchain - ID: {}", matchId);
            
        } catch (Exception e) {
            logger.error("❌ Failed to record match proof for {}: {}", matchId, e.getMessage(), e);
        }
    }
    
    /**
     * Verify an operation against blockchain
     */
    public VerificationResult verifyOperation(String operationId, Map<String, Object> data) {
        try {
            // Generate hash from provided data
            String dataJson = gson.toJson(data);
            byte[] hash = generateSHA256(dataJson);
            
            // Compare with blockchain
            boolean matches = integrityService.verifyProof(operationId, hash);
            
            if (matches) {
                logger.info("✅ Verification SUCCESS for operation: {}", operationId);
                return VerificationResult.success(operationId, bytesToHex(hash));
            } else {
                logger.warn("⚠️  Verification FAILED for operation: {} - Hash mismatch!", operationId);
                return VerificationResult.failure(operationId, "Hash mismatch - data may be corrupted");
            }
            
        } catch (Exception e) {
            logger.error("❌ Verification ERROR for operation {}: {}", operationId, e.getMessage());
            return VerificationResult.error(operationId, e.getMessage());
        }
    }
    
    /**
     * Generate SHA-256 hash
     */
    private byte[] generateSHA256(String data) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return digest.digest(data.getBytes(StandardCharsets.UTF_8));
    }
    
    /**
     * Convert bytes to hex string
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
    
    /**
     * Verification result
     */
    public static class VerificationResult {
        private final boolean success;
        private final String operationId;
        private final String hash;
        private final String message;
        
        private VerificationResult(boolean success, String operationId, String hash, String message) {
            this.success = success;
            this.operationId = operationId;
            this.hash = hash;
            this.message = message;
        }
        
        public static VerificationResult success(String operationId, String hash) {
            return new VerificationResult(true, operationId, hash, "Verification successful");
        }
        
        public static VerificationResult failure(String operationId, String message) {
            return new VerificationResult(false, operationId, null, message);
        }
        
        public static VerificationResult error(String operationId, String message) {
            return new VerificationResult(false, operationId, null, "Error: " + message);
        }
        
        public boolean isSuccess() { return success; }
        public String getOperationId() { return operationId; }
        public String getHash() { return hash; }
        public String getMessage() { return message; }
    }
}
