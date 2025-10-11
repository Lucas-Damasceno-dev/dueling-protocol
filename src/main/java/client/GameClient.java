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

public class GameClient {
    private static final String SERVER_ADDRESS = System.getenv().getOrDefault("SERVER_HOST", "localhost");
    private static final String SERVER_PORT = System.getenv().getOrDefault("SERVER_PORT", "8081");
    private static final int UDP_PORT = 7778;

    private static MyWebSocketClient webSocketClient;
    private static String playerId;
    private static String currentMatchId;
    private static boolean inGame = false;
    private static volatile boolean isExiting = false;

    public static void main(String[] args) {
        try {
            System.out.println("Waiting for servers to start...");
            Thread.sleep(10000); // 10-second delay

            playerId = "player-" + UUID.randomUUID().toString().substring(0, 8);
            String serverUri = "ws://" + SERVER_ADDRESS + ":" + SERVER_PORT + "/ws?playerId=" + playerId;
            System.out.println("Connecting to server at " + serverUri);

            webSocketClient = new MyWebSocketClient(new URI(serverUri));
            if (!webSocketClient.connectBlocking()) {
                System.err.println("Failed to connect to the WebSocket server.");
                return;
            }

            System.out.println(">> Your player ID is: " + playerId);

            if (args.length > 0) {
                switch (args[0]) {
                    case "autobot":
                        runAutobot(args.length > 1 ? args[1] : "");
                        return;
                    case "maliciousbot":
                        runMaliciousBot();
                        return;
                }
            }

            printFullMenu();
            handleUserInput();

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (webSocketClient != null) {
                webSocketClient.close();
            }
        }
    }

