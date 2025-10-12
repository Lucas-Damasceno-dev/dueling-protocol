package client;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Scanner;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class GameClient {
    private static final Logger logger = LoggerFactory.getLogger(GameClient.class);
    private GameClient() { }
    private static final String GATEWAY_ADDRESS = System.getenv().getOrDefault("GATEWAY_HOST", "localhost");
    private static final String GATEWAY_PORT = System.getenv().getOrDefault("GATEWAY_PORT", "8080");
    private static final int UDP_PORT = 7778; // This would also be handled by the gateway in a real setup

    private static MyWebSocketClient webSocketClient;
    private static String playerId;
    private static String currentMatchId;
    private static boolean inGame;
    private static volatile boolean isExiting;

    private static final long INITIAL_SERVER_WAIT_TIME_MS = 10000;
    private static final int PLAYER_ID_SUBSTRING_LENGTH = 8;

    public static void main(String[] args) {
        boolean shouldExit = false;
        try {
            logger.info("Waiting for gateway to start...");
            Thread.sleep(INITIAL_SERVER_WAIT_TIME_MS);

            playerId = "player-" + UUID.randomUUID().toString().substring(0, PLAYER_ID_SUBSTRING_LENGTH);
            
            // Connect through the API Gateway instead of directly to the server
            String gatewayUri = "ws://" + GATEWAY_ADDRESS + ":" + GATEWAY_PORT + "/ws?playerId=" + playerId;
            logger.info("Connecting to server through gateway at {}", gatewayUri);

            webSocketClient = new MyWebSocketClient(new URI(gatewayUri));
            if (!webSocketClient.connectBlocking()) {
                logger.error("Failed to connect to the WebSocket server through gateway.");
                shouldExit = true;
            }

            if (!shouldExit) {
                logger.info(">> Your player ID is: {}", playerId);

                if (args.length > 0) {
                    logger.info(">>>>>>>>> AUTOBOT MODE DETECTED <<<<<<<<<");
                    switch (args[0]) {
                        case "autobot":
                            runAutobot(args.length > 1 ? args[1] : "");
                            logger.info(">>>>>>>>> AUTOBOT FINISHED, EXITING MAIN <<<<<<<<<");
                            break;
                        case "maliciousbot":
                            runMaliciousBot();
                            logger.info(">>>>>>>>> AUTOBOT FINISHED, EXITING MAIN <<<<<<<<<");
                            break;
                        default:
                            logger.warn("Unknown autobot scenario: {}", args[0]);
                            break;
                    }
                    shouldExit = true;
                }
            }

            if (!shouldExit) {
                printFullMenu();
                handleUserInput();
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Main thread interrupted: {}", e.getMessage());
        } catch (URISyntaxException e) {
            logger.error("URI error in main: {}", e.getMessage(), e);
        } finally {
            if (webSocketClient != null) {
                webSocketClient.close();
            }
        }
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
        Scanner scanner = new Scanner(System.in);
        while (!isExiting && webSocketClient.isOpen()) {
            String input = scanner.nextLine().trim();

            switch (input) {
                case "1":
                    setupCharacter(scanner);
                    break;
                case "2":
                    enterMatchmaking();
                    break;
                case "3": // Previously "buyCardPack", now "select deck"
                    selectDeck(scanner);
                    break;
                case "4":
                    buyCardPack(scanner);
                    break;
                case "5":
                    checkPing();
                    break;
                case "6":
                    if (inGame) {
                        playCard(scanner);
                    } else {
                        logger.info("\nYou need to be in a match to play cards!");
                        printFullMenu();
                    }
                    break;
                case "7":
                    upgradeAttributes(scanner);
                    break;
                case "8":
                    logger.info("Exiting...");
                    isExiting = true;
                    webSocketClient.close();
                    break;
                default:
                    logger.info("\nInvalid option!");
                    printFullMenu();
                    break;
            }
        }
    }

    // AUTOBOT MODE AND SCENARIOS
    private static void runAutobot(String scenario) {
        try {
            webSocketClient.send("CHARACTER_SETUP:Elf:Warrior");
            Thread.sleep(MALICIOUS_BOT_DELAY_MS);

            switch (scenario) {
                case "matchmaking_disconnect":
                    scenarioMatchmakingDisconnect();
                    break;
                case "pre_game_disconnect":
                    scenarioPreGameDisconnect();
                    break;
                case "simultaneous_play":
                    scenarioSimultaneousPlay();
                    break;
                case "mid_game_disconnect":
                    scenarioMidGameDisconnect();
                    break;
                case "race_condition":
                    scenarioRaceCondition();
                    break;
                default:
                    scenarioDefault();
                    break;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("[autobot] Interrupted during autobot execution: {}", e.getMessage());
        }
    }

    private static final long SCENARIO_DELAY_MS = 500;

    private static void scenarioMatchmakingDisconnect() throws InterruptedException {
        if (selectedDeckId != null && !selectedDeckId.isEmpty()) {
            webSocketClient.send("MATCHMAKING:ENTER:" + selectedDeckId);
            logger.info("[autobot] Entering matchmaking with selected deck: {}", selectedDeckId);
        } else {
            webSocketClient.send("MATCHMAKING:ENTER");
        }
        Thread.sleep(SCENARIO_DELAY_MS);
        logger.info("[autobot] Disconnecting abruptly in matchmaking queue");
        webSocketClient.close();
    }

    private static final int MAX_WAIT_TIME_MS = 10000;
    private static final int WAIT_INTERVAL_MS = 200;

    private static void scenarioPreGameDisconnect() throws InterruptedException {
        if (selectedDeckId != null && !selectedDeckId.isEmpty()) {
            webSocketClient.send("MATCHMAKING:ENTER:" + selectedDeckId);
            logger.info("[autobot] Entering matchmaking with selected deck: {}", selectedDeckId);
        } else {
            webSocketClient.send("MATCHMAKING:ENTER");
        }
        int waited = 0;
        while (!inGame && waited < MAX_WAIT_TIME_MS) {
            Thread.sleep(WAIT_INTERVAL_MS);
            waited += WAIT_INTERVAL_MS;
        }
        if (!inGame) {
            logger.info("[autobot] Disconnecting abruptly before GAME_START");
            webSocketClient.close();
        }
    }

    private static void scenarioSimultaneousPlay() throws InterruptedException {
        if (selectedDeckId != null && !selectedDeckId.isEmpty()) {
            webSocketClient.send("MATCHMAKING:ENTER:" + selectedDeckId);
            logger.info("[autobot] Entering matchmaking with selected deck: {}", selectedDeckId);
        } else {
            webSocketClient.send("MATCHMAKING:ENTER");
        }
        int waited = 0;
        while (!inGame && waited < MAX_WAIT_TIME_MS) {
            Thread.sleep(WAIT_INTERVAL_MS);
            waited += WAIT_INTERVAL_MS;
        }
        if (inGame) {
            webSocketClient.send("PLAY_CARD:" + currentMatchId + ":card-001");
            logger.info("[autobot] Play sent immediately after GAME_START");
            Thread.sleep(SCENARIO_DELAY_MS);
            webSocketClient.close();
        }
    }

    private static final long MID_GAME_DISCONNECT_DELAY_MS = 300;

    private static void scenarioMidGameDisconnect() throws InterruptedException {
        if (selectedDeckId != null && !selectedDeckId.isEmpty()) {
            webSocketClient.send("MATCHMAKING:ENTER:" + selectedDeckId);
            logger.info("[autobot] Entering matchmaking with selected deck: {}", selectedDeckId);
        } else {
            webSocketClient.send("MATCHMAKING:ENTER");
        }
        int waited = 0;
        while (!inGame && waited < MAX_WAIT_TIME_MS) {
            Thread.sleep(WAIT_INTERVAL_MS);
            waited += WAIT_INTERVAL_MS;
        }
        if (inGame) {
            webSocketClient.send("PLAY_CARD:" + currentMatchId + ":card-001");
            Thread.sleep(MID_GAME_DISCONNECT_DELAY_MS);
            logger.info("[autobot] Disconnecting abruptly in the middle of the match");
            webSocketClient.close();
        }
    }

    private static void scenarioRaceCondition() throws InterruptedException {
        webSocketClient.send("STORE:BUY:BASIC");
        webSocketClient.send("UPGRADE:BASE_ATTACK");
        Thread.sleep(SCENARIO_DELAY_MS);
        webSocketClient.close();
    }

    private static final long DEFAULT_SCENARIO_TEST_TIME_MS = 20000;

    private static void scenarioDefault() throws InterruptedException {
        logger.info("[autobot] Character created. Entering matchmaking queue...");
        if (selectedDeckId != null && !selectedDeckId.isEmpty()) {
            webSocketClient.send("MATCHMAKING:ENTER:" + selectedDeckId);
            logger.info("[autobot] Entering matchmaking with selected deck: {}", selectedDeckId);
        } else {
            webSocketClient.send("MATCHMAKING:ENTER");
        }
        Thread.sleep(DEFAULT_SCENARIO_TEST_TIME_MS);
        logger.info("[autobot] Test time elapsed, exiting...");
        webSocketClient.close();
    }

    private static final long MALICIOUS_BOT_DELAY_MS = 200;

    private static void runMaliciousBot() {
        try {
            webSocketClient.send("MATCHMAKING"); // Malformed
            Thread.sleep(MALICIOUS_BOT_DELAY_MS);
            webSocketClient.send("STORE:BUY:BASIC:EXTRA_PARAM"); // Malformed
            Thread.sleep(MALICIOUS_BOT_DELAY_MS);
            webSocketClient.send("PLAY_CARD:card-001"); // Malformed
            Thread.sleep(MALICIOUS_BOT_DELAY_MS);
            webSocketClient.send("INVALIDCOMMAND");
            Thread.sleep(MALICIOUS_BOT_DELAY_MS);
            logger.info("[maliciousbot] Malformed messages sent. Exiting...");
            webSocketClient.close();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("[maliciousbot] Interrupted during malicious bot execution: {}", e.getMessage());
        }
    }

    // CLIENT ACTION METHODS
    private static void setupCharacter(Scanner scanner) {
        logger.info("\n=== CHARACTER SETUP ===");
        logger.info("Choose your race: ");
        String race = scanner.nextLine().trim();
        logger.info("Choose your class: ");
        String playerClass = scanner.nextLine().trim();
        webSocketClient.send("CHARACTER_SETUP:" + race + ":" + playerClass);
    }

    private static String selectedDeckId = null; // Store the selected deck id
    
    private static void selectDeck(Scanner scanner) {
        logger.info("\n=== DECK SELECTION ===");
        logger.info("Enter the ID of the deck you want to use (or 'none' for default):");
        String input = scanner.nextLine().trim();
        
        if ("none".equalsIgnoreCase(input) || input.isEmpty()) {
            selectedDeckId = null;
            logger.info("Using default deck for matchmaking");
        } else {
            selectedDeckId = input;
            logger.info("Selected deck ID: {}", selectedDeckId);
        }
    }
    
    private static void enterMatchmaking() {
        logger.info("\nEntering matchmaking queue...");
        if (selectedDeckId != null && !selectedDeckId.isEmpty()) {
            webSocketClient.send("MATCHMAKING:ENTER:" + selectedDeckId);
            logger.info("Entering matchmaking with deck ID: {}", selectedDeckId);
        } else {
            webSocketClient.send("MATCHMAKING:ENTER");
            logger.info("Entering matchmaking with default deck");
        }
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

    private static final int UDP_SOCKET_TIMEOUT_MS = 1000;

    public static void checkPing() {
        // UDP Ping logic - in a real implementation, this should go through the gateway too
        // For now, we'll keep it pointing to the gateway host but using the original ping port
        try (DatagramSocket datagramSocket = new DatagramSocket()) {
            InetAddress address = InetAddress.getByName(GATEWAY_ADDRESS);
            long startTime = System.currentTimeMillis();
            String message = String.valueOf(startTime);
            byte[] buffer = message.getBytes();
            DatagramPacket request = new DatagramPacket(buffer, buffer.length, address, UDP_PORT);
            datagramSocket.send(request);
            DatagramPacket response = new DatagramPacket(new byte[buffer.length], buffer.length);
            datagramSocket.setSoTimeout(UDP_SOCKET_TIMEOUT_MS);
            datagramSocket.receive(response);
            long endTime = System.currentTimeMillis();
            long latency = endTime - startTime;
            logger.info("\nPing: {} ms", latency);
            printFullMenu();
        } catch (SocketException e) {
            logger.error("\nSocket error during ping: {}", e.getMessage());
            printFullMenu();
        } catch (UnknownHostException e) {
            logger.error("\nCould not resolve host during ping: {}", e.getMessage());
            printFullMenu();
        } catch (java.io.IOException e) {
            logger.error("\nNetwork error during ping: {}", e.getMessage());
            printFullMenu();
        }
    }

    private static void processServerMessage(String message) {
        String[] parts = message.split(":");
        String type = parts[0];

        logger.info(""); 

        if ("UPDATE".equals(type)) {
            String subType = parts[1];
            switch (subType) {
                case "GAME_START":
                    currentMatchId = parts[2];
                    inGame = true;
                    logger.info(">> Match found against: {}", parts[GAME_START_OPPONENT_NICKNAME_INDEX]);
                    logger.info(">> Match ID: {}", currentMatchId);
                    break;
                case "GAME_OVER":
                    inGame = false;
                    currentMatchId = null;
                    logger.info(">> GAME OVER: You {}", 
                               parts[GAME_OVER_RESULT_INDEX].equals("VICTORY") ? "WON!" : "LOST!");
                    break;
                // Other cases...
                default:
                    logger.info("\nServer: {}", message);
                    break;
            }
        } else if ("SUCCESS".equals(type)) {
            logger.info("[SUCCESS] ✓ {}", message.substring(SUCCESS_MESSAGE_PREFIX_LENGTH));
        } else if ("ERROR".equals(type)) {
            logger.error("[ERROR] ✗ {}", message.substring(ERROR_MESSAGE_PREFIX_LENGTH));
        } else {
            logger.info("\nServer: {}", message);
        }

        printFullMenu();
    }

    private static final int SUCCESS_MESSAGE_PREFIX_LENGTH = 8;
    private static final int ERROR_MESSAGE_PREFIX_LENGTH = 6;
    private static final int GAME_START_OPPONENT_NICKNAME_INDEX = 3;
    private static final int GAME_OVER_RESULT_INDEX = 2;

    // INNER CLASS FOR WEBSOCKET CLIENT
    public static class MyWebSocketClient extends WebSocketClient {
        public MyWebSocketClient(URI serverUri) {
            super(serverUri);
        }

        @Override
        public void onOpen(ServerHandshake handshakedata) {
            logger.info("Connected to WebSocket server through gateway.");
        }

        @Override
        public void onMessage(String message) {
            processServerMessage(message);
        }

        @Override
        public void onClose(int code, String reason, boolean remote) {
            if (!isExiting) {
                logger.error("\nConnection to server lost through gateway. Code: {}, Reason: {}", code, reason);
                isExiting = true;
            }
        }

        @Override
        public void onError(Exception ex) {
            logger.error("WebSocket error through gateway: {}", ex.getMessage(), ex);
        }
    }
}