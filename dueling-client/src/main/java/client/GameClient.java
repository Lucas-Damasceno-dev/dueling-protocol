        package client;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class GameClient {
    private static final Logger logger = LoggerFactory.getLogger(GameClient.class);
    private GameClient() { }

    private static final String GATEWAY_ADDRESS = System.getenv().getOrDefault("GATEWAY_HOST", "localhost");
    private static final String GATEWAY_PORT = System.getenv().getOrDefault("GATEWAY_PORT", "8080");
    private static final String API_BASE_URL = "http://" + GATEWAY_ADDRESS + ":" + GATEWAY_PORT;
    private static final int UDP_PORT = 7778;
    
    private static final boolean IS_DOCKER_ENV = System.getenv().getOrDefault("DOCKER_ENV", "false").equalsIgnoreCase("true");
    private static final String AUTO_USERNAME = System.getenv().getOrDefault("CLIENT_USERNAME", "");
    private static final String AUTO_PASSWORD = System.getenv().getOrDefault("CLIENT_PASSWORD", "");

    private static MyWebSocketClient webSocketClient;
    private static String jwtToken;
    private static String currentUsername;
    private static String currentMatchId;
    private static boolean inGame;
    private static boolean characterSet = false;
    private static boolean hasCards = false;
    private static java.util.List<String> currentHand = new java.util.ArrayList<>(); // Track current hand
    private static volatile boolean isExiting;
    private static volatile long lastPingTime = -1; // Now in nanoseconds
    private static volatile Thread pingUpdateThread;
    private static volatile boolean pingModeActive = false;
    private static volatile boolean awaitingUserInput = false;
    private static volatile boolean duringConnectionAttempt = false;
    private static volatile boolean authenticationFailureOnConnect = false;
    private static volatile long connectionStartTime;

    public static void main(String[] args) {
        // logger.info("Dueling Protocol Client Started");  // Removed to keep UI clean
        handleUserAuthentication();
    }

    private static void handleUserAuthentication() {
        if (IS_DOCKER_ENV && !AUTO_USERNAME.isEmpty() && !AUTO_PASSWORD.isEmpty()) {
            System.out.println("Running in Docker environment, attempting auto-login with provided credentials");
            autoLogin();
        } else {
            Scanner scanner = new Scanner(System.in);
            while (jwtToken == null && !isExiting) {
                System.out.println("");
                System.out.println("--- AUTHENTICATION ---");
                System.out.println("1. Login");
                System.out.println("2. Register");
                System.out.println("3. Exit");
                System.out.print("Choose an option: > ");
                String choice = scanner.nextLine().trim();

                switch (choice) {
                    case "1":
                        login(scanner);
                        break;
                    case "2":
                        register(scanner);
                        break;
                    case "3":
                        isExiting = true;
                        break;
                    default:
                        System.out.println("Invalid option.");
                        break;
                }
            }
        }

        if (jwtToken != null) {
            connectToWebSocket();
            // If authentication failed during WebSocket connection, return to authentication
            if (authenticationFailureOnConnect) {
                authenticationFailureOnConnect = false;
                handleUserAuthentication(); // Recursively call to return to auth menu
                return;
            }
            if (webSocketClient != null && webSocketClient.isOpen()) {
                if (!IS_DOCKER_ENV) {
                    startPingUpdateService();
                }
                handleUserInput();
                if (!IS_DOCKER_ENV) {
                    stopPingUpdateService();
                }
            }
        }
        // logger.info("Exiting client.");  // Removed to keep UI clean
    }
    
    private static void startPingUpdateService() {
        if (pingUpdateThread != null && pingUpdateThread.isAlive()) {
            pingUpdateThread.interrupt();
        }
        
        pingUpdateThread = new Thread(() -> {
            try (DatagramSocket datagramSocket = new DatagramSocket()) {
                datagramSocket.setSoTimeout(1000);
                long lastWebSocketPing = System.currentTimeMillis();

                while (!Thread.currentThread().isInterrupted() && webSocketClient != null && webSocketClient.isOpen()) {
                    try {
                        InetAddress address = InetAddress.getByName(GATEWAY_ADDRESS);
                        long startTime = System.nanoTime();
                        byte[] buffer = String.valueOf(startTime).getBytes();
                        DatagramPacket request = new DatagramPacket(buffer, buffer.length, address, UDP_PORT);
                        
                        datagramSocket.send(request);
                        
                        DatagramPacket response = new DatagramPacket(new byte[buffer.length], buffer.length);
                        datagramSocket.receive(response);
                        
                        long endTime = System.nanoTime();
                        lastPingTime = (endTime - startTime);

                    } catch (IOException e) {
                        lastPingTime = -1;
                    }
                    
                    // Send WebSocket PING every 15 seconds to keep connection alive during match
                    long now = System.currentTimeMillis();
                    if (now - lastWebSocketPing > 15000 && webSocketClient != null && webSocketClient.isOpen()) {
                        try {
                            webSocketClient.send("PING");
                            lastWebSocketPing = now;
                        } catch (Exception e) {
                            // Ignore ping send errors
                        }
                    }
                    
                    long sleepTime = pingModeActive ? 200 : 5000;
                    Thread.sleep(sleepTime);
                }
            } catch (Exception e) {
                // Silent error in ping service to avoid cluttering UI
            }
        }, "PingUpdateService");
        
        pingUpdateThread.setDaemon(true);
        pingUpdateThread.start();
    }
    
    private static void stopPingUpdateService() {
        if (pingUpdateThread != null && !pingUpdateThread.isInterrupted()) {
            pingUpdateThread.interrupt();
        }
    }

    private static void login(Scanner scanner) {
        try {
            System.out.print("Username: > ");
            String username = scanner.nextLine().trim();
            if (username.isEmpty()) {
                System.out.println("✗ Username cannot be empty");
                return;
            }
            
            System.out.print("Password: > ");
            String password = scanner.nextLine().trim();
            if (password.isEmpty()) {
                System.out.println("✗ Password cannot be empty");
                return;
            }

            JsonObject credentials = new JsonObject();
            credentials.addProperty("username", username);
            credentials.addProperty("password", password);

            HttpResponse response = makeHttpRequest("/api/auth/login", "POST", credentials.toString(), null);

            if (response.statusCode == 200) {
                JsonObject responseJson = new Gson().fromJson(response.body, JsonObject.class);
                jwtToken = responseJson.get("token").getAsString();
                currentUsername = username;
                System.out.println("✓ Login successful!");
            } else {
                String errorMessage = response.body != null && !response.body.isEmpty() ? response.body : "Invalid credentials";
                System.out.println("✗ Login failed: " + errorMessage);
            }
        } catch (IOException e) {
            logger.error("[ERROR] ✗ IOException during login: {}", e.getMessage());
        }
    }

    private static void autoLogin() {
        try {
            logger.info("Auto-login with username: {}", AUTO_USERNAME);

            JsonObject credentials = new JsonObject();
            credentials.addProperty("username", AUTO_USERNAME);
            credentials.addProperty("password", AUTO_PASSWORD);

            HttpResponse response = makeHttpRequest("/api/auth/login", "POST", credentials.toString(), null);

            if (response.statusCode == 200) {
                JsonObject responseJson = new Gson().fromJson(response.body, JsonObject.class);
                jwtToken = responseJson.get("token").getAsString();
                currentUsername = AUTO_USERNAME;
                System.out.println("✓ Auto-login successful!");
            } else {
                System.out.println("✗ Auto-login failed: " + response.body);
                autoRegister();
            }
        } catch (IOException e) {
            System.out.println("[ERROR] ✗ IOException during auto-login: " + e.getMessage());
        }
    }

    private static void autoRegister() {
        try {
            System.out.println("Attempting auto-registration with username: " + AUTO_USERNAME);

            JsonObject credentials = new JsonObject();
            credentials.addProperty("username", AUTO_USERNAME);
            credentials.addProperty("password", AUTO_PASSWORD);

            HttpResponse response = makeHttpRequest("/api/auth/register", "POST", credentials.toString(), null);

            if (response.statusCode == 200 || response.statusCode == 201) {
                System.out.println("✓ Auto-registration successful. Attempting to login now.");
                autoLogin();
            } else {
                System.out.println("✗ Auto-registration failed: " + response.body);
                isExiting = true;
            }
        } catch (IOException e) {
            System.out.println("[ERROR] ✗ IOException during auto-registration: " + e.getMessage());
        }
    }

    private static void register(Scanner scanner) {
        try {
            System.out.print("Username: > ");
            String username = scanner.nextLine().trim();
            if (username.isEmpty()) {
                System.out.println("✗ Username cannot be empty");
                return;
            }
            
            System.out.print("Password: > ");
            String password = scanner.nextLine().trim();
            if (password.isEmpty()) {
                System.out.println("✗ Password cannot be empty");
                return;
            }

            JsonObject credentials = new JsonObject();
            credentials.addProperty("username", username);
            credentials.addProperty("password", password);

            HttpResponse response = makeHttpRequest("/api/auth/register", "POST", credentials.toString(), null);

            if (response.statusCode == 200 || response.statusCode == 201) {
                System.out.println("✓ Registration successful. Please log in.");
            } else {
                String errorMessage = response.body != null && !response.body.isEmpty() ? response.body : "Registration failed";
                System.out.println("✗ Registration failed: " + errorMessage);
            }
        } catch (IOException e) {
            System.out.println("[ERROR] ✗ IOException during registration: " + e.getMessage());
        }
    }

    private static void connectToWebSocket() {
        int maxRetries = 10;
        int retryCount = 0;
        
        // Set the flag to indicate we're in a connection attempt
        duringConnectionAttempt = true;
        authenticationFailureOnConnect = false; // Reset the authentication failure flag
        
        while (retryCount < maxRetries) {
            try {
                // Only proceed if we still have a valid JWT token
                if (jwtToken == null || jwtToken.isEmpty()) {
                    System.out.println("JWT token is not available. Cannot connect to WebSocket.");
                    duringConnectionAttempt = false;
                    return;
                }
                
                String encodedToken = java.net.URLEncoder.encode(jwtToken, "UTF-8");
                String gatewayUri = "ws://" + GATEWAY_ADDRESS + ":" + GATEWAY_PORT + "/ws?token=" + encodedToken;
                // logger.info("Connecting to WebSocket server at {} (attempt {}/{})", gatewayUri, retryCount + 1, maxRetries);  // Removed to keep UI clean
                
                webSocketClient = new MyWebSocketClient(new URI(gatewayUri));
                if (webSocketClient.connectBlocking()) {
                    // logger.info("WebSocket connection established.");  // Removed to keep UI clean
                    // logger.info("Successfully connected to WebSocket server.");  // Removed to keep UI clean
                    duringConnectionAttempt = false;
                    authenticationFailureOnConnect = false;
                    return;
                } else {
                    logger.warn("Failed to connect to the WebSocket server on attempt {}/{}", (retryCount + 1), maxRetries);
                    // Don't set jwtToken to null here as it may still be valid; only set to null on authentication error
                }
            } catch (URISyntaxException e) {
                System.out.println("Invalid URI syntax: " + e.getMessage());
                jwtToken = null;
                duringConnectionAttempt = false;
                return;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("Connection interrupted: " + e.getMessage());
                jwtToken = null;
                duringConnectionAttempt = false;
                return;
            } catch (java.io.UnsupportedEncodingException e) {
                System.out.println("Encoding error: " + e.getMessage());
                jwtToken = null;
                duringConnectionAttempt = false;
                return;
            }
            
            retryCount++;
            if (retryCount < maxRetries) {
                System.out.println("Waiting 3 seconds before retry...");
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    System.out.println("Retry wait interrupted: " + e.getMessage());
                    jwtToken = null;
                    duringConnectionAttempt = false;
                    return;
                }
            }
        }
        
        System.out.println("Failed to connect to WebSocket server after " + maxRetries + " attempts.");
        
        // If it was an authentication failure, clear the token so user can re-authenticate
        if (authenticationFailureOnConnect) {
            jwtToken = null;
            System.out.println("Authentication failed. Please re-authenticate.");
        }
        
        duringConnectionAttempt = false;
    }

    private static void printFullMenu() {
        System.out.println();
        System.out.println("--- GAME MENU ---");
        if (!characterSet) {
            System.out.println("1. Set up character");
        } else {
            System.out.println("1. Set up character (already set up)");
        }
        System.out.println("2. Enter matchmaking queue");
        System.out.println("3. Select and use custom deck");
        System.out.println("4. Buy card pack");
        System.out.println("5. Check ping");
        System.out.println("6. Play card (during match)");
        System.out.println("7. Upgrade attributes");
        System.out.println("8. Trade cards with another player");
        System.out.println("9. 🔗 Blockchain Verification");
        System.out.println("0. Exit");
        if (lastPingTime >= 0 && !pingModeActive) {
            // Only show ping in main menu if not in ping mode
            if (lastPingTime < 1000) {
                // Less than 1 microsecond, display in nanoseconds
                System.out.println("Current ping: " + lastPingTime + " ns");
            } else if (lastPingTime < 1_000_000) {
                // Less than 1 millisecond, display in microseconds
                System.out.println("Current ping: " + (lastPingTime / 1000) + " µs");
            } else {
                // 1 millisecond or more, display in milliseconds but as microseconds in µs
                System.out.println("Current ping: " + (lastPingTime / 1000) + " µs");
            }
        }
        System.out.print("Choose an option: > ");
    }

    private static void handleUserInput() {
        // Initialize connection start time for timeout tracking
        connectionStartTime = System.currentTimeMillis();
        
        if (IS_DOCKER_ENV) {
            logger.info("Running in Docker mode - keeping WebSocket connection alive for game events");
            while (!isExiting && webSocketClient != null && webSocketClient.isOpen()) {
                // Check for game timeout only when in game
                if (inGame && isGameTimeout()) {
                    logger.warn("[TIMEOUT] Game session timed out due to inactivity.");
                    isExiting = true;
                    break;
                }
                
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.info("Client interrupted, exiting...");
                    break;
                }
            }
        } else {
            Scanner scanner = new Scanner(System.in);
            while (!isExiting && webSocketClient != null && webSocketClient.isOpen()) {
                // Check for game timeout only when in game
                if (inGame && isGameTimeout()) {
                    logger.warn("[TIMEOUT] Game session timed out due to inactivity.");
                    isExiting = true;
                    break;
                }
                
                // If not in game, allow unlimited time in menu
                awaitingUserInput = true;
                printFullMenu();
                
                String input = scanner.nextLine().trim();
                awaitingUserInput = false;
                
                if (jwtToken == null) {
                    logger.warn("[WARNING] Your session has expired. Please restart the client to log in again.");
                    isExiting = true;
                    break;
                }

                switch (input) {
                    case "1": setupCharacter(scanner); break;
                    case "2": 
                        enterMatchmaking(); 
                        // Set connection start time for potential game timeout
                        connectionStartTime = System.currentTimeMillis();
                        break;
                    case "3": selectDeck(scanner); break;
                    case "4": buyCardPack(scanner); break;
                    case "5": checkPing(scanner); break;
                    case "6":
                        if (inGame) {
                            playCard(scanner);
                            // Refresh connection start time when playing card
                            connectionStartTime = System.currentTimeMillis();
                        } else {
                            logger.warn("You need to be in a match to play cards!");
                        }
                        break;
                    case "7": upgradeAttributes(scanner); break;
                    case "8": tradeCards(scanner); break;
                    case "9": blockchainMenu(scanner); break;
                    case "0":
                        System.out.println("Exiting...");
                        isExiting = true;
                        break;
                    default:
                        System.out.println("Invalid option! Please choose a number from 0-9.");
                        break;
                }
            }
        }
        if (webSocketClient != null) {
            webSocketClient.close();
        }
        
        if (!IS_DOCKER_ENV) {
            stopPingUpdateService();
        }
    }
    
    private static boolean isGameTimeout() {
        if (!inGame) {
            return false; // No timeout when not in game
        }
        
        // Game timeout after 10 minutes (600,000 ms) of inactivity
        long timeout = 10 * 60 * 1000; // 10 minutes
        return System.currentTimeMillis() - connectionStartTime > timeout;
    }

    private static void setupCharacter(Scanner scanner) {
        if (characterSet) {
            System.out.println("You already have a character set up! Character creation is only allowed once.");
            return;
        }
        
        System.out.println("");
        System.out.println("--- CHARACTER SETUP ---");
        
        System.out.print("Enter your nickname: > ");
        String nickname = scanner.nextLine().trim();
        if (nickname.isEmpty()) {
            System.out.println("Nickname cannot be empty!");
            return;
        }
        
        System.out.println("Available races:");
        System.out.println("1. Human");
        System.out.println("2. Elf");
        System.out.println("3. Dwarf");
        System.out.println("4. Orc");
        System.out.print("Choose your race (1-4): > ");
        String raceInput = scanner.nextLine().trim();
        String race = getRaceFromInput(raceInput);
        if (race == null) {
            System.out.println("Invalid race selection. Please choose 1-4.");
            return;
        }
        
        System.out.println("Available classes:");
        System.out.println("1. Warrior");
        System.out.println("2. Mage");
        System.out.println("3. Archer");
        System.out.println("4. Rogue");
        System.out.print("Choose your class (1-4): > ");
        String classInput = scanner.nextLine().trim();
        String playerClass = getClassFromInput(classInput);
        if (playerClass == null) {
            System.out.println("Invalid class selection. Please choose 1-4.");
            return;
        }
        
        webSocketClient.send("CHARACTER_SETUP:" + nickname + ":" + race + ":" + playerClass);
        characterSet = true;
    }
    
    private static String getRaceFromInput(String input) {
        switch (input) {
            case "1": return "Human";
            case "2": return "Elf";
            case "3": return "Dwarf";
            case "4": return "Orc";
            default: return null;
        }
    }
    
    private static String getClassFromInput(String input) {
        switch (input) {
            case "1": return "Warrior";
            case "2": return "Mage";
            case "3": return "Archer";
            case "4": return "Rogue";
            default: return null;
        }
    }

    private static String selectedDeckId = null;

    private static void selectDeck(Scanner scanner) {
        if (!hasCards) {
            System.out.println("You need to have cards in your collection before selecting a deck!");
            System.out.println("Try buying a card pack first (option 4).");
            return;
        }
        
        System.out.println("");
        System.out.println("--- DECK SELECTION ---");
        System.out.println("Available decks:");
        System.out.println("1. none (default deck)");
        System.out.println("2. custom_deck_1");
        System.out.println("3. custom_deck_2");
        System.out.print("Enter the number or ID of the deck you want to use: > ");
        
        String input = scanner.nextLine().trim();
        
        if ("1".equals(input) || "none".equalsIgnoreCase(input)) {
            selectedDeckId = null;
        } else if ("2".equals(input)) {
            selectedDeckId = "custom_deck_1";
        } else if ("3".equals(input)) {
            selectedDeckId = "custom_deck_2";
        } else if (!input.isEmpty()) {
            if (input.length() < 2) {
                System.out.println("Invalid deck ID. Deck ID should be at least 2 characters.");
                return;
            }
            selectedDeckId = input;
        } else {
            selectedDeckId = null;
        }
        
        System.out.println("Deck selection updated.");
    }

    private static void enterMatchmaking() {
        if (!characterSet) {
            System.out.println("You need to set up your character first before entering matchmaking!");
            return;
        }
        
        System.out.println("Entering matchmaking queue...");
        String message = (selectedDeckId != null) ? "MATCHMAKING:ENTER:" + selectedDeckId : "MATCHMAKING:ENTER";
        safeSend(message);
    }

    private static void buyCardPack(Scanner scanner) {
        if (!characterSet) {
            System.out.println("You need to set up your character first!");
            return;
        }
        
        System.out.println("Available card packages:");
        System.out.println("1. BASIC - Basic card pack");
        System.out.println("2. PREMIUM - Premium card pack");
        System.out.println("3. LEGENDARY - Legendary card pack");
        System.out.print("Which package do you want to buy? (1-3): > ");
        
        String input = scanner.nextLine().trim();
        String packType;
        
        switch (input) {
            case "1": packType = "BASIC"; break;
            case "2": packType = "PREMIUM"; break;
            case "3": packType = "LEGENDARY"; break;
            default:
                System.out.println("Invalid package selection. Please choose 1, 2, or 3.");
                return;
        }
        
        String command = "STORE:BUY:" + packType;
        if (safeSend(command)) {
            System.out.println("Purchase command sent. Waiting for server response...");
        }
    }

    private static void playCard(Scanner scanner) {
        if (currentMatchId == null || currentMatchId.isEmpty()) {
            System.out.println("No active match to play a card in!");
            return;
        }
        
        // Show current hand if available
        if (!currentHand.isEmpty()) {
            System.out.println("\n🎴 Your Hand:");
            for (int i = 0; i < currentHand.size(); i++) {
                String cardId = currentHand.get(i);
                // Extract readable name from card ID (e.g., basic-1-abc123 -> Basic Card 1)
                String displayName = getDisplayName(cardId);
                System.out.println("  [" + (i+1) + "] " + displayName + " (" + cardId + ")");
            }
            System.out.println();
            System.out.print("Card to play (1-" + currentHand.size() + " or card ID): > ");
        } else {
            System.out.print("Card ID to play: > ");
        }
        
        String input = scanner.nextLine().trim();
        if (input.isEmpty()) {
            System.out.println("Card selection cannot be empty!");
            return;
        }
        
        String cardId;
        // Check if input is a number (card selection by index)
        try {
            int choice = Integer.parseInt(input);
            if (choice >= 1 && choice <= currentHand.size()) {
                cardId = currentHand.get(choice - 1);
                System.out.println("→ Playing: " + getDisplayName(cardId));
            } else {
                System.out.println("Invalid choice! Please select 1-" + currentHand.size());
                return;
            }
        } catch (NumberFormatException e) {
            // Input is not a number, treat as card ID
            cardId = input;
        }
        
        if (!safeSend("PLAY_CARD:" + currentMatchId + ":" + cardId)) {
            System.out.println("Please try reconnecting or restart the client.");
        }
    }
    
    /**
     * Extract display name from card ID
     * E.g., basic-1-abc123 -> Basic Card 1
     *       combo-1-def456 -> Combo Strike
     */
    private static String getDisplayName(String cardId) {
        // Try to extract template from ID (e.g., basic-1 from basic-1-abc123)
        if (cardId.contains("-")) {
            String[] parts = cardId.split("-");
            if (parts.length >= 2) {
                String template = parts[0] + "-" + parts[1];
                // Map common templates to display names
                return switch (template) {
                    case "basic-0" -> "Basic Card 0";
                    case "basic-1" -> "Basic Card 1";
                    case "basic-2" -> "Basic Card 2";
                    case "basic-3" -> "Basic Card 3";
                    case "basic-4" -> "Basic Card 4";
                    case "combo-1" -> "Combo Strike";
                    case "counter-1" -> "Counter Spell";
                    case "defense-1" -> "Light Shield";
                    case "equip-1" -> "Light Sword";
                    default -> template; // Fallback to template name
                };
            }
        }
        return cardId; // Fallback to full ID
    }

    private static void upgradeAttributes(Scanner scanner) {
        if (!characterSet) {
            System.out.println("You need to set up your character first before upgrading attributes!");
            return;
        }
        
        System.out.print("Which attribute do you want to upgrade? (BASE_ATTACK): > ");
        String attribute = scanner.nextLine().trim().toUpperCase();
        if (attribute.isEmpty()) {
            System.out.println("Attribute cannot be empty!");
            return;
        }
        
        webSocketClient.send("UPGRADE:" + attribute);
    }

    private static void tradeCards(Scanner scanner) {
        if (currentUsername == null || currentUsername.isEmpty()) {
            System.out.println("[ERROR] You must be logged in to trade cards!");
            return;
        }
        
        System.out.println("");
        System.out.println("--- TRADE CARDS ---");
        System.out.println("1. Propose a new trade");
        System.out.println("2. Accept a trade (you need the trade ID)");
        System.out.println("3. Reject a trade (you need the trade ID)");
        System.out.print("Choose an option (1-3): > ");
        String choice = scanner.nextLine().trim();
        
        switch (choice) {
            case "1":
                proposeNewTrade(scanner);
                break;
            case "2":
                acceptTrade(scanner);
                break;
            case "3":
                rejectTrade(scanner);
                break;
            default:
                System.out.println("Invalid option!");
                break;
        }
    }
    
    private static void proposeNewTrade(Scanner scanner) {
        System.out.print("Enter the username of the player you want to trade with: > ");
        String targetUsername = scanner.nextLine().trim();
        if (targetUsername.isEmpty()) {
            System.out.println("Target username cannot be empty!");
            return;
        }

        // First, request the player's card collection to help with selection
        System.out.println("Requesting your card collection to help with card selection...");
        webSocketClient.send("SHOW_CARDS");
        
        // Give server time to respond with card list
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        System.out.print("Enter the ID of the card you want to offer (comma-separated if multiple): > ");
        String offeredCards = scanner.nextLine().trim().toLowerCase();
        if (offeredCards.isEmpty()) {
            System.out.println("You must offer at least one card!");
            return;
        }

        System.out.print("Enter the ID of the card you want to request (comma-separated if multiple): > ");
        String requestedCards = scanner.nextLine().trim().toLowerCase();
        if (requestedCards.isEmpty()) {
            System.out.println("You must request at least one card!");
            return;
        }

        // Send trade proposal - WebSocketHandler will add GAME:playerId prefix automatically
        String tradeCommand = "TRADE:PROPOSE:" + targetUsername + ":" + offeredCards + ":" + requestedCards;
        webSocketClient.send(tradeCommand);
        System.out.println("Trade proposal sent! Waiting for a response from " + targetUsername);
    }
    
    private static void acceptTrade(Scanner scanner) {
        System.out.print("Enter the Trade ID to accept: > ");
        String tradeId = scanner.nextLine().trim();
        if (tradeId.isEmpty()) {
            System.out.println("Trade ID cannot be empty!");
            return;
        }
        
        String command = "TRADE:ACCEPT:" + tradeId;
        webSocketClient.send(command);
        System.out.println("Trade acceptance sent! Waiting for confirmation...");
    }
    
    private static void rejectTrade(Scanner scanner) {
        System.out.print("Enter the Trade ID to reject: > ");
        String tradeId = scanner.nextLine().trim();
        if (tradeId.isEmpty()) {
            System.out.println("Trade ID cannot be empty!");
            return;
        }
        
        String command = "TRADE:REJECT:" + tradeId;
        webSocketClient.send(command);
        System.out.println("Trade rejection sent!");
    }

    private static void checkPing(Scanner scanner) {
        pingModeActive = true;
        System.out.println("");
        System.out.println("--- REAL-TIME PING MODE ---");
        System.out.println("Press Enter to return to main menu...");

        Thread displayThread = new Thread(() -> {
            while (pingModeActive) {
                long pingNanos = lastPingTime;
                String pingDisplay;
                if (pingNanos >= 0) {
                    if (pingNanos < 1000) {
                        pingDisplay = pingNanos + " ns";
                    } else if (pingNanos < 1_000_000) {
                        pingDisplay = (pingNanos / 1000) + " µs";
                    } else {
                        pingDisplay = (pingNanos / 1000) + " µs";
                    }
                } else {
                    pingDisplay = "N/A";
                }
                System.out.print("\rCurrent ping: " + pingDisplay);
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, "PingDisplayThread");

        displayThread.setDaemon(true);
        displayThread.start();

        if (scanner.hasNextLine()) {
            scanner.nextLine();
        }

        pingModeActive = false;
        displayThread.interrupt();
        try {
            displayThread.join(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        System.out.print("\r" + " ".repeat(40) + "\r");
    }

    
    private static void blockchainMenu(Scanner scanner) {
        System.out.println();
        System.out.println("--- 🔗 BLOCKCHAIN VERIFICATION MENU ---");
        System.out.println("1. Verify Card Ownership");
        System.out.println("2. View Purchase History");
        System.out.println("3. View Match Results");
        System.out.println("4. Complete Verification (All 3)");
        System.out.println("5. Verify Specific Card by Token ID");
        System.out.println("6. Open Etherscan (View on Browser)");
        System.out.println("0. Back to Main Menu");
        System.out.print("Choose an option: > ");
        
        String choice = scanner.nextLine().trim();
        
        System.out.println("\n⚠️  Blockchain verification requires the blockchain node to be running.");
        System.out.println("Instructions:");
        System.out.println("1. Open a new terminal");
        System.out.println("2. Navigate to the project root");
        System.out.println("3. Run: ./menu.sh");
        System.out.println("4. Select option 39 to start the blockchain node");
        System.out.println("5. Keep that terminal open");
        System.out.println("6. Return here and try again\n");
        
        switch (choice) {
            case "1":
                System.out.println("\n📋 To verify your card ownership:");
                System.out.println("Open a new terminal and run:");
                System.out.println("  cd dueling-blockchain");
                System.out.println("  PLAYER_ADDRESS=0xYourAddress npm run verify:ownership");
                System.out.println("\nOr use menu.sh option 42");
                break;
            case "2":
                System.out.println("\n📦 To view your purchase history:");
                System.out.println("Open a new terminal and run:");
                System.out.println("  cd dueling-blockchain");
                System.out.println("  PLAYER_ADDRESS=0xYourAddress npm run verify:purchases");
                System.out.println("\nOr use menu.sh option 44");
                break;
            case "3":
                System.out.println("\n⚔️  To view your match results:");
                System.out.println("Open a new terminal and run:");
                System.out.println("  cd dueling-blockchain");
                System.out.println("  PLAYER_ADDRESS=0xYourAddress npm run verify:matches");
                System.out.println("\nOr use menu.sh option 45");
                break;
            case "4":
                System.out.println("\n🔍 To run complete verification:");
                System.out.println("Open a new terminal and run:");
                System.out.println("  ./scripts/blockchain-verify.sh");
                System.out.println("\nOr use menu.sh option 46");
                break;
            case "5":
                System.out.print("\nEnter Token ID: > ");
                String tokenId = scanner.nextLine().trim();
                if (!tokenId.isEmpty()) {
                    System.out.println("\n🔍 To verify card #" + tokenId + ":");
                    System.out.println("Open a new terminal and run:");
                    System.out.println("  cd dueling-blockchain");
                    System.out.println("  TOKEN_ID=" + tokenId + " npm run verify:card");
                    System.out.println("\nOr use menu.sh option 43");
                } else {
                    System.out.println("Token ID cannot be empty!");
                }
                break;
            case "6":
                System.out.println("\n🌐 View on Etherscan:");
                System.out.println("Sepolia Testnet: https://sepolia.etherscan.io/");
                System.out.println("Enter your Ethereum address in the search bar");
                System.out.println("Click 'ERC-721 Token Txns' to see your NFT cards");
                break;
            case "0":
                return;
            default:
                System.out.println("Invalid option!");
                break;
        }
        
        System.out.println("\nPress Enter to continue...");
        scanner.nextLine();
    }
    
    /**
     * Safely sends a message through WebSocket with connection check
     */
    private static boolean safeSend(String message) {
        if (webSocketClient == null || !webSocketClient.isOpen()) {
            System.out.println("❌ Connection lost!");
            return false;
        }
        
        try {
            webSocketClient.send(message);
            return true;
        } catch (org.java_websocket.exceptions.WebsocketNotConnectedException e) {
            System.out.println("❌ Failed to send message - connection lost!");
            return false;
        }
    }

    private static void processServerMessage(String message) {
        String[] parts = message.split(":");
        String type = parts[0];

        if ("PONG".equals(message)) {
            return;
        }

        // Do not print connection success message, it's implicit
        if ("SUCCESS".equals(type) && parts.length > 1 && "CONNECTED".equals(parts[1])) {
            return;
        }

        // Generic error message
        if ("ERROR".equals(type)) {
            String errorMsg = message.substring(message.indexOf(":") + 1).trim();
            System.out.println("\n✗ " + errorMsg);
            return;
        }
        
        // Handle specific success messages
        if ("SUCCESS".equals(type) && parts.length > 1) {
            if ("CHARACTER_CREATED".equals(parts[1])) {
                System.out.println("\n✓ Character created successfully!");
            } else if (message.contains("Pack purchased")) {
                System.out.println("\n✓ Pack purchased successfully!");
                hasCards = true;
            } else if (message.contains("Entered matchmaking")) {
                System.out.println("\n✓ " + message.substring(message.indexOf(":") + 1));
            } else {
                // Display other success messages
                System.out.println("\n✓ " + message.substring(message.indexOf(":") + 1));
            }
            return;
        }

        // Handle info messages, including card collection
        if ("INFO".equals(type)) {
            if (message.startsWith("INFO:YOUR_CARDS:")) {
                String cardsInfo = message.substring("INFO:YOUR_CARDS:".length());
                System.out.println("\n--- Your Card Collection ---");
                String[] cardEntries = cardsInfo.split(";");
                for (String card : cardEntries) {
                    String trimmed = card.trim();
                    // Extract just the card name (before parenthesis) for display
                    // Format from server: "Basic Card 4 (card-12345678)"
                    int parenIndex = trimmed.indexOf(" (");
                    if (parenIndex > 0) {
                        String cardName = trimmed.substring(0, parenIndex);
                        String cardId = trimmed.substring(parenIndex + 2, trimmed.length() - 1);
                        System.out.println("- " + cardName);
                        System.out.println("  ID: " + cardId);
                    } else {
                        System.out.println("- " + trimmed);
                    }
                }
                System.out.println("--------------------------");
                System.out.println("Use these card IDs for trading.");
            } else {
                System.out.println("\n" + message.substring(5)); // Remove "INFO:" prefix
            }
            return;
        }
        
        // Fallback for other server messages
        System.out.println("\n" + message);

        if ("UPDATE".equals(type)) {
            if (parts.length > 2 && "GAME_START".equals(parts[1])) {
                currentMatchId = parts[2];
                inGame = true;
                // Reset connection start time when game starts for timeout tracking
                connectionStartTime = System.currentTimeMillis();
            } else if ("GAME_OVER".equals(parts[1])) {
                inGame = false;
                currentMatchId = null;
                currentHand.clear(); // Clear hand when game ends
                // Reset connection start time after game ends
                connectionStartTime = System.currentTimeMillis();
            } else if (parts.length > 1 && "DRAW_CARDS".equals(parts[1])) {
                // Parse drawn cards: UPDATE:DRAW_CARDS:card1,card2,card3
                if (parts.length > 2) {
                    String cardsStr = parts[2];
                    currentHand.clear();
                    for (String card : cardsStr.split(",")) {
                        currentHand.add(card.trim());
                    }
                }
            }
        } else if ("CARD_UPDATE".equals(type) || message.contains("PURCHASE_SUCCESS") || message.contains("CARDS_GRANTED") || 
                   (message.startsWith("SUCCESS:Pack purchased") || message.contains("Cards received"))) {
            hasCards = true;
        }
        
        // Update connection time when receiving important game-related messages
        if (inGame && (message.contains("GAME_START") || message.contains("GAME_OVER") || 
                      message.contains("PLAY_CARD") || message.contains("TURN") || 
                      message.contains("MATCH") || message.contains("OPPONENT"))) {
            connectionStartTime = System.currentTimeMillis();
        }
    }

    private static class HttpResponse {
        int statusCode;
        String body;
        HttpResponse(int statusCode, String body) {
            this.statusCode = statusCode;
            this.body = body;
        }
    }

    private static HttpResponse makeHttpRequest(String path, String method, String jsonBody, String token) throws IOException {
        URL url = new URL(API_BASE_URL + path);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(method);
        conn.setRequestProperty("Content-Type", "application/json; utf-8");
        conn.setRequestProperty("Accept", "application/json");
        if (token != null && !token.isEmpty()) {
            conn.setRequestProperty("Authorization", "Bearer " + token);
        }
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = jsonBody.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        int statusCode = conn.getResponseCode();
        StringBuilder responseBody = new StringBuilder();
        InputStream inputStream = (statusCode >= 200 && statusCode < 300) ? conn.getInputStream() : conn.getErrorStream();
        if (inputStream != null) {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    responseBody.append(responseLine.trim());
                }
            }
        }
        conn.disconnect();
        return new HttpResponse(statusCode, responseBody.toString());
    }

    public static class MyWebSocketClient extends WebSocketClient {
        public MyWebSocketClient(URI serverUri) {
            super(serverUri);
            // Disable connection lost timeout to prevent disconnection when in menu
            // The server-side heartbeat handling will manage timeouts appropriately
            this.setConnectionLostTimeout(0); // 0 disables the automatic connection lost detection
        }

        @Override
        public void onOpen(ServerHandshake handshakedata) {
        }

        @Override
        public void onMessage(String message) {
            processServerMessage(message);
            
            // Reset connection start time when receiving any message from server
            if (inGame) {
                connectionStartTime = System.currentTimeMillis();
            }
        }

        @Override
        public void onClose(int code, String reason, boolean remote) {
            if (!isExiting) {
                logger.error("Connection lost. Code: {}, Reason: {}. Please restart the client.", code, reason);
            }
            
            // If we're in a connection attempt and getting 401, it's an authentication issue
            if (duringConnectionAttempt && reason != null && (reason.contains("401") || reason.contains("Unauthorized") || reason.contains("Invalid authentication token") || reason.contains("Authentication"))) {
                // This is an authentication failure, set the flag to re-authenticate
                authenticationFailureOnConnect = true;
            }
            
            characterSet = false;
            if (!IS_DOCKER_ENV) {
                stopPingUpdateService();
            }
        }

        @Override
        public void onError(Exception ex) {
            if (!isExiting) {
                logger.error("WebSocket error: {}", ex.getMessage());
            }
        }
    }
}
