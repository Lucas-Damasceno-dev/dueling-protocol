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

public class MatchmakingTestClient {
    private static final Logger logger = LoggerFactory.getLogger(MatchmakingTestClient.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    private final String username;
    private final String password;
    private final String serverUrl;
    private String token;
    private String playerId;
    private WebSocket webSocket;
    private final CountDownLatch characterCreatedLatch = new CountDownLatch(1);
    private final CountDownLatch gameStartLatch = new CountDownLatch(1);
    private String matchId;
    
    public MatchmakingTestClient(String username, String password, String serverUrl) {
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
        } else if (message.contains("GAME_START") || message.contains("UPDATE:GAME_START")) {
            // Extract match ID if present
            if (message.contains("Match ID:") || message.contains("matchId:")) {
                String[] parts = message.split("(Match ID:|matchId:)");
                if (parts.length > 1) {
                    this.matchId = parts[1].trim().split("[\\s,}]")[0];
                }
            }
            gameStartLatch.countDown();
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
    
    public void enterMatchmaking() throws Exception {
        logger.info("[{}] Entering matchmaking queue...", username);
        webSocket.sendText("MATCHMAKING:ENTER", true);
    }
    
    public boolean waitForGameStart(long seconds) throws Exception {
        logger.info("[{}] Waiting for game start...", username);
        return gameStartLatch.await(seconds, TimeUnit.SECONDS);
    }
    
    public String getPlayerId() {
        return playerId;
    }
    
    public String getMatchId() {
        return matchId;
    }
    
    public void disconnect() {
        if (webSocket != null) {
            webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "Test completed");
        }
    }
    
    public static void main(String[] args) {
        if (args.length < 4) {
            System.err.println("Usage: MatchmakingTestClient <userA> <passwordA> <serverUrlA> <serverUrlB>");
            System.exit(1);
        }
        
        String userA = args[0];
        String passwordA = args[1];
        String serverA = args[2]; // e.g., http://localhost:8080
        String serverB = args[3]; // e.g., http://localhost:8083
        
        String userB = userA + "_opponent";
        String passwordB = passwordA;
        
        MatchmakingTestClient clientA = new MatchmakingTestClient(userA, passwordA, serverA);
        MatchmakingTestClient clientB = new MatchmakingTestClient(userB, passwordB, serverB);
        
        try {
            logger.info("=== STARTING MATCHMAKING TEST ===");
            
            // Setup Player A
            logger.info("--- Setting up Player A on {} ---", serverA);
            clientA.registerAndLogin();
            clientA.connectWebSocket();
            clientA.createCharacter();
            Thread.sleep(2000);
            
            // Setup Player B
            logger.info("--- Setting up Player B on {} ---", serverB);
            clientB.registerAndLogin();
            clientB.connectWebSocket();
            clientB.createCharacter();
            Thread.sleep(2000);
            
            // Enter matchmaking
            logger.info("--- Both players entering matchmaking ---");
            clientA.enterMatchmaking();
            Thread.sleep(500);
            clientB.enterMatchmaking();
            
            logger.info("--- Waiting for match to be created (cross-server) ---");
            boolean aStarted = clientA.waitForGameStart(30);
            boolean bStarted = clientB.waitForGameStart(30);
            
            if (aStarted && bStarted) {
                logger.info("=== ✓ MATCHMAKING TEST PASSED ===");
                logger.info("Match created successfully between servers!");
                logger.info("Player A ({}): received GAME_START", serverA);
                logger.info("Player B ({}): received GAME_START", serverB);
                if (clientA.getMatchId() != null) {
                    logger.info("Match ID: {}", clientA.getMatchId());
                }
                System.exit(0);
            } else {
                logger.error("=== ✗ MATCHMAKING TEST FAILED ===");
                logger.error("Match not created. A received: {}, B received: {}", aStarted, bStarted);
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
