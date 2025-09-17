import java.io.*;
import java.net.*;
import java.util.Scanner;
import java.util.UUID;

/**
 * Main client class for the Dueling Protocol card game.
 * This class handles the connection to the game server, user interaction through a menu system,
 * and automated testing scenarios. It supports both TCP communication for game commands and
 * UDP communication for ping measurements.
 */
public class GameClient {
    private static final String SERVER_ADDRESS = System.getenv().getOrDefault("SERVER_HOST", "172.16.201.6");
    private static final int TCP_PORT = 7777;
    private static final int UDP_PORT = 7778;
    
    private static Socket socket;
    private static PrintWriter out;
    private static String playerId;
    private static String currentMatchId;
    private static boolean inGame = false;
    
    // Variable to control intentional exit and avoid error message
    private static volatile boolean isExiting = false;
    
    /**
     * Main method that starts the game client.
     * Establishes connection to the server, initializes the message receiver thread,
     * and starts either the interactive menu or an automated test mode based on command line arguments.
     *
     * @param args command line arguments - if provided, can be "autobot" or "maliciousbot" for automated testing
     */
    public static void main(String[] args) {
        try {
            socket = new Socket(SERVER_ADDRESS, TCP_PORT);
            System.out.println("Connected to server at " + SERVER_ADDRESS + ":" + TCP_PORT);

            Thread receiverThread = new Thread(new ServerMessageReceiver(socket));
            receiverThread.start();

            out = new PrintWriter(socket.getOutputStream(), true);

            playerId = "player-" + UUID.randomUUID().toString().substring(0, 8);
            System.out.println(">> Your player ID is: " + playerId);

            // Automatic modes for testing
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

            // Display menu for the first time for interactive mode
            printFullMenu();
            // Start the loop that only reads user input
            handleUserInput();

        } catch (Exception e) {
            System.err.println("Error connecting to server: " + e.getMessage());
            e.printStackTrace();
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
    
    /**
     * Handles user input in a loop. Menu display is handled by other methods.
     */
    private static void handleUserInput() {
        Scanner scanner = new Scanner(System.in);
        while (!isExiting) {
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
                        printFullMenu(); // Redisplay menu since there will be no server response
                    }
                    break;
                case "6":
                    upgradeAttributes(scanner);
                    break;
                case "7":
                    System.out.println("Exiting...");
                    isExiting = true; // Signals that exit is intentional
                    try {
                        socket.close();
                    } catch (IOException e) {
                        // No need to print error, exit was requested
                    }
                    return; // End method and main thread
                default:
                    System.out.println("\nInvalid option!");
                    printFullMenu(); // Redisplay menu
                    break;
            }
        }
    }
    
