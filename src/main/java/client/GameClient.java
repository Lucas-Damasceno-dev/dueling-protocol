package client;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.URI;
import java.util.Scanner;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class GameClient {
    private static final Logger logger = LoggerFactory.getLogger(GameClient.class);
    private GameClient() { }
    private static final String SERVER_ADDRESS = System.getenv().getOrDefault("SERVER_HOST", "localhost");
    private static final String SERVER_PORT = System.getenv().getOrDefault("SERVER_PORT", "8081");
    private static final int UDP_PORT = 7778;

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
            logger.info("Waiting for servers to start...");
            Thread.sleep(INITIAL_SERVER_WAIT_TIME_MS);

            playerId = "player-" + UUID.randomUUID().toString().substring(0, PLAYER_ID_SUBSTRING_LENGTH);
            String serverUri = "ws://" + SERVER_ADDRESS + ":" + SERVER_PORT + "/ws?playerId=" + playerId;
            logger.info("Connecting to server at {}", serverUri);

            webSocketClient = new MyWebSocketClient(new URI(serverUri));
            if (!webSocketClient.connectBlocking()) {
                logger.error("Failed to connect to the WebSocket server.");
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
        } catch (IOException | URISyntaxException e) {
            logger.error("Connection or URI error in main: {}", e.getMessage(), e);
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
        logger.info("3. Buy card pack");
        logger.info("4. Check ping");
        logger.info("5. Play card (during match)");
        logger.info("6. Upgrade attributes");
        logger.info("7. Exit");
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
                case "3":
                    buyCardPack(scanner);
                    break;
                case "4":
                    checkPing();
                    break;
                case "5":
                    if (inGame) {
                        playCard(scanner);
                    } else {
                        logger.info("\nYou need to be in a match to play cards!");
                        printFullMenu();
                    }
                    break;
                case "6":
                    upgradeAttributes(scanner);
                    break;
                case "7":
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
        } catch (IOException e) {
            logger.error("[autobot] IO error during autobot execution: {}", e.getMessage(), e);
        }
    }

    private static final long SCENARIO_DELAY_MS = 500;

    private static void scenarioMatchmakingDisconnect() throws InterruptedException {
        webSocketClient.send("MATCHMAKING:ENTER");
        Thread.sleep(SCENARIO_DELAY_MS);
        logger.info("[autobot] Disconnecting abruptly in matchmaking queue");
        webSocketClient.close();
    }

    private static final int MAX_WAIT_TIME_MS = 10000;
    private static final int WAIT_INTERVAL_MS = 200;

    private static void scenarioPreGameDisconnect() throws InterruptedException {
        webSocketClient.send("MATCHMAKING:ENTER");
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
        webSocketClient.send("MATCHMAKING:ENTER");
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
        webSocketClient.send("MATCHMAKING:ENTER");
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
        webSocketClient.send("MATCHMAKING:ENTER");
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
        } catch (IOException e) {
            logger.error("[maliciousbot] IO error during malicious bot execution: {}", e.getMessage(), e);
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

    private static void enterMatchmaking() {
        logger.info("\nEntering matchmaking queue...");
        webSocketClient.send("MATCHMAKING:ENTER");
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
        // UDP Ping logic remains the same
        try (DatagramSocket datagramSocket = new DatagramSocket()) {
            InetAddress address = InetAddress.getByName(SERVER_ADDRESS);
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
        } catch (IOException e) {
            logger.error("\nPing failed: {}", e.getMessage());
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
            logger.info("[SUCCESS] \u2713 {}", message.substring(SUCCESS_MESSAGE_PREFIX_LENGTH));
        } else if ("ERROR".equals(type)) {
            logger.error("[ERROR] \u2718 {}", message.substring(ERROR_MESSAGE_PREFIX_LENGTH));
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
            logger.info("Connected to WebSocket server.");
        }

        @Override
        public void onMessage(String message) {
            processServerMessage(message);
        }

        @Override
        public void onClose(int code, String reason, boolean remote) {
            if (!isExiting) {
                logger.error("\nConnection to server lost. Code: {}, Reason: {}", code, reason);
                isExiting = true;
            }
        }

        @Override
        public void onError(Exception ex) {
            logger.error("WebSocket error: {}", ex.getMessage(), ex);
        }
    }
}
