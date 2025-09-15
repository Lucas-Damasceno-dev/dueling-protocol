package controller;

import model.Card;
import model.GameSession;
import model.Player;
import repository.PlayerRepository;
import repository.PlayerRepositoryJson;
import service.matchmaking.ConcurrentMatchmakingService;
import service.matchmaking.MatchmakingService;
import service.store.StoreService;
import service.store.StoreServiceImpl;
import service.store.PurchaseResult;

import java.io.PrintWriter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Facade class that provides a simplified interface for game operations.
 * This class coordinates between different services and manages game sessions,
 * player connections, and game flow.
 */
public class GameFacade {
    private final MatchmakingService matchmakingService;
    private final StoreService storeService;
    private final PlayerRepository playerRepository;
    private final Map<String, GameSession> activeGames = new ConcurrentHashMap<>();
    private final Map<String, PrintWriter> activeClients = new ConcurrentHashMap<>();
    
    private static final Logger logger = LoggerFactory.getLogger(GameFacade.class);

    /**
     * Constructs a new GameFacade with default implementations for services.
     */
    public GameFacade() {
        this.matchmakingService = ConcurrentMatchmakingService.getInstance();
        this.storeService = new StoreServiceImpl();
        this.playerRepository = new PlayerRepositoryJson();
    }

    /**
     * Registers a client connection with the game facade.
     *
     * @param playerId the unique identifier of the player
     * @param writer the PrintWriter for sending messages to the client
     */
    public void registerClient(String playerId, PrintWriter writer) {
        activeClients.put(playerId, writer);
        logger.info("Client registered: {}", playerId);
    }

    /**
     * Removes a client connection from the game facade.
     *
     * @param playerId the unique identifier of the player
     */
    public void removeClient(String playerId) {
        activeClients.remove(playerId);
        logger.info("Client removed: {}", playerId);
    }

    /**
     * Adds a player to the matchmaking queue.
     *
     * @param player the player to add to the matchmaking queue
     */
    public void enterMatchmaking(Player player) {
        matchmakingService.addPlayerToQueue(player);
        logger.info("Player {} added to matchmaking queue", player.getId());
        tryToCreateMatch();
    }

    /**
     * Attempts to create a match between players in the matchmaking queue.
     * If a match is found, a new game session is created and both players are notified.
     */
    public void tryToCreateMatch() {
    matchmakingService.findMatch().ifPresent(match -> {
        Player p1 = match.getPlayer1();
        Player p2 = match.getPlayer2();

        if (!activeClients.containsKey(p1.getId()) || !activeClients.containsKey(p2.getId())) {
            logger.warn("Match cancelled. One or both players ({}, {}) disconnected before start.", p1.getId(), p2.getId());

            if (activeClients.containsKey(p1.getId())) {
                matchmakingService.addPlayerToQueue(p1);
                logger.info("Player {} returned to queue.", p1.getId());
            }
            if (activeClients.containsKey(p2.getId())) {
                matchmakingService.addPlayerToQueue(p2);
                logger.info("Player {} returned to queue.", p2.getId());
            }
            return; 
        }

        String matchId = UUID.randomUUID().toString();

        PrintWriter outP1 = activeClients.get(p1.getId());
        PrintWriter outP2 = activeClients.get(p2.getId());

        if (outP1 == null || outP2 == null) {
            logger.error("Critical failure: Active client for {} or {} not found, even after initial verification.", p1.getId(), p2.getId());
            return;
        }

        List<Card> deckP1 = new ArrayList<>(p1.getCardCollection());
        List<Card> deckP2 = new ArrayList<>(p2.getCardCollection());

        GameSession session = new GameSession(matchId, p1, p2, deckP1, deckP2, this);
        session.startGame();
        activeGames.put(matchId, session);
        
        logger.info("New match created between {} and {} with ID {}", p1.getId(), p2.getId(), matchId);

        outP1.println("UPDATE:GAME_START:" + matchId + ":" + p2.getNickname());
        outP2.println("UPDATE:GAME_START:" + matchId + ":" + p1.getNickname());

        outP1.println("UPDATE:DRAW_CARDS:" + getCardIds(session.getHandP1()));
        outP2.println("UPDATE:DRAW_CARDS:" + getCardIds(session.getHandP2()));
    });
}

