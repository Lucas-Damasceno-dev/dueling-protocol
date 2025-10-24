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
    
    // Environment variable to check if running in Docker
    private static final boolean IS_DOCKER_ENV = System.getenv().getOrDefault("DOCKER_ENV", "false").equalsIgnoreCase("true");
    private static final String AUTO_USERNAME = System.getenv().getOrDefault("CLIENT_USERNAME", "");
    private static final String AUTO_PASSWORD = System.getenv().getOrDefault("CLIENT_PASSWORD", "");

    private static MyWebSocketClient webSocketClient;
    private static String jwtToken;
    private static String currentMatchId;
    private static boolean inGame;
    private static volatile boolean isExiting;

    public static void main(String[] args) {
        logger.info("Dueling Protocol Client Started");
        handleUserAuthentication();
    }

    private static void handleUserAuthentication() {
        if (IS_DOCKER_ENV && !AUTO_USERNAME.isEmpty() && !AUTO_PASSWORD.isEmpty()) {
            // Non-interactive mode: auto-login with environment variables
            logger.info("Running in Docker environment, attempting auto-login with provided credentials");
            autoLogin();
        } else {
            // Interactive mode: prompt user for input
            Scanner scanner = new Scanner(System.in);
            while (jwtToken == null && !isExiting) {
                logger.info("\n=== AUTHENTICATION ===");
                logger.info("1. Login");
                logger.info("2. Register");
                logger.info("3. Exit");
                logger.info("Choose an option: ");
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
                        logger.warn("Invalid option.");
                        break;
                }
            }
        }

        if (jwtToken != null) {
            connectToWebSocket();
            if (webSocketClient != null && webSocketClient.isOpen()) {
                if (!IS_DOCKER_ENV) {
                    printFullMenu();
                }
                handleUserInput();
            }
        }
        logger.info("Exiting client.");
    }

    private static void login(Scanner scanner) {
        try {
            logger.info("Username: ");
            String username = scanner.nextLine().trim();
            logger.info("Password: ");
            String password = scanner.nextLine().trim();

            JsonObject credentials = new JsonObject();
            credentials.addProperty("username", username);
            credentials.addProperty("password", password);

            HttpResponse response = makeHttpRequest("/api/auth/login", "POST", credentials.toString(), null);

            if (response.statusCode == 200) {
                JsonObject responseJson = new Gson().fromJson(response.body, JsonObject.class);
                jwtToken = responseJson.get("token").getAsString();
                logger.info("[SUCCESS] \u2713 Login successful!");
            } else {
                logger.error("[ERROR] \u2717 Login failed: {} (Code: {})", response.body, response.statusCode);
            }
        } catch (IOException e) {
            logger.error("[ERROR] \u2717 IOException during login: {}", e.getMessage());
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
                logger.info("[SUCCESS] ✓ Auto-login successful!");
            } else {
                logger.error("[ERROR] ✗ Auto-login failed: {} (Code: {})", response.body, response.statusCode);
                // Attempt to register if login fails
                autoRegister();
            }
        } catch (IOException e) {
            logger.error("[ERROR] ✗ IOException during auto-login: {}", e.getMessage());
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
                logger.info("[SUCCESS] ✓ Auto-registration successful. Attempting to login now.");
                // Try login again after successful registration
                autoLogin();
            } else {
                logger.error("[ERROR] ✗ Auto-registration failed: {} (Code: {})", response.body, response.statusCode);
                isExiting = true;
            }
        } catch (IOException e) {
            logger.error("[ERROR] ✗ IOException during auto-registration: {}", e.getMessage());
        }
    }

    private static void register(Scanner scanner) {
        try {
            logger.info("Username: ");
            String username = scanner.nextLine().trim();
            logger.info("Password: ");
            String password = scanner.nextLine().trim();

            JsonObject credentials = new JsonObject();
            credentials.addProperty("username", username);
            credentials.addProperty("password", password);

            HttpResponse response = makeHttpRequest("/api/auth/register", "POST", credentials.toString(), null);

            if (response.statusCode == 200 || response.statusCode == 201) {
                logger.info("[SUCCESS] \u2713 Registration successful. Please log in.");
            } else {
                logger.error("[ERROR] \u2717 Registration failed: {} (Code: {})", response.body, response.statusCode);
            }
        } catch (IOException e) {
            logger.error("[ERROR] \u2717 IOException during registration: {}", e.getMessage());
        }
    }

    private static void connectToWebSocket() {
        int maxRetries = 10;
        int retryCount = 0;
        
        while (retryCount < maxRetries) {
            try {
                String gatewayUri = "ws://" + GATEWAY_ADDRESS + ":" + GATEWAY_PORT + "/ws?token=" + jwtToken;
                logger.info("Connecting to WebSocket server at {} (attempt {}/{})", gatewayUri, retryCount + 1, maxRetries);
                
                webSocketClient = new MyWebSocketClient(new URI(gatewayUri));
                if (webSocketClient.connectBlocking()) {
                    logger.info("Successfully connected to WebSocket server.");
                    return; // Success, exit the retry loop
                } else {
                    logger.warn("Failed to connect to the WebSocket server on attempt {}/{}", retryCount + 1, maxRetries);
                    jwtToken = null; // Clear token on connection failure
                }
            } catch (URISyntaxException e) {
                logger.error("Invalid URI syntax: {}", e.getMessage());
                jwtToken = null;
                return; // Can't recover from invalid URI
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error("Connection interrupted: {}", e.getMessage());
                jwtToken = null;
                return;
            }
            
            retryCount++;
            if (retryCount < maxRetries) {
                logger.info("Waiting 3 seconds before retry...");
                try {
                    Thread.sleep(3000); // Wait 3 seconds before retry
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.error("Retry wait interrupted: {}", e.getMessage());
                    jwtToken = null;
                    return;
                }
            }
        }
        
        logger.error("Failed to connect to WebSocket server after {} attempts.", maxRetries);
        jwtToken = null; // Clear token on connection failure
    }

    private static void printFullMenu() {
        logger.info("\n=== GAME MENU ===");
        logger.info("1. Set up character");
        logger.info("2. Enter matchmaking queue");
        logger.info("3. Select and use custom deck");
        logger.info("4. Buy card pack");
        logger.info("5. Check ping");
        logger.info("6. Play card (during match)");
        logger.info("7. Upgrade attributes");
        logger.info("8. Exit");
        logger.info("Choose an option: ");
    }

    private static void handleUserInput() {
        if (IS_DOCKER_ENV) {
            // In Docker environment, keep the connection alive but don't wait for input
            logger.info("Running in Docker mode - keeping WebSocket connection alive for game events");
            while (!isExiting && webSocketClient != null && webSocketClient.isOpen()) {
                try {
                    // Sleep for a short period to avoid busy waiting
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.info("Client interrupted, exiting...");
                    break;
                }
            }
        } else {
            // Interactive mode: wait for user input
            Scanner scanner = new Scanner(System.in);
            while (!isExiting && webSocketClient != null && webSocketClient.isOpen()) {
                String input = scanner.nextLine().trim();
                if (jwtToken == null) {
                    logger.warn("Your session has expired. Please restart the client to log in again.");
                    isExiting = true;
                    break;
                }

                switch (input) {
                    case "1": setupCharacter(scanner); break;
                    case "2": enterMatchmaking(); break;
                    case "3": selectDeck(scanner); break;
                    case "4": buyCardPack(scanner); break;
                    case "5": checkPing(); break;
                    case "6":
                        if (inGame) {
                            playCard(scanner);
                        } else {
                            logger.info("\nYou need to be in a match to play cards!");
                            printFullMenu();
                        }
                        break;
                    case "7": upgradeAttributes(scanner); break;
                    case "8":
                        logger.info("Exiting...");
                        isExiting = true;
                        break;
                    default:
                        logger.info("\nInvalid option!");
                        printFullMenu();
                        break;
                }
            }
        }
        if (webSocketClient != null) {
            webSocketClient.close();
        }
    }

    // Existing action methods (unchanged, they send messages via WebSocket)
    private static void setupCharacter(Scanner scanner) {
        logger.info("\n=== CHARACTER SETUP ===");
        logger.info("Choose your race: ");
        String race = scanner.nextLine().trim();
        logger.info("Choose your class: ");
        String playerClass = scanner.nextLine().trim();
        webSocketClient.send("CHARACTER_SETUP:" + race + ":" + playerClass);
    }

    private static String selectedDeckId = null;

    private static void selectDeck(Scanner scanner) {
        logger.info("\n=== DECK SELECTION ===");
        logger.info("Enter the ID of the deck you want to use (or 'none' for default):");
        String input = scanner.nextLine().trim();
        selectedDeckId = ("none".equalsIgnoreCase(input) || input.isEmpty()) ? null : input;
        logger.info("Deck selection updated.");
    }

    private static void enterMatchmaking() {
        logger.info("\nEntering matchmaking queue...");
        String message = (selectedDeckId != null) ? "MATCHMAKING:ENTER:" + selectedDeckId : "MATCHMAKING:ENTER";
        webSocketClient.send(message);
    }

    private static void buyCardPack(Scanner scanner) {
        logger.info("Which package do you want to buy? (BASIC, PREMIUM, LEGENDARY): ");
        String packType = scanner.nextLine().trim().toUpperCase();
        webSocketClient.send("STORE:BUY:" + packType);
    }

    private static void playCard(Scanner scanner) {
        logger.info("Card ID to play: ");
        String cardId = scanner.nextLine().trim();
        webSocketClient.send("PLAY_CARD:" + currentMatchId + ":" + cardId);
    }

    private static void upgradeAttributes(Scanner scanner) {
        logger.info("Which attribute do you want to upgrade? (BASE_ATTACK): ");
        String attribute = scanner.nextLine().trim().toUpperCase();
        webSocketClient.send("UPGRADE:" + attribute);
    }

    public static void checkPing() {
        try (DatagramSocket datagramSocket = new DatagramSocket()) {
            InetAddress address = InetAddress.getByName(GATEWAY_ADDRESS);
            long startTime = System.currentTimeMillis();
            byte[] buffer = String.valueOf(startTime).getBytes();
            DatagramPacket request = new DatagramPacket(buffer, buffer.length, address, UDP_PORT);
            datagramSocket.send(request);
            DatagramPacket response = new DatagramPacket(new byte[buffer.length], buffer.length);
            datagramSocket.setSoTimeout(1000);
            datagramSocket.receive(response);
            long endTime = System.currentTimeMillis();
            logger.info("\nPing: {} ms", (endTime - startTime));
        } catch (IOException e) {
            logger.error("\nNetwork error during ping: {}", e.getMessage());
        }
        printFullMenu();
    }

    private static void processServerMessage(String message) {
        logger.info("\nServer: {}", message);
        String[] parts = message.split(":");
        String type = parts[0];

        if ("UPDATE".equals(type)) {
            if (parts.length > 2 && "GAME_START".equals(parts[1])) {
                currentMatchId = parts[2];
                inGame = true;
            } else if ("GAME_OVER".equals(parts[1])) {
                inGame = false;
                currentMatchId = null;
            }
        }
        printFullMenu();
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
            logger.info("WebSocket connection established.");
        }

        @Override
        public void onMessage(String message) {
            processServerMessage(message);
        }

        @Override
        public void onClose(int code, String reason, boolean remote) {
            logger.error("\nConnection lost. Code: {}, Reason: {}. Please restart the client.", code, reason);
            isExiting = true;
            jwtToken = null;
        }

        @Override
        public void onError(Exception ex) {
            logger.error("WebSocket error: {}", ex.getMessage());
        }
    }
}
