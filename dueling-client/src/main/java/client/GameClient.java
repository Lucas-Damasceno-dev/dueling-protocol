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
    private static String currentMatchId;
    private static boolean inGame;
    private static boolean characterSet = false;
    private static boolean hasCards = false;
    private static volatile boolean isExiting;
    private static volatile long lastPingTime = -1; // Now in nanoseconds
    private static volatile Thread pingUpdateThread;
    private static volatile boolean pingModeActive = false;
    private static volatile boolean awaitingUserInput = false;

    public static void main(String[] args) {
        // logger.info("Dueling Protocol Client Started");  // Removed to keep UI clean
        handleUserAuthentication();
    }

    private static void handleUserAuthentication() {
        if (IS_DOCKER_ENV && !AUTO_USERNAME.isEmpty() && !AUTO_PASSWORD.isEmpty()) {
            logger.info("Running in Docker environment, attempting auto-login with provided credentials");
            autoLogin();
        } else {
            Scanner scanner = new Scanner(System.in);
            while (jwtToken == null && !isExiting) {
                System.out.println();
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
                    
                    long sleepTime = pingModeActive ? 200 : 5000;
                    try {
                        Thread.sleep(sleepTime);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
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
                System.out.println("[ERROR] ✗ Username cannot be empty");
                return;
            }
            
            System.out.print("Password: > ");
            String password = scanner.nextLine().trim();
            if (password.isEmpty()) {
                System.out.println("[ERROR] ✗ Password cannot be empty");
                return;
            }

            JsonObject credentials = new JsonObject();
            credentials.addProperty("username", username);
            credentials.addProperty("password", password);

            HttpResponse response = makeHttpRequest("/api/auth/login", "POST", credentials.toString(), null);

            if (response.statusCode == 200) {
                JsonObject responseJson = new Gson().fromJson(response.body, JsonObject.class);
                jwtToken = responseJson.get("token").getAsString();
                System.out.println("[SUCCESS] ✓ Login successful!");
            } else {
                String errorMessage = response.body != null && !response.body.isEmpty() ? response.body : "Invalid credentials";
                System.out.println("[ERROR] ✗ Login failed: " + errorMessage + " (Code: " + response.statusCode + ")");
            }
        } catch (IOException e) {
            System.out.println("[ERROR] ✗ IOException during login: " + e.getMessage());
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
                System.out.println("[SUCCESS] ✓ Auto-login successful!");
            } else {
                System.out.println("[ERROR] ✗ Auto-login failed: " + response.body + " (Code: " + response.statusCode + ")");
                autoRegister();
            }
        } catch (IOException e) {
            System.out.println("[ERROR] ✗ IOException during auto-login: " + e.getMessage());
        }
    }

    private static void autoRegister() {
        try {
            logger.info("Attempting auto-registration with username: {}", AUTO_USERNAME);

            JsonObject credentials = new JsonObject();
            credentials.addProperty("username", AUTO_USERNAME);
            credentials.addProperty("password", AUTO_PASSWORD);

            HttpResponse response = makeHttpRequest("/api/auth/register", "POST", credentials.toString(), null);

            if (response.statusCode == 200 || response.statusCode == 201) {
                System.out.println("[SUCCESS] ✓ Auto-registration successful. Attempting to login now.");
                autoLogin();
            } else {
                System.out.println("[ERROR] ✗ Auto-registration failed: " + response.body + " (Code: " + response.statusCode + ")");
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
                System.out.println("[ERROR] ✗ Username cannot be empty");
                return;
            }
            
            System.out.print("Password: > ");
            String password = scanner.nextLine().trim();
            if (password.isEmpty()) {
                System.out.println("[ERROR] ✗ Password cannot be empty");
                return;
            }

            JsonObject credentials = new JsonObject();
            credentials.addProperty("username", username);
            credentials.addProperty("password", password);

            HttpResponse response = makeHttpRequest("/api/auth/register", "POST", credentials.toString(), null);

            if (response.statusCode == 200 || response.statusCode == 201) {
                System.out.println("[SUCCESS] ✓ Registration successful. Please log in.");
            } else {
                String errorMessage = response.body != null && !response.body.isEmpty() ? response.body : "Registration failed";
                System.out.println("[ERROR] ✗ Registration failed: " + errorMessage + " (Code: " + response.statusCode + ")");
            }
        } catch (IOException e) {
            System.out.println("[ERROR] ✗ IOException during registration: " + e.getMessage());
        }
    }

    private static void connectToWebSocket() {
        int maxRetries = 10;
        int retryCount = 0;
        
        while (retryCount < maxRetries) {
            try {
                String gatewayUri = "ws://" + GATEWAY_ADDRESS + ":" + GATEWAY_PORT + "/ws?token=" + jwtToken;
                // logger.info("Connecting to WebSocket server at {} (attempt {}/{})", gatewayUri, retryCount + 1, maxRetries);  // Removed to keep UI clean
                
                webSocketClient = new MyWebSocketClient(new URI(gatewayUri));
                if (webSocketClient.connectBlocking()) {
                    // logger.info("WebSocket connection established.");  // Removed to keep UI clean
                    // logger.info("Successfully connected to WebSocket server.");  // Removed to keep UI clean
                    return;
                } else {
                    System.out.println("Failed to connect to the WebSocket server on attempt " + (retryCount + 1) + "/" + maxRetries);
                    jwtToken = null;
                }
            } catch (URISyntaxException e) {
                System.out.println("Invalid URI syntax: " + e.getMessage());
                jwtToken = null;
                return;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("Connection interrupted: " + e.getMessage());
                jwtToken = null;
                return;
            }
            
            retryCount++;
            if (retryCount < maxRetries) {
                logger.info("Waiting 3 seconds before retry...");
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    System.out.println("Retry wait interrupted: " + e.getMessage());
                    jwtToken = null;
                    return;
                }
            }
        }
        
        System.out.println("Failed to connect to WebSocket server after " + maxRetries + " attempts.");
        jwtToken = null;
    }

    private static void printFullMenu() {
        System.out.println();
        System.out.println("--- GAME MENU ---");
        System.out.println("1. Set up character");
        System.out.println("2. Enter matchmaking queue");
        System.out.println("3. Select and use custom deck");
        System.out.println("4. Buy card pack");
        System.out.println("5. Check ping");
        System.out.println("6. Play card (during match)");
        System.out.println("7. Upgrade attributes");
        System.out.println("8. Exit");
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
        if (IS_DOCKER_ENV) {
            logger.info("Running in Docker mode - keeping WebSocket connection alive for game events");
            while (!isExiting && webSocketClient != null && webSocketClient.isOpen()) {
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
                awaitingUserInput = true;
                printFullMenu();
                
                String input = scanner.nextLine().trim();
                awaitingUserInput = false;
                
                if (jwtToken == null) {
                    System.out.println("[WARNING] Your session has expired. Please restart the client to log in again.");
                    isExiting = true;
                    break;
                }

                switch (input) {
                    case "1": setupCharacter(scanner); break;
                    case "2": enterMatchmaking(); break;
                    case "3": selectDeck(scanner); break;
                    case "4": buyCardPack(scanner); break;
                    case "5": checkPing(scanner); break;
                    case "6":
                        if (inGame) {
                            playCard(scanner);
                        } else {
                            System.out.println("You need to be in a match to play cards!");
                        }
                        break;
                    case "7": upgradeAttributes(scanner); break;
                    case "8":
                        System.out.println("Exiting...");
                        isExiting = true;
                        break;
                    default:
                        System.out.println("Invalid option! Please choose a number from 1-8.");
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

    private static void setupCharacter(Scanner scanner) {
        System.out.println();
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
        
        System.out.println();
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
        webSocketClient.send(message);
    }

    private static void buyCardPack(Scanner scanner) {
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
        
        webSocketClient.send("STORE:BUY:" + packType);
    }

    private static void playCard(Scanner scanner) {
        if (currentMatchId == null || currentMatchId.isEmpty()) {
            System.out.println("No active match to play a card in!");
            return;
        }
        
        System.out.print("Card ID to play: > ");
        String cardId = scanner.nextLine().trim();
        if (cardId.isEmpty()) {
            System.out.println("Card ID cannot be empty!");
            return;
        }
        
        webSocketClient.send("PLAY_CARD:" + currentMatchId + ":" + cardId);
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

    private static void checkPing(Scanner scanner) {
        pingModeActive = true;
        System.out.println();
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
                System.out.printf("\rCurrent ping: %-20s", pingDisplay);
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

    private static void processServerMessage(String message) {
        String[] parts = message.split(":");
        String type = parts[0];

        if ("PONG".equals(message)) {
            return;
        }

        if ("ERROR".equals(type)) {
            String errorMsg = message.substring(message.indexOf(":") + 1).trim();
            System.out.println("\n[ERROR] " + errorMsg);
        } else if ("SUCCESS".equals(parts[0]) && parts.length > 1 && parts[1].startsWith("CONNECTED")) {
            return;
        } else {
            System.out.println("\n[SERVER] " + message);
        }

        if ("UPDATE".equals(type)) {
            if (parts.length > 2 && "GAME_START".equals(parts[1])) {
                currentMatchId = parts[2];
                inGame = true;
            } else if ("GAME_OVER".equals(parts[1])) {
                inGame = false;
                currentMatchId = null;
            }
        } else if ("CARD_UPDATE".equals(type) || message.contains("PURCHASE_SUCCESS") || message.contains("CARDS_GRANTED")) {
            hasCards = true;
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
        }

        @Override
        public void onOpen(ServerHandshake handshakedata) {
        }

        @Override
        public void onMessage(String message) {
            processServerMessage(message);
        }

        @Override
        public void onClose(int code, String reason, boolean remote) {
            if (!isExiting) {
                System.out.println("Connection lost. Code: " + code + ", Reason: " + reason + ". Please restart the client.");
            }
            jwtToken = null;
            characterSet = false;
            if (!IS_DOCKER_ENV) {
                stopPingUpdateService();
            }
        }

        @Override
        public void onError(Exception ex) {
            if (!isExiting) {
                System.out.println("WebSocket error: " + ex.getMessage());
            }
        }
    }
}
