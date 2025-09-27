import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Optional;
import controller.GameFacade;
import model.Player;
import repository.PlayerRepository;
import repository.PlayerRepositoryJson;
import service.store.PurchaseResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles communication with a single client connected to the game server.
 * This class runs in its own thread and processes commands received from the client.
 */
public class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private final GameFacade gameFacade;
    private final PlayerRepository playerRepository;
    private Player player; // Stores the player for this session
    
    private static final Logger logger = LoggerFactory.getLogger(ClientHandler.class);

    /**
     * Constructs a new ClientHandler for the specified client socket and game facade.
     *
     * @param socket the socket for the connected client
     * @param facade the game facade for coordinating game operations
     */
    public ClientHandler(Socket socket, GameFacade facade) {
        this.clientSocket = socket;
        this.gameFacade = facade;
        this.playerRepository = new PlayerRepositoryJson();
    }

    /**
     * Main method that runs in a separate thread to handle client communication.
     * Reads commands from the client and processes them until the connection is closed.
     */
    @Override
    public void run() {
        try (
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))
        ) {
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                logger.debug("Command received: {}", inputLine);
                String[] command = inputLine.split(":");
                processCommand(command, out);
            }
        } catch (Exception e) {
            logger.warn("Connection with client lost: {}", e.getMessage(), e);
        } finally {
            if (player != null) {
                gameFacade.removeClient(player.getId());
                logger.info("Client {} from {} disconnected", player.getId(), clientSocket.getInetAddress().getHostAddress());
            } else {
                logger.info("Client from {} disconnected (player was not created)", clientSocket.getInetAddress().getHostAddress());
            }
        }
    }

    /**
     * Processes a command received from the client.
     *
     * @param command the command array parsed from the client input
     * @param out the PrintWriter for sending responses to the client
     */
    private void processCommand(String[] command, PrintWriter out) {
        if (command.length < 2) {
            logger.warn("Invalid command received: {}", String.join(":", command));
            out.println("ERROR:Invalid command.");
            return;
        }
        
        String action = command[0];
        String playerId = command[1];

        if (playerId == null || playerId.trim().isEmpty()) {
            logger.warn("Player ID not provided for action: {}", action);
            out.println("ERROR:Player ID not provided.");
            return;
        }

        // Load the player or create a new one, and register them
        if (this.player == null) {
            this.player = getOrCreatePlayer(playerId);
            gameFacade.registerClient(playerId, out);
        }

        Player player = getOrCreatePlayer(playerId);

        switch (action) {
            case "CHARACTER_SETUP":
                if (command.length < 4) {
                    logger.warn("Incomplete CHARACTER_SETUP command");
                    out.println("ERROR:Incomplete CHARACTER_SETUP command. Use: CHARACTER_SETUP:<playerId>:<race>:<class>");
                    return;
                }
                String race = command[2];
                String pClass = command[3];
                player.setCharacter(race, pClass);
                playerRepository.save(player);
                logger.info("Character configured for {}: {} {}", playerId, race, pClass);
                out.println("SUCCESS:Character configured as " + race + " " + pClass);
                break;

            case "MATCHMAKING":
                gameFacade.enterMatchmaking(player);
                logger.info("Player {} entered matchmaking queue", playerId);
                out.println("SUCCESS:You entered the queue.");
                break;

            case "STORE":
                if (command.length < 4) {
                    logger.warn("Incomplete STORE command");
                    out.println("ERROR:Incomplete STORE command. Use: STORE:<playerId>:BUY:<packType>");
                    return;
                }
                String packType = command[3];
                PurchaseResult result = gameFacade.buyPack(player, packType);

                if (result.isSuccess()) {
                    playerRepository.save(player); // Ensures the player's state (coins, cards) is saved
                    StringBuilder cardsStr = new StringBuilder();
                    result.getCards().forEach(c -> cardsStr.append(c.getName()).append(" (").append(c.getRarity()).append("), "));
                    String cardList = cardsStr.length() > 0 ? cardsStr.substring(0, cardsStr.length() - 2) : "None";
                    
                    logger.info("Player {} successfully bought pack of type {}.", playerId, packType);
                    out.println("SUCCESS:Pack purchased! Cards obtained: " + cardList);
                } else {
                    String errorMessage;
                    switch (result.getStatus()) {
                        case INSUFFICIENT_FUNDS:
                            errorMessage = "Insufficient coins.";
                            break;
                        case OUT_OF_STOCK:
                            errorMessage = "Pack out of stock.";
                            break;
                        case PACK_NOT_FOUND:
                        default:
                            errorMessage = "Error purchasing pack.";
                            break;
                    }
                    logger.warn("Failed to purchase pack {} for player {}: {}", packType, playerId, errorMessage);
                    out.println("ERROR:" + errorMessage);
                }
                break;

            case "GAME":
                gameFacade.processGameCommand(command, out);
                break;
            
            case "UPGRADE":
                if (command.length < 3) {
                    logger.warn("Incomplete UPGRADE command");
                    out.println("ERROR:Incomplete UPGRADE command. Use: UPGRADE:<playerId>:<attribute>");
                    return;
                }
                String attribute = command[2];
                int cost = 5;

                if (player.getUpgradePoints() >= cost) {
                    player.setUpgradePoints(player.getUpgradePoints() - cost);
                    if ("BASE_ATTACK".equals(attribute)) {
                        player.setBaseAttack(player.getBaseAttack() + 1);
                    }
                    playerRepository.save(player);
                    logger.info("Player {} successfully upgraded attribute {}", playerId, attribute);
                    out.println("SUCCESS:Upgrade applied! New attack: " + player.getBaseAttack());
                } else {
                    logger.warn("Player {} tried to upgrade attribute {} but doesn't have enough points", playerId, attribute);
                    out.println("ERROR:Insufficient upgrade points.");
                }
                break;

            default:
                logger.warn("Unknown command received: {}", action);
                out.println("ERROR:Unknown command.");
                break;
        }
    }

    /**
     * Gets an existing player by ID or creates a new one if not found.
     *
     * @param playerId the unique identifier of the player
     * @return the existing player or a newly created one
     */
    private Player getOrCreatePlayer(String playerId) {
        Optional<Player> playerOpt = playerRepository.findById(playerId);
        return playerOpt.orElseGet(() -> {
            Player newPlayer = new Player(playerId, "Player " + playerId);
            playerRepository.save(newPlayer);
            logger.info("New player created: {}", playerId);
            return newPlayer;
        });
    }
}