package controller;

import model.Card;
import model.GameSession;
import model.Player;
import repository.PlayerRepository;
import service.matchmaking.MatchmakingService;
import service.store.StoreService;
import service.store.PurchaseResult;
import pubsub.EventManager;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class GameFacade {
    private final MatchmakingService matchmakingService;
    private final StoreService storeService;
    private final PlayerRepository playerRepository;
    private final EventManager eventManager;

    private static final Logger logger = LoggerFactory.getLogger(GameFacade.class);
    private final Map<String, GameSession> activeGames = new ConcurrentHashMap<>();

    @Autowired
    public GameFacade(MatchmakingService matchmakingService, StoreService storeService, 
                      PlayerRepository playerRepository, EventManager eventManager) {
        this.matchmakingService = matchmakingService;
        this.storeService = storeService;
        this.playerRepository = playerRepository;
        this.eventManager = eventManager;
    }

    public EventManager getEventManager() {
        return this.eventManager;
    }

    /**
     * Registers a player with the system, preparing them to receive notifications.
     *
     * @param playerId the unique identifier of the player
     */
    public void registerPlayer(String playerId) {
        // The EventManager is responsible for handling subscriptions.
        // We just need to ensure the player is known to the system.
        logger.info("Player registered in facade: {}", playerId);
    }
    
    /**
     * Unregisters a player and cleans up any active games they were involved in.
     *
     * @param playerId the unique identifier of the player
     */
    public void unregisterPlayer(String playerId) {
        // EventManager handles unsubscriptions.
        
        List<String> gamesToRemove = new ArrayList<>();
        for (Map.Entry<String, GameSession> entry : activeGames.entrySet()) {
            GameSession session = entry.getValue();
            if (session.getPlayer1().getId().equals(playerId) || session.getPlayer2().getId().equals(playerId)) {
                gamesToRemove.add(entry.getKey());
            }
        }
        
        for (String matchId : gamesToRemove) {
            GameSession session = activeGames.remove(matchId);
            if (session != null) {
                String opponentId = session.getPlayer1().getId().equals(playerId) ? 
                    session.getPlayer2().getId() : session.getPlayer1().getId();
                
                notifyPlayer(opponentId, "UPDATE:GAME_OVER:OPPONENT_DISCONNECT");
                
                logger.info("Game {} removed due to player {} disconnection", matchId, playerId);
            }
        }
        
        logger.info("Player unregistered and games cleaned up: {}", playerId);
    }

    public void enterMatchmaking(Player player) {
        matchmakingService.addPlayerToQueue(player);
        logger.info("Player {} added to matchmaking queue", player.getId());
        tryToCreateMatch();
    }

    public void tryToCreateMatch() {
        // First, try to match locally
        Optional<Match> localMatch = matchmakingService.findMatch();
        if (localMatch.isPresent()) {
            startMatch(localMatch.get());
            return;
        }

        // If no local match, try to find a remote partner for one of our waiting players
        Optional<Player> localPlayerOpt = matchmakingService.findAndLockPartner(); // Poll one player from our queue
        if (localPlayerOpt.isPresent()) {
            Player p1 = localPlayerOpt.get();
            
            List<String> remoteServers = new ArrayList<>(serverRegistry.getRegisteredServers());
            remoteServers.remove(getSelfUrl()); 

            for (String serverUrl : remoteServers) {
                try {
                    Player p2 = serverApiClient.findAndLockPartner(serverUrl);
                    if (p2 != null) {
                        logger.info("Found remote partner {} for {} on server {}", p2.getNickname(), p1.getNickname(), serverUrl);
                        startMatch(new Match(p1, p2));
                        return; // Match found, exit
                    }
                } catch (Exception e) {
                    logger.warn("Could not request partner from server {}: {}", serverUrl, e.getMessage());
                }
            }

            // If no remote partner was found, put our player back in the queue
            logger.info("No remote partner found for {}. Returning to queue.", p1.getNickname());
            matchmakingService.addPlayerToQueue(p1);
        }
    }

    private void startMatch(Match match) {
        Player p1 = match.getPlayer1();
        Player p2 = match.getPlayer2();

        String matchId = UUID.randomUUID().toString();
        List<Card> deckP1 = new ArrayList<>(p1.getCardCollection());
        List<Card> deckP2 = new ArrayList<>(p2.getCardCollection());

        GameSession session = new GameSession(matchId, p1, p2, deckP1, deckP2, this);
        session.startGame();
        activeGames.put(matchId, session);
        
        logger.info("New match created between {} and {} with ID {}", p1.getId(), p2.getId(), matchId);

        notifyPlayer(p1.getId(), "UPDATE:GAME_START:" + matchId + ":" + p2.getNickname());
        notifyPlayer(p2.getId(), "UPDATE:GAME_START:" + matchId + ":" + p1.getNickname());

        notifyPlayer(p1.getId(), "UPDATE:DRAW_CARDS:" + getCardIds(session.getHandP1()));
        notifyPlayer(p2.getId(), "UPDATE:DRAW_CARDS:" + getCardIds(session.getHandP2()));
    }

    public void notifyPlayer(String playerId, String message) {
        eventManager.publish(playerId, message);
    }

    public void notifyPlayers(List<String> playerIds, String message) {
        for (String playerId : playerIds) {
            notifyPlayer(playerId, message);
        }
    }

    private String getCardIds(List<Card> cards) {
        StringBuilder sb = new StringBuilder();
        cards.forEach(c -> sb.append(c.getId()).append(","));
        return sb.length() > 0 ? sb.substring(0, sb.length() - 1) : "";
    }

    /**
     * Processes a game command from a player.
     *
     * @param command the command array received from the client
     */
    public void processGameCommand(String[] command) {
        if (command.length < 2) { // e.g., GAME:playerId:ACTION...
            logger.warn("Invalid GAME command: {}", String.join(":", command));
            return;
        }
        String playerId = command[1];

        if (command.length < 3) {
            logger.warn("Invalid GAME command: {}", String.join(":", command));
            notifyPlayer(playerId, "ERROR:Invalid GAME command.");
            return;
        }
        
        String subAction = command[2];
        if ("PLAY_CARD".equals(subAction)) {
            if (command.length < 5) {
                logger.warn("Incomplete PLAY_CARD command");
                notifyPlayer(playerId, "ERROR:Incomplete PLAY_CARD command.");
                return;
            }
            
            String matchId = command[3];
            String cardId = command[4];
            GameSession session = activeGames.get(matchId);
            if (session != null && session.playCard(playerId, cardId)) {
                logger.info("Player {} played card {} in match {}", playerId, cardId, matchId);
                notifyPlayer(playerId, "SUCCESS:Move executed.");
            } else {
                logger.warn("Invalid move or player on cooldown. Player: {}, Card: {}, Match: {}", 
                           playerId, cardId, matchId);
                notifyPlayer(playerId, "ERROR:Invalid move or player on cooldown.");
            }
        }
    }

    public Player findPlayerById(String playerId) {
        return playerRepository.findById(playerId).orElse(null);
    }

    public PurchaseResult buyPack(Player player, String packType) {
        PurchaseResult result = storeService.purchaseCardPack(player, packType);
        if (result.isSuccess()) {
            playerRepository.save(player); // Save player state after purchase
            logger.info("Player {} bought pack of type {}", player.getId(), packType);
        }
        return result;
    }

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

        // Notify players using the EventManager
        notifyPlayer(winnerId, "UPDATE:GAME_OVER:VICTORY");
        notifyPlayer(loserId, "UPDATE:GAME_OVER:DEFEAT");
        
        logger.info("Match {} finished. Winner: {}, Loser: {}", matchId, winnerId, loserId);
    }
}
