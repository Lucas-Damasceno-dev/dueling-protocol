package client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class TradeTestClient {
    private static final Logger logger = LoggerFactory.getLogger(TradeTestClient.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    private final String username;
    private final String password;
    private final String serverUrl;
    private String token;
    private String playerId;
    private WebSocket webSocket;
    private final CountDownLatch characterCreatedLatch = new CountDownLatch(1);
    private final CountDownLatch packPurchasedLatch = new CountDownLatch(1);
    private final CountDownLatch tradeProposalLatch = new CountDownLatch(1);
    private final CountDownLatch tradeCompletedLatch = new CountDownLatch(1);
    private String cardToTrade;
    private String receivedTradeId;
    
    public TradeTestClient(String username, String password, String serverUrl) {
        this.username = username;
        this.password = password;
        this.serverUrl = serverUrl;
    }
    
    public void registerAndLogin() throws Exception {
        HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
        
        // Register
        logger.info("[{}] Registering...", username);
        String registerJson = String.format("{\"username\":\"%s\",\"password\":\"%s\"}", username, password);
        HttpRequest registerRequest = HttpRequest.newBuilder()
            .uri(URI.create(serverUrl + "/api/auth/register"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(registerJson))
            .build();
        
        try {
            HttpResponse<String> registerResponse = client.send(registerRequest, HttpResponse.BodyHandlers.ofString());
            logger.info("[{}] Register response: {}", username, registerResponse.body());
        } catch (Exception e) {
            logger.info("[{}] Register failed (may already exist): {}", username, e.getMessage());
        }
        
        // Login
        logger.info("[{}] Logging in...", username);
        String loginJson = String.format("{\"username\":\"%s\",\"password\":\"%s\"}", username, password);
        HttpRequest loginRequest = HttpRequest.newBuilder()
            .uri(URI.create(serverUrl + "/api/auth/login"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(loginJson))
            .build();
        
        HttpResponse<String> loginResponse = client.send(loginRequest, HttpResponse.BodyHandlers.ofString());
        JsonNode loginNode = objectMapper.readTree(loginResponse.body());
        this.token = loginNode.get("token").asText();
        logger.info("[{}] Login successful, obtained JWT token", username);
    }
    
    public void connectWebSocket() throws Exception {
        String wsUrl = serverUrl.replace("http://", "ws://") + "/ws?token=" + token;
        logger.info("[{}] Connecting to WebSocket: {}", username, wsUrl);
        
        HttpClient client = HttpClient.newHttpClient();
        CompletableFuture<WebSocket> wsFuture = client.newWebSocketBuilder()
            .buildAsync(URI.create(wsUrl), new WebSocket.Listener() {
                private StringBuilder messageBuffer = new StringBuilder();
                
                @Override
                public void onOpen(WebSocket ws) {
                    logger.info("[{}] WebSocket opened", username);
                    WebSocket.Listener.super.onOpen(ws);
                }
                
                @Override
                public CompletionStage<?> onText(WebSocket ws, CharSequence data, boolean last) {
                    messageBuffer.append(data);
                    if (last) {
                        String message = messageBuffer.toString();
                        messageBuffer.setLength(0);
                        handleMessage(message);
                    }
                    return WebSocket.Listener.super.onText(ws, data, last);
                }
                
                @Override
                public void onError(WebSocket ws, Throwable error) {
                    logger.error("[{}] WebSocket error: {}", username, error.getMessage());
                }
            });
        
        this.webSocket = wsFuture.get(10, TimeUnit.SECONDS);
    }
    
    private void handleMessage(String message) {
        logger.info("[{}] Received: {}", username, message);
        
        if (message.contains("SUCCESS:Character created")) {
            this.playerId = extractPlayerId(message);
            characterCreatedLatch.countDown();
        } else if (message.contains("SUCCESS:Pack purchased")) {
            // Extract first card from the pack
            if (message.contains("(ID_")) {
                int idStart = message.indexOf("(ID_") + 4;
                int idEnd = message.indexOf(")", idStart);
                this.cardToTrade = message.substring(idStart, idEnd);
                logger.info("[{}] Got card to trade: {}", username, cardToTrade);
            }
            packPurchasedLatch.countDown();
        } else if (message.contains("UPDATE:TRADE_PROPOSAL")) {
            // Format: UPDATE:TRADE_PROPOSAL:tradeId:proposerId:offeredCards:requestedCards
            String[] parts = message.split(":");
            if (parts.length >= 3) {
                this.receivedTradeId = parts[2];
                logger.info("[{}] Extracted trade ID: {}", username, receivedTradeId);
            }
            tradeProposalLatch.countDown();
        } else if (message.contains("UPDATE:TRADE_COMPLETED") || message.contains("SUCCESS:Trade completed")) {
            tradeCompletedLatch.countDown();
        }
    }
    
    private String extractPlayerId(String message) {
        // Message format: SUCCESS:Character created. Player ID: <id>
        String[] parts = message.split("Player ID:");
        if (parts.length > 1) {
            return parts[1].trim();
        }
        return username; // fallback
    }
    
    public void createCharacter() throws Exception {
        logger.info("[{}] Creating character...", username);
        String cmd = String.format("CHARACTER_SETUP:%s:HUMAN:WARRIOR", username);
        webSocket.sendText(cmd, true);
        
        if (!characterCreatedLatch.await(10, TimeUnit.SECONDS)) {
            throw new Exception("Character creation timeout");
        }
        logger.info("[{}] Character created, Player ID: {}", username, playerId);
    }
    
    public void buyPack() throws Exception {
        logger.info("[{}] Buying pack...", username);
        webSocket.sendText("STORE:BUY:BASIC", true);
        
        if (!packPurchasedLatch.await(10, TimeUnit.SECONDS)) {
            throw new Exception("Pack purchase timeout");
        }
        logger.info("[{}] Pack purchased, got card: {}", username, cardToTrade);
    }
    
    public void proposeTrade(String targetPlayerId, String offeredCardId, String requestedCardId) throws Exception {
        logger.info("[{}] Proposing trade to {} offering {} for {}", username, targetPlayerId, offeredCardId, requestedCardId);
        String cmd = String.format("TRADE:PROPOSE:%s:%s:%s", targetPlayerId, offeredCardId, requestedCardId);
        webSocket.sendText(cmd, true);
    }
    
    public boolean waitForTradeProposal(long seconds) throws Exception {
        logger.info("[{}] Waiting for trade proposal...", username);
        return tradeProposalLatch.await(seconds, TimeUnit.SECONDS);
    }
    
    public void acceptTrade(String tradeId) throws Exception {
        logger.info("[{}] Accepting trade {}", username, tradeId);
        String cmd = String.format("TRADE:ACCEPT:%s", tradeId);
        webSocket.sendText(cmd, true);
    }
    
    public boolean waitForTradeCompleted(long seconds) throws Exception {
        logger.info("[{}] Waiting for trade completion...", username);
        return tradeCompletedLatch.await(seconds, TimeUnit.SECONDS);
    }
    
    public String getPlayerId() {
        return playerId;
    }
    
    public String getCardToTrade() {
        return cardToTrade;
    }
    
    public void disconnect() {
        if (webSocket != null) {
            webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "Test completed");
        }
    }
    
    public static void main(String[] args) {
        if (args.length < 4) {
            System.err.println("Usage: TradeTestClient <userA> <passwordA> <serverUrlA> <serverUrlB>");
            System.exit(1);
        }
        
        String userA = args[0];
        String passwordA = args[1];
        String serverA = args[2]; // e.g., http://localhost:8080
        String serverB = args[3]; // e.g., http://localhost:8083
        
        String userB = userA + "_trader";
        String passwordB = passwordA;
        
        TradeTestClient clientA = new TradeTestClient(userA, passwordA, serverA);
        TradeTestClient clientB = new TradeTestClient(userB, passwordB, serverB);
        
        try {
            logger.info("=== STARTING TRADE TEST ===");
            
            // Setup Player A
            logger.info("--- Setting up Player A on {} ---", serverA);
            clientA.registerAndLogin();
            clientA.connectWebSocket();
            clientA.createCharacter();
            clientA.buyPack();
            Thread.sleep(2000);
            
            // Setup Player B
            logger.info("--- Setting up Player B on {} ---", serverB);
            clientB.registerAndLogin();
            clientB.connectWebSocket();
            clientB.createCharacter();
            clientB.buyPack();
            Thread.sleep(2000);
            
            // Trade
            logger.info("--- Proposing trade from A to B ---");
            String cardA = clientA.getCardToTrade();
            String cardB = clientB.getCardToTrade();
            String playerB = clientB.getPlayerId();
            
            if (cardA == null || cardB == null || playerB == null) {
                logger.error("Missing card or player ID! CardA: {}, CardB: {}, PlayerB: {}", cardA, cardB, playerB);
                System.exit(1);
            }
            
            clientA.proposeTrade(playerB, cardA, cardB);
            
            logger.info("--- Waiting for Player B to receive proposal ---");
            if (!clientB.waitForTradeProposal(15)) {
                logger.error("Trade proposal not received by Player B!");
                System.exit(1);
            }
            
            logger.info("--- Player B accepting trade ---");
            // Use the trade ID that was received in the proposal
            String tradeId = clientB.receivedTradeId;
            if (tradeId == null) {
                logger.error("No trade ID received!");
                System.exit(1);
            }
            logger.info("Using trade ID: {}", tradeId);
            clientB.acceptTrade(tradeId);
            
            logger.info("--- Waiting for trade completion ---");
            boolean aCompleted = clientA.waitForTradeCompleted(15);
            boolean bCompleted = clientB.waitForTradeCompleted(15);
            
            if (aCompleted && bCompleted) {
                logger.info("=== ✓ TRADE TEST PASSED ===");
                logger.info("Trade completed successfully between servers!");
                System.exit(0);
            } else {
                logger.error("=== ✗ TRADE TEST FAILED ===");
                logger.error("Trade not completed. A: {}, B: {}", aCompleted, bCompleted);
                System.exit(1);
            }
            
        } catch (Exception e) {
            logger.error("Test failed with exception: {}", e.getMessage(), e);
            System.exit(1);
        } finally {
            clientA.disconnect();
            clientB.disconnect();
        }
    }
}
