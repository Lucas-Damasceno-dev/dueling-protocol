package controller;

import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import service.oracle.OracleService;

import java.util.HashMap;
import java.util.Map;

/**
 * REST API for public integrity verification
 * Allows anyone to verify operations against blockchain proofs
 */
@RestController
@RequestMapping("/api/verify")
public class IntegrityVerificationController {
    
    private static final Logger logger = LoggerFactory.getLogger(IntegrityVerificationController.class);
    
    private final OracleService oracleService;
    private final Gson gson;
    
    @Autowired
    public IntegrityVerificationController(OracleService oracleService) {
        this.oracleService = oracleService;
        this.gson = new Gson();
    }
    
    /**
     * Verify a purchase operation
     * POST /api/verify/purchase
     * Body: JSON with purchase data
     */
    @PostMapping("/purchase")
    public ResponseEntity<Map<String, Object>> verifyPurchase(@RequestBody Map<String, Object> purchaseData) {
        try {
            String purchaseId = (String) purchaseData.get("purchaseId");
            if (purchaseId == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Missing purchaseId"
                ));
            }
            
            logger.info("🔍 Verifying purchase: {}", purchaseId);
            
            OracleService.VerificationResult result = oracleService.verifyOperation(purchaseId, purchaseData);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", result.isSuccess());
            response.put("operationId", result.getOperationId());
            response.put("hash", result.getHash());
            response.put("message", result.getMessage());
            response.put("verified", result.isSuccess());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error verifying purchase: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "message", "Verification error: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Verify a trade operation
     * POST /api/verify/trade
     * Body: JSON with trade data
     */
    @PostMapping("/trade")
    public ResponseEntity<Map<String, Object>> verifyTrade(@RequestBody Map<String, Object> tradeData) {
        try {
            String tradeId = (String) tradeData.get("tradeId");
            if (tradeId == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Missing tradeId"
                ));
            }
            
            logger.info("🔍 Verifying trade: {}", tradeId);
            
            OracleService.VerificationResult result = oracleService.verifyOperation(tradeId, tradeData);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", result.isSuccess());
            response.put("operationId", result.getOperationId());
            response.put("hash", result.getHash());
            response.put("message", result.getMessage());
            response.put("verified", result.isSuccess());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error verifying trade: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "message", "Verification error: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Verify a match operation
     * POST /api/verify/match
     * Body: JSON with match data
     */
    @PostMapping("/match")
    public ResponseEntity<Map<String, Object>> verifyMatch(@RequestBody Map<String, Object> matchData) {
        try {
            String matchId = (String) matchData.get("matchId");
            if (matchId == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Missing matchId"
                ));
            }
            
            logger.info("🔍 Verifying match: {}", matchId);
            
            OracleService.VerificationResult result = oracleService.verifyOperation(matchId, matchData);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", result.isSuccess());
            response.put("operationId", result.getOperationId());
            response.put("hash", result.getHash());
            response.put("message", result.getMessage());
            response.put("verified", result.isSuccess());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error verifying match: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "message", "Verification error: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Get verification instructions
     * GET /api/verify/instructions
     */
    @GetMapping("/instructions")
    public ResponseEntity<Map<String, Object>> getInstructions() {
        Map<String, Object> instructions = new HashMap<>();
        instructions.put("description", "Oracle Pattern Integrity Verification API");
        instructions.put("purpose", "Verify that operations in PostgreSQL match blockchain proofs");
        
        Map<String, String> endpoints = new HashMap<>();
        endpoints.put("POST /api/verify/purchase", "Verify purchase integrity");
        endpoints.put("POST /api/verify/trade", "Verify trade integrity");
        endpoints.put("POST /api/verify/match", "Verify match integrity");
        instructions.put("endpoints", endpoints);
        
        Map<String, Object> example = new HashMap<>();
        example.put("purchaseId", "purchase-uuid-here");
        example.put("operationType", "PURCHASE");
        example.put("playerId", "player-id");
        example.put("packType", "bronze");
        example.put("timestamp", System.currentTimeMillis());
        instructions.put("examplePayload", example);
        
        instructions.put("howItWorks", Map.of(
            "1", "Backend generates SHA-256 hash of operation data",
            "2", "Hash is recorded on blockchain via IntegrityContract",
            "3", "Anyone can verify by providing same data",
            "4", "System recalculates hash and compares with blockchain",
            "5", "Match = data is authentic, Mismatch = data was tampered"
        ));
        
        return ResponseEntity.ok(instructions);
    }
    
    /**
     * Health check
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "Integrity Verification API",
            "pattern", "Oracle Pattern"
        ));
    }
}