    private static void printFullMenu() {
        System.out.println("\n=== GAME MENU ===");
        System.out.println("1. Set up character");
        System.out.println("2. Enter matchmaking queue");
        System.out.println("3. Buy card pack");
        System.out.println("4. Check ping");
        System.out.println("5. Play card (during match)");
        System.out.println("6. Upgrade attributes");
        System.out.println("7. Exit");
        System.out.print("Choose an option: ");
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
                        System.out.println("\nYou need to be in a match to play cards!");
                        printFullMenu();
                    }
                    break;
                case "6":
                    upgradeAttributes(scanner);
                    break;
                case "7":
                    System.out.println("Exiting...");
                    isExiting = true;
                    webSocketClient.close();
                    return;
                default:
                    System.out.println("\nInvalid option!");
                    printFullMenu();
                    break;
            }
        }
    }

    // AUTOBOT MODE AND SCENARIOS
    private static void runAutobot(String scenario) {
        try {
            webSocketClient.send("CHARACTER_SETUP:Elf:Warrior");
            Thread.sleep(200);

            switch (scenario) {
                case "matchmaking_disconnect":
                    scenarioMatchmakingDisconnect();
                    return;
                case "pre_game_disconnect":
                    scenarioPreGameDisconnect();
                    return;
                case "simultaneous_play":
                    scenarioSimultaneousPlay();
                    return;
                case "mid_game_disconnect":
                    scenarioMidGameDisconnect();
                    return;
                case "race_condition":
                    scenarioRaceCondition();
                    return;
                default:
                    scenarioDefault();
                    return;
            }
        } catch (Exception e) {
            System.err.println("[autobot] Error: " + e.getMessage());
        }
    }

    private static void scenarioMatchmakingDisconnect() throws Exception {
        webSocketClient.send("MATCHMAKING:ENTER");
        Thread.sleep(500);
        System.out.println("[autobot] Disconnecting abruptly in matchmaking queue");
        webSocketClient.close();
    }

    private static void scenarioPreGameDisconnect() throws Exception {
        webSocketClient.send("MATCHMAKING:ENTER");
        int waited = 0;
        while (!inGame && waited < 10000) {
            Thread.sleep(200);
            waited += 200;
        }
        if (!inGame) {
            System.out.println("[autobot] Disconnecting abruptly before GAME_START");
            webSocketClient.close();
        }
    }

    private static void scenarioSimultaneousPlay() throws Exception {
        webSocketClient.send("MATCHMAKING:ENTER");
        int waited = 0;
        while (!inGame && waited < 10000) {
            Thread.sleep(200);
            waited += 200;
        }
        if (inGame) {
            webSocketClient.send("PLAY_CARD:" + currentMatchId + ":card-001");
            System.out.println("[autobot] Play sent immediately after GAME_START");
            Thread.sleep(500);
            webSocketClient.close();
        }
    }

    private static void scenarioMidGameDisconnect() throws Exception {
        webSocketClient.send("MATCHMAKING:ENTER");
        int waited = 0;
        while (!inGame && waited < 10000) {
            Thread.sleep(200);
            waited += 200;
        }
        if (inGame) {
            webSocketClient.send("PLAY_CARD:" + currentMatchId + ":card-001");
            Thread.sleep(300);
            System.out.println("[autobot] Disconnecting abruptly in the middle of the match");
            webSocketClient.close();
        }
    }

    private static void scenarioRaceCondition() throws Exception {
        webSocketClient.send("STORE:BUY:BASIC");
        webSocketClient.send("UPGRADE:BASE_ATTACK");
        Thread.sleep(500);
        webSocketClient.close();
    }

    private static void scenarioDefault() throws Exception {
        System.out.println("[autobot] Character created. Entering matchmaking queue...");
        webSocketClient.send("MATCHMAKING:ENTER");
        Thread.sleep(20000);
        System.out.println("[autobot] Test time elapsed, exiting...");
        webSocketClient.close();
    }

    private static void runMaliciousBot() {
        try {
            webSocketClient.send("MATCHMAKING"); // Malformed
            Thread.sleep(200);
            webSocketClient.send("STORE:BUY:BASIC:EXTRA_PARAM"); // Malformed
            Thread.sleep(200);
            webSocketClient.send("PLAY_CARD:card-001"); // Malformed
            Thread.sleep(200);
            webSocketClient.send("INVALIDCOMMAND");
            Thread.sleep(200);
            System.out.println("[maliciousbot] Malformed messages sent. Exiting...");
            webSocketClient.close();
        } catch (Exception e) {
            System.err.println("[maliciousbot] Error: " + e.getMessage());
        }
    }

    // CLIENT ACTION METHODS
    private static void setupCharacter(Scanner scanner) {
        System.out.println("\n=== CHARACTER SETUP ===");
        System.out.print("Choose your race: ");
        String race = scanner.nextLine().trim();
        System.out.print("Choose your class: ");
        String playerClass = scanner.nextLine().trim();
        webSocketClient.send("CHARACTER_SETUP:" + race + ":" + playerClass);
    }

    private static void enterMatchmaking() {
        System.out.println("\nEntering matchmaking queue...");
        webSocketClient.send("MATCHMAKING:ENTER");
    }

    private static void buyCardPack(Scanner scanner) {
        System.out.print("Which package do you want to buy? (BASIC, PREMIUM, LEGENDARY): ");
        String packType = scanner.nextLine().trim().toUpperCase();
        webSocketClient.send("STORE:BUY:" + packType);
    }

    private static void playCard(Scanner scanner) {
        System.out.print("Card ID to play: ");
        String cardId = scanner.nextLine().trim();
        webSocketClient.send("PLAY_CARD:" + currentMatchId + ":" + cardId);
    }

    private static void upgradeAttributes(Scanner scanner) {
        System.out.print("Which attribute do you want to upgrade? (BASE_ATTACK): ");
        String attribute = scanner.nextLine().trim().toUpperCase();
        webSocketClient.send("UPGRADE:" + attribute);
    }

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
            datagramSocket.setSoTimeout(1000);
            datagramSocket.receive(response);
            long endTime = System.currentTimeMillis();
            long latency = endTime - startTime;
            System.out.println("\nPing: " + latency + " ms");
            printFullMenu();
        } catch (IOException e) {
            System.err.println("\nPing failed: " + e.getMessage());
            printFullMenu();
        }
    }

    private static void processServerMessage(String message) {
        String[] parts = message.split(":");
        String type = parts[0];

        System.out.println(); 

        if ("UPDATE".equals(type)) {
            String subType = parts[1];
            switch (subType) {
                case "GAME_START":
                    currentMatchId = parts[2];
                    inGame = true;
                    System.out.println(">> Match found against: " + parts[3]);
                    System.out.println(">> Match ID: " + currentMatchId);
                    break;
                case "GAME_OVER":
                    inGame = false;
                    currentMatchId = null;
                    System.out.println(">> GAME OVER: You " + (parts[2].equals("VICTORY") ? "WON!" : "LOST!"));
                    break;
                // Other cases...
                default:
                    System.out.println("\nServer: " + message);
                    break;
            }
        } else if ("SUCCESS".equals(type)) {
            System.out.println("[SUCCESS] \u2713 " + message.substring(8));
        } else if ("ERROR".equals(type)) {
            System.out.println("[ERROR] \u2718 " + message.substring(6));
        } else {
            System.out.println("\nServer: " + message);
        }

        printFullMenu();
    }

    // INNER CLASS FOR WEBSOCKET CLIENT
    public static class MyWebSocketClient extends WebSocketClient {
        public MyWebSocketClient(URI serverUri) {
            super(serverUri);
        }

        @Override
        public void onOpen(ServerHandshake handshakedata) {
            System.out.println("Connected to WebSocket server.");
        }

        @Override
        public void onMessage(String message) {
            processServerMessage(message);
        }

        @Override
        public void onClose(int code, String reason, boolean remote) {
            if (!isExiting) {
                System.err.println("\nConnection to server lost. Code: " + code + ", Reason: " + reason);
                isExiting = true;
            }
        }

        @Override
        public void onError(Exception ex) {
            System.err.println("WebSocket error: " + ex.getMessage());
        }
    }
}