    /**
     * Sends a notification message to a list of players.
     *
     * @param playerIds the list of player IDs to notify
     * @param message the message to send
     */
    public void notifyPlayers(List<String> playerIds, String message) {
        for (String id : playerIds) {
            PrintWriter writer = activeClients.get(id);
            if (writer != null) {
                writer.println(message);
            }
        }
    }

    /**
     * Converts a list of cards to a comma-separated string of card IDs.
     *
     * @param cards the list of cards to convert
     * @return a comma-separated string of card IDs
     */
    private String getCardIds(List<Card> cards) {
        StringBuilder sb = new StringBuilder();
        cards.forEach(c -> sb.append(c.getId()).append(","));
        return sb.length() > 0 ? sb.substring(0, sb.length() - 1) : "";
    }

    /**
     * Processes a game command from a player.
     *
     * @param command the command array received from the client
     * @param out the PrintWriter for sending responses to the client
     */
    public void processGameCommand(String[] command, PrintWriter out) {
        if (command.length < 3) {
            logger.warn("Invalid GAME command: {}", String.join(":", command));
            out.println("ERROR:Invalid GAME command.");
            return;
        }
        
        String subAction = command[2];
        if ("PLAY_CARD".equals(subAction)) {
            if (command.length < 5) {
                logger.warn("Incomplete PLAY_CARD command");
                out.println("ERROR:Incomplete PLAY_CARD command.");
                return;
            }
            
            String playerId = command[1];
            String matchId = command[3];
            String cardId = command[4];
            GameSession session = activeGames.get(matchId);
            if (session != null && session.playCard(playerId, cardId)) {
                logger.info("Player {} played card {} in match {}", playerId, cardId, matchId);
                out.println("SUCCESS:Move executed.");
            } else {
                logger.warn("Invalid move or player on cooldown. Player: {}, Card: {}, Match: {}", 
                           playerId, cardId, matchId);
                out.println("ERROR:Invalid move or player on cooldown.");
            }
        }
    }

    /**
     * Processes a card pack purchase for a player.
     *
     * @param player the player purchasing the card pack
     * @param packType the type of card pack to purchase
     * @return the result of the purchase operation
     */
    public PurchaseResult buyPack(Player player, String packType) {
        PurchaseResult result = storeService.purchaseCardPack(player, packType);
        if (result.isSuccess()) {
            playerRepository.save(player); // Save player state after purchase
            logger.info("Player {} bought pack of type {}", player.getId(), packType);
        }
        return result;
    }

    /**
     * Finishes a game and notifies the players of the result.
     *
     * @param matchId the unique identifier of the match
     * @param winnerId the ID of the winning player
     * @param loserId the ID of the losing player
     */
    public void finishGame(String matchId, String winnerId, String loserId) {
        if (activeGames.remove(matchId) == null) {
            logger.warn("Attempt to finish non-existent match: {}", matchId);
            return;
        }
        
        Optional<Player> winnerOpt = playerRepository.findById(winnerId);
        if (winnerOpt.isPresent()) {
            Player winner = winnerOpt.get();
            int pointsEarned = 10;
            winner.setUpgradePoints(winner.getUpgradePoints() + pointsEarned);
            playerRepository.update(winner);
            logger.info("Match {} finished. {} won {} points!", matchId, winner.getNickname(), pointsEarned);
        }

        PrintWriter winnerOut = activeClients.get(winnerId);
        if (winnerOut != null) {
            winnerOut.println("UPDATE:GAME_OVER:VICTORY");
        }

        PrintWriter loserOut = activeClients.get(loserId);
        if (loserOut != null) {
            loserOut.println("UPDATE:GAME_OVER:DEFEAT");
        }
        
        logger.info("Match {} finished. Winner: {}, Loser: {}", matchId, winnerId, loserId);
    }
}