    // AUTOBOT MODE AND SCENARIOS
    private static void runAutobot(String scenario) {
        try {
            out.println("CHARACTER_SETUP:" + playerId + ":Elf:Warrior");
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
        out.println("MATCHMAKING:" + playerId + ":ENTER");
        Thread.sleep(500);
        System.out.println("[autobot] Disconnecting abruptly in matchmaking queue");
        socket.close();
    }
    private static void scenarioPreGameDisconnect() throws Exception {
        out.println("MATCHMAKING:" + playerId + ":ENTER");
        int waited = 0;
        while (!inGame && waited < 10000) {
            Thread.sleep(200);
            waited += 200;
        }
        if (!inGame) {
            System.out.println("[autobot] Disconnecting abruptly before GAME_START");
            socket.close();
        }
    }
    private static void scenarioSimultaneousPlay() throws Exception {
        out.println("MATCHMAKING:" + playerId + ":ENTER");
        int waited = 0;
        while (!inGame && waited < 10000) {
            Thread.sleep(200);
            waited += 200;
        }
        if (inGame) {
            out.println("GAME:" + playerId + ":" + currentMatchId + ":PLAY_CARD:card-001");
            System.out.println("[autobot] Play sent immediately after GAME_START");
            Thread.sleep(500);
            socket.close();
        }
    }
    private static void scenarioMidGameDisconnect() throws Exception {
        out.println("MATCHMAKING:" + playerId + ":ENTER");
        int waited = 0;
        while (!inGame && waited < 10000) {
            Thread.sleep(200);
            waited += 200;
        }
        if (inGame) {
            out.println("GAME:" + playerId + ":" + currentMatchId + ":PLAY_CARD:card-001");
            Thread.sleep(300);
            System.out.println("[autobot] Disconnecting abruptly in the middle of the match");
            socket.close();
        }
    }
    private static void scenarioRaceCondition() throws Exception {
        out.println("STORE:" + playerId + ":BUY:BASIC");
        out.println("UPGRADE:" + playerId + ":BASE_ATTACK");
        Thread.sleep(500);
        socket.close();
    }
    private static void scenarioDefault() throws Exception {
        System.out.println("[autobot] Test completed, exiting...");
        socket.close();
    }
    private static void runMaliciousBot() {
        try {
            out.println("MATCHMAKING:" + playerId);
            Thread.sleep(200);
            out.println("STORE:" + playerId + ":BUY:BASIC:EXTRA_PARAM");
            Thread.sleep(200);
            out.println("GAME:PLAY_CARD:card-001");
            Thread.sleep(200);
            out.println("INVALIDCOMMAND");
            Thread.sleep(200);
            System.out.println("[maliciousbot] Malformed messages sent. Exiting...");
            socket.close();
        } catch (Exception e) {
            System.err.println("[maliciousbot] Error: " + e.getMessage());
        }
    }
    
    // CLIENT ACTION METHODS
    private static void setupCharacter(Scanner scanner) {
        System.out.println("\n=== CHARACTER SETUP ===");
        System.out.println("Available races: Elf, Dwarf, Human, Orc");
        System.out.print("Choose your race: ");
        String race = scanner.nextLine().trim();
        
        System.out.println("Available classes: Warrior, Mage, Archer, Rogue");
        System.out.print("Choose your class: ");
        String playerClass = scanner.nextLine().trim();
        
        out.println("CHARACTER_SETUP:" + playerId + ":" + race + ":" + playerClass);
    }
    
    private static void enterMatchmaking() {
        System.out.println("\nEntering matchmaking queue...");
        out.println("MATCHMAKING:" + playerId + ":ENTER");
    }
    
    private static void buyCardPack(Scanner scanner) {
        System.out.println("\n=== CARD STORE ===");
        System.out.println("Package types: BASIC, PREMIUM, LEGENDARY");
        System.out.print("Which package do you want to buy? ");
        String packType = scanner.nextLine().trim().toUpperCase();
        
        out.println("STORE:" + playerId + ":BUY:" + packType);
    }
    
    private static void playCard(Scanner scanner) {
        System.out.print("Card ID to play: ");
        String cardId = scanner.nextLine().trim();
        
        out.println("GAME:" + playerId + ":" + currentMatchId + ":PLAY_CARD:" + cardId);
    }
    
    private static void upgradeAttributes(Scanner scanner) {
        System.out.println("\n=== ATTRIBUTE UPGRADE ===");
        System.out.println("Available attributes to upgrade:");
        System.out.println("BASE_ATTACK - Cost: 5 points");
        System.out.print("Which attribute do you want to upgrade? ");
        String attribute = scanner.nextLine().trim().toUpperCase();
        
        out.println("UPGRADE:" + playerId + ":" + attribute);
    }
    
    public static void checkPing() {
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
            printFullMenu(); // Redisplay menu

        } catch (IOException e) {
            System.err.println("\nPing failed: " + e.getMessage());
            printFullMenu(); // Redisplay menu
        }
    }
    
    // INNER CLASS TO RECEIVE MESSAGES
    static class ServerMessageReceiver implements Runnable {
        private Socket socket;
        
        public ServerMessageReceiver(Socket socket) {
            this.socket = socket;
        }
        
        @Override
        public void run() {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                String response;
                while ((response = in.readLine()) != null) {
                    processServerMessage(response);
                }
            } catch (IOException e) {
                // Only display error message if exit was NOT intentional
                if (!isExiting) {
                    System.err.println("\nConnection to server lost: " + e.getMessage());
                }
            }
        }
        
        private void processServerMessage(String message) {
            String[] parts = message.split(":");
            String type = parts[0];

            System.out.println(); // Add a blank line for spacing

            if ("UPDATE".equals(type)) {
                String subType = parts[1];
                switch (subType) {
                    case "GAME_START":
                        currentMatchId = parts[2];
                        inGame = true;
                        System.out.println("=========================================");
                        System.out.println(">> Match found against: " + parts[3]);
                        System.out.println(">> Match ID: " + currentMatchId);
                        System.out.println("=========================================");
                        break;
                    case "GAME_OVER":
                        inGame = false;
                        currentMatchId = null;
                        System.out.println("=========================================");
                        System.out.println(">> GAME OVER: You " + (parts[2].equals("VICTORY") ? "WON!" : "LOST!"));
                        System.out.println("=========================================");
                        break;
                    case "ACTION":
                        System.out.println("[ACTION] >> " + parts[2]);
                        break;
                    case "HEALTH":
                        System.out.println("[HEALTH] >> " + parts[2] + " now has " + parts[3] + " health points");
                        break;
                    case "DRAW_CARDS":
                        System.out.println("[CARDS] >> Cards received: " + parts[2]);
                        break;
                    default:
                        System.out.println("\nServer: " + message);
                        break;
                }
            } else if ("SUCCESS".equals(type)) {
                System.out.println("[SUCCESS] ✓ " + message.substring(8)); // Remove "SUCCESS:" prefix
            } else if ("ERROR".equals(type)) {
                System.out.println("[ERROR] ✗ " + message.substring(6)); // Remove "ERROR:" prefix
            } else {
                System.out.println("\nServer: " + message);
            }

            // After each server message, redisplay the menu for new input
            printFullMenu();
        }
    }
}