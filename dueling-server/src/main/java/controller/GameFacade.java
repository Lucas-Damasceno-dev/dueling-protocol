package controller;

import api.ServerApiClient;
import api.registry.ServerRegistry;
import model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import pubsub.IEventManager;
import repository.CardRepository;

import repository.PlayerRepository;
import service.deck.DeckService;
import service.election.LeaderElectionService;
import service.matchmaking.MatchmakingService;
import service.store.PurchaseResult;
import service.store.StoreService;
import service.trade.TradeService;


import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Central coordinator for game operations in the Dueling Protocol system.
 * 
 * <p>This class acts as the main entry point for all game-related functionality,
 * coordinating between different services like matchmaking, store, trading, etc.
 * It also manages active game sessions and player registrations.</p>
 * 
 * <p>The GameFacade is responsible for:</p>
 * <ul>
 *   <li>Managing player registration and unregistration</li>
 *   <li>Coordinating matchmaking between players</li>
 *   <li>Handling game session creation and management</li>
 *   <li>Processing game commands from clients</li>
 *   <li>Managing trading between players</li>
 *   <li>Distributing daily rewards to active players</li>
 * </ul>
 * 
 * <p>This class is annotated with {@code @Profile("server")} to ensure it only runs
 * in server mode, and with {@code @Service} to register it as a Spring service bean.</p>
 * 
 * @author Dueling Protocol Team
 * @since 1.0
 */
@Profile("server")
@Service
public class GameFacade {
    private final MatchmakingService matchmakingService;
    private final StoreService storeService;
    private final PlayerRepository playerRepository;
    private final repository.JpaPlayerRepository jpaPlayerRepository;
    private final IEventManager eventManager;
    private final ServerRegistry serverRegistry;
    private final ServerApiClient serverApiClient;
    private final TradeService tradeService;
    private final LeaderElectionService leaderElectionService;
    private final CardRepository cardRepository;
    private final DeckService deckService;
    

    @Value("${server.name}")
    private String serverName;
    @Value("${server.port}")
    private String serverPort;
    private String selfUrl;

    private static final Logger logger = LoggerFactory.getLogger(GameFacade.class);
    private final Map<String, GameSession> activeGames = new ConcurrentHashMap<>();

    @Autowired
    public GameFacade(MatchmakingService matchmakingService, StoreService storeService,
                      PlayerRepository playerRepository, repository.JpaPlayerRepository jpaPlayerRepository,
                      IEventManager eventManager, ServerRegistry serverRegistry, ServerApiClient serverApiClient,
                      TradeService tradeService, LeaderElectionService leaderElectionService,
                      CardRepository cardRepository, DeckService deckService) {
        this.matchmakingService = matchmakingService;
        this.storeService = storeService;
        this.playerRepository = playerRepository;
        this.jpaPlayerRepository = jpaPlayerRepository;
        this.eventManager = eventManager;
        this.serverRegistry = serverRegistry;
        this.serverApiClient = serverApiClient;
        this.tradeService = tradeService;
        this.leaderElectionService = leaderElectionService;
        this.cardRepository = cardRepository;
        this.deckService = deckService;
    }

    /**
     * Constructs and returns the URL of the current server instance.
     * The URL is built using the configured server name and port.
     *
     * @return The full URL of the current server (e.g., "http://localhost:8080").
     */
    private String getSelfUrl() {
        if (selfUrl == null) {
            selfUrl = "http://" + serverName + ":" + serverPort;
        }
        return selfUrl;
    }

    /**
     * Retrieves the event manager instance used by the game facade.
     *
     * @return The {@link IEventManager} instance for publishing and subscribing to game events.
     */
    public IEventManager getEventManager() {
        return this.eventManager;
    }

    /**
     * Registers a player in the game facade.
     * 
     * <p>This method is called when a new player connects to the WebSocket server.
     * It logs the player registration and prepares the player for game interactions.</p>
     * 
     * @param playerId the unique identifier of the player to register
     */
    public void registerPlayer(String playerId) {
        logger.info("Player registered in facade: {}", playerId);
    }
    
    /**
     * Unregisters a player from the game facade and cleans up associated game sessions.
     * 
     * <p>This method is called when a player disconnects from the WebSocket server.
     * It removes the player from any active games and notifies their opponents
     * about the disconnection.</p>
     * 
     * @param playerId the unique identifier of the player to unregister
     */
    public void unregisterPlayer(String playerId) {
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

    /**
     * Enters a player into the matchmaking queue to find an opponent for a duel.
     * 
     * <p>This method adds the player to the matchmaking queue and attempts to create
     * a match immediately. If a suitable opponent is found, a new game session is started.</p>
     * 
     * @param player the player to enter into matchmaking
     */
    public void enterMatchmaking(Player player) {
        matchmakingService.addPlayerToQueue(player);
        logger.info("Player {} added to matchmaking queue", player.getId());
        tryToCreateMatch();
    }
    
    /**
     * Enters a player into the matchmaking queue to find an opponent for a duel, with a specific deck.
     * 
     * <p>This method adds the player to the matchmaking queue with a specific deck and attempts to create
     * a match immediately. If a suitable opponent is found, a new game session is started using the specified deck.</p>
     * 
     * @param player the player to enter into matchmaking
     * @param deckId the ID of the deck to use in the game
     */
    public void enterMatchmaking(Player player, String deckId) {
        // Create a matchmaking entry that includes the deck selection
        matchmakingService.addPlayerToQueueWithDeck(player, deckId);
        logger.info("Player {} added to matchmaking queue with deck {}", player.getId(), deckId);
        tryToCreateMatch();
    }

    /**
     * Attempts to create a match by looking for players in the local matchmaking queue
     * and, if necessary, requesting a partner from other registered servers.
     * If a match is found or a remote partner is successfully locked, a new game session is started.
     */
    public void tryToCreateMatch() {
        Optional<Match> localMatch = matchmakingService.findMatch();
        if (localMatch.isPresent()) {
            startMatch(localMatch.get());
            return;
        }

        Optional<Player> localPlayerOpt = matchmakingService.findAndLockPartner();
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
                        return;
                    }
                } catch (Exception e) {
                    logger.warn("Could not request partner from server {}: {}", serverUrl, e.getMessage());
                }
            }

            logger.info("No remote partner found for {}. Returning to queue.", p1.getNickname());
            matchmakingService.addPlayerToQueue(p1);
        }
    }

    /**
     * Initiates a new game session for the given match.
     * This involves creating a {@link GameSession}, distributing initial cards,
     * and notifying both players about the game start.
     *
     * @param match The {@link Match} object containing the two players for the game.
     */
    private void startMatch(Match match) {
        Player p1 = match.getPlayer1();
        Player p2 = match.getPlayer2();

        String matchId = UUID.randomUUID().toString();
        // Use default deck if available, otherwise use the full card collection
        List<Card> deckP1 = new ArrayList<>(p1.getCardCollection());
        List<Card> deckP2 = new ArrayList<>(p2.getCardCollection());

        GameSession session = new GameSession(matchId, p1, p2, deckP1, deckP2, this, cardRepository);
        activeGames.put(matchId, session);
        
        session.startGame(); // Session now handles its own start notifications
        
        logger.info("New match created between {} and {} with ID {}", p1.getId(), p2.getId(), matchId);
    }
    
    /**
     * Initiates a new game session for the given match with specific decks.
     * This involves creating a {@link GameSession}, distributing initial cards from the selected decks,
     * and notifying both players about the game start.
     *
     * @param match The {@link Match} object containing the two players for the game.
     * @param deckId1 The ID of the deck to use for player 1
     * @param deckId2 The ID of the deck to use for player 2
     */
    private void startMatchWithDecks(Match match, String deckId1, String deckId2) {
        Player p1 = match.getPlayer1();
        Player p2 = match.getPlayer2();

        String matchId = UUID.randomUUID().toString();
        
        // Get cards from the specified decks
        List<Card> deckP1 = getDeckCards(p1.getId(), deckId1);
        List<Card> deckP2 = getDeckCards(p2.getId(), deckId2);
        
        // If deck not found or invalid, fallback to default deck or full collection
        if (deckP1 == null) {
            deckP1 = getDefaultDeckCards(p1.getId());
            if (deckP1 == null) {
                deckP1 = new ArrayList<>(p1.getCardCollection()); // Fallback to full collection
            }
        }
        
        if (deckP2 == null) {
            deckP2 = getDefaultDeckCards(p2.getId());
            if (deckP2 == null) {
                deckP2 = new ArrayList<>(p2.getCardCollection()); // Fallback to full collection
            }
        }

        GameSession session = new GameSession(matchId, p1, p2, deckP1, deckP2, this, cardRepository);
        activeGames.put(matchId, session);

        session.startGame(); // Session now handles its own start notifications

        logger.info("New match created between {} and {} with ID {} using decks {} and {}", 
                   p1.getId(), p2.getId(), matchId, deckId1, deckId2);
    }
    
    /**
     * Retrieves cards from a specific deck for a player.
     *
     * @param playerId The ID of the player
     * @param deckId The ID of the deck to retrieve
     * @return List of cards in the specified deck, or null if not found
     */
    private List<Card> getDeckCards(String playerId, String deckId) {
        Optional<model.Deck> deckOpt = deckService.getDeckForPlayer(deckId, playerId);
        if (deckOpt.isPresent()) {
            return deckOpt.get().getCards();
        }
        return null;
    }
    
    /**
     * Retrieves cards from the default deck for a player.
     *
     * @param playerId The ID of the player
     * @return List of cards in the default deck, or null if not found
     */
    private List<Card> getDefaultDeckCards(String playerId) {
        Optional<model.Deck> deckOpt = deckService.getDefaultDeck(playerId);
        if (deckOpt.isPresent()) {
            return deckOpt.get().getCards();
        }
        return null;
    }

    /**
     * Notifies a specific player with a given message.
     * This method uses the {@link IEventManager} to publish the message to the player's channel.
     *
     * @param playerId The unique identifier of the player to notify.
     * @param message The message to send to the player.
     */
    public void notifyPlayer(String playerId, String message) {
        eventManager.publish(playerId, message);
    }

    /**
     * Notifies a list of players with a given message.
     * This method iterates through the provided player IDs and calls {@link #notifyPlayer(String, String)}
     * for each player.
     *
     * @param playerIds A list of unique identifiers of the players to notify.
     * @param message The message to send to all specified players.
     */
    public void notifyPlayers(List<String> playerIds, String message) {
        for (String playerId : playerIds) {
            notifyPlayer(playerId, message);
        }
    }

    /**
     * Extracts and concatenates the IDs of a list of cards into a single comma-separated string.
     *
     * @param cards The list of {@link Card} objects from which to extract IDs.
     * @return A comma-separated string of card IDs, or an empty string if the list is empty.
     */
    private String getCardIds(List<Card> cards) {
        StringBuilder sb = new StringBuilder();
        cards.forEach(c -> sb.append(c.getId()).append(","));
        return sb.length() > 0 ? sb.substring(0, sb.length() - 1) : "";
    }

    /**
     * Processes a game command received from a player.
     * This method parses the command array and delegates to appropriate handlers
     * based on the action specified in the command.
     *
     * @param command A string array representing the parsed game command.
     *                Expected format: ["COMMAND_TYPE", "PLAYER_ID", "ACTION", ...]
     */
    public void processGameCommand(String[] command) {
        if (command.length < 3) {
            logger.warn("Invalid command structure: {}", String.join(":", command));
            return;
        }
        String playerId = command[1];
        String action = command[2];

        Player player = playerRepository.findById(playerId).orElse(null);
        if (player == null && !"CHARACTER_SETUP".equals(action)) {
            notifyPlayer(playerId, "ERROR:Player not found. Please set up your character first.");
            return;
        }

        switch (action) {
            case "CHARACTER_SETUP":
                if (command.length < 6) {
                    notifyPlayer(playerId, "ERROR:Incomplete character setup command. Expected: NICKNAME:RACE:CLASS");
                    return;
                }
                // First, create the player object with ID and Nickname
                Player newPlayer = new Player(playerId, command[3]);
                // Then, set the character Race and Class, which also applies attributes
                newPlayer.setCharacter(command[4], command[5]);
                playerRepository.save(newPlayer);
                notifyPlayer(playerId, "SUCCESS:Character created.");
                break;

            case "MATCHMAKING":
                if (command.length > 3 && "ENTER".equals(command[3])) {
                    String deckId = null;
                    if (command.length > 4) {
                        deckId = command[4]; // Optional deck ID parameter
                    }
                    
                    if (deckId != null && !deckId.isEmpty()) {
                        // Verify that the deck belongs to the player and is valid
                        if (deckService.isValidDeckForGame(deckId, playerId)) {
                            enterMatchmaking(player, deckId);
                            notifyPlayer(playerId, "SUCCESS:Entered matchmaking queue with deck: " + deckId);
                        } else {
                            notifyPlayer(playerId, "ERROR:Invalid or non-existent deck: " + deckId);
                        }
                    } else {
                        // Use default deck if available, otherwise use full collection
                        enterMatchmaking(player);
                        notifyPlayer(playerId, "SUCCESS:Entered matchmaking queue with default deck.");
                    }
                }
                break;

            case "PLAY_CARD":
                if (command.length < 5) {
                    notifyPlayer(playerId, "ERROR:Incomplete PLAY_CARD command.");
                    return;
                }
                String matchId = command[3];
                String cardId = command[4];
                GameSession session = activeGames.get(matchId);
                if (session != null) {
                    session.playCard(playerId, cardId);
                } else {
                    notifyPlayer(playerId, "ERROR:Match not found for PLAY_CARD command.");
                }
                break;

            case "STORE":
                if (command.length > 4 && "BUY".equals(command[3])) {
                    String packType = command[4];
                    PurchaseResult result = buyPack(player, packType);
                    if (result.isSuccess()) {
                        notifyPlayer(playerId, "SUCCESS:Pack purchased. You got " + result.getCards().size() + " cards.");
                    } else {
                        notifyPlayer(playerId, "ERROR:Purchase failed: " + result.getStatus());
                    }
                }
                break;
                
            case "TRADE":
                if (command.length < 4) {
                    notifyPlayer(playerId, "ERROR:Incomplete TRADE command.");
                    return;
                }
                
                String tradeAction = command[3];
                if ("PROPOSE".equals(tradeAction) && command.length >= 7) {
                    String targetPlayerId = command[4];
                    String[] offeredCards = command[5].split(",");
                    String[] requestedCards = command[6].split(",");
                    
                    handleTradeProposal(playerId, targetPlayerId, offeredCards, requestedCards);
                } else if ("ACCEPT".equals(tradeAction) && command.length >= 5) {
                    String tradeId = command[4];
                    handleTradeAcceptance(playerId, tradeId);
                } else if ("REJECT".equals(tradeAction) && command.length >= 5) {
                    String tradeId = command[4];
                    handleTradeRejection(playerId, tradeId);
                } else {
                    notifyPlayer(playerId, "ERROR:Invalid trade command format or action.");
                }
                break;
                
            case "PRIVATE_MESSAGE":
                if (command.length < 4) {
                    notifyPlayer(playerId, "ERROR:Incomplete PRIVATE_MESSAGE command.");
                    return;
                }
                
                String recipientId = command[3];
                String messageContent = String.join(":", java.util.Arrays.copyOfRange(command, 4, command.length));
                
                handlePrivateMessage(playerId, recipientId, messageContent);
                break;

            default:
                notifyPlayer(playerId, "ERROR:Unknown command '" + action + "'.");
                break;
        }
    }
    
    /**
     * Handle sending a private message from one player to another
     */
    private void handlePrivateMessage(String senderId, String recipientId, String content) {
        // Validate that sender and recipient are friends (optional, can be configured based on requirements)
        // For now, we'll allow any player to send a message to any other player
        // In a more robust system, you could verify friendship status here
        
        // Send the private message using the event manager
        if (eventManager instanceof pubsub.RedisEventManager) {
            pubsub.RedisEventManager redisEventManager = (pubsub.RedisEventManager) eventManager;
            redisEventManager.sendPrivateMessage(senderId, recipientId, content);
            
            // Confirm to the sender that the message was sent
            notifyPlayer(senderId, "PRIVATE_MESSAGE_SENT:" + recipientId + ":" + content);
        } else {
            // Fallback to regular publish if not using RedisEventManager
            // In this case, we'll use a private message channel
            String channel = "private-messages:" + recipientId;
            String message = "PRIVATE:" + senderId + ":" + content;
            eventManager.publish(channel, message);
            
            // Confirm to the sender
            notifyPlayer(senderId, "PRIVATE_MESSAGE_SENT:" + recipientId + ":" + content);
        }
    }

    /**
     * Finds a player by their unique identifier.
     *
     * @param playerId The unique identifier of the player to find.
     * @return The {@link Player} object if found, otherwise {@code null}.
     */
    public Player findPlayerById(String playerId) {
        return playerRepository.findById(playerId).orElse(null);
    }

    /**
     * Allows a player to purchase a card pack from the store.
     * The player's card collection and coin balance are updated upon successful purchase.
     *
     * @param player The {@link Player} attempting to buy the pack.
     * @param packType The type of card pack to purchase (e.g., "STARTER", "PREMIUM").
     * @return A {@link PurchaseResult} indicating the outcome of the purchase, including any cards received.
     */
    public PurchaseResult buyPack(Player player, String packType) {
        PurchaseResult result = storeService.purchaseCardPack(player, packType);
        if (result.isSuccess()) {
            playerRepository.save(player); 
            logger.info("Player {} bought pack of type {}", player.getId(), packType);
        }
        return result;
    }

    /**
     * Concludes a game session, updates player scores, and notifies participants.
     * The active game session is removed, the winner receives upgrade points,
     * and both players are informed of the game's outcome.
     *
     * @param matchId The ID of the match that is finishing.
     * @param winnerId The ID of the player who won the match.
     * @param loserId The ID of the player who lost the match.
     */
    public void finishGame(String matchId, String winnerId, String loserId) {
        if (gameSessionRepository.findById(matchId).isEmpty()) {
            logger.warn("Attempt to finish non-existent match: {}", matchId);
            return;
        }
        gameSessionRepository.deleteById(matchId);

        Optional<Player> winnerOpt = playerRepository.findById(winnerId);
        Optional<Player> loserOpt = playerRepository.findById(loserId);

        if (winnerOpt.isPresent() && loserOpt.isPresent()) {
            Player winner = winnerOpt.get();
            Player loser = loserOpt.get();

            rankingService.updateEloRatings(winner, loser);

            int pointsEarned = 10;
            winner.setUpgradePoints(winner.getUpgradePoints() + pointsEarned);
            playerRepository.update(winner);
            logger.info("Match {} finished. {} won {} points!", matchId, winner.getNickname(), pointsEarned);
        }

        notifyPlayer(winnerId, "UPDATE:GAME_OVER:VICTORY");
        notifyPlayer(loserId, "UPDATE:GAME_OVER:DEFEAT");

        logger.info("Match {} finished. Winner: {}, Loser: {}", matchId, winnerId, loserId);
    }

    /**
     * Executes a trade proposal that has been accepted.
     * This method handles the atomic exchange of cards between two players,
     * ensuring both players have the cards they are offering/requesting.
     * It also involves acquiring and releasing a distributed lock to ensure data consistency.
     *
     * @param tradeId The unique identifier of the trade proposal to execute.
     * @return {@code true} if the trade was executed successfully, {@code false} otherwise.
     */
    public boolean executeTrade(String tradeId) {
        Optional<TradeProposal> proposalOpt = tradeService.findTradeById(tradeId);
        if (proposalOpt.isEmpty()) {
            logger.warn("Attempted to execute non-existent trade: {}", tradeId);
            return false;
        }
        TradeProposal proposal = proposalOpt.get();
        
        // Check if this is an acceptance of an existing proposal
        if (proposal.getStatus() != TradeProposal.Status.ACCEPTED) {
            logger.warn("Trade {} status is not ACCEPTED, cannot execute", tradeId);
            return false;
        }

        String leader = leaderElectionService.getLeader();
        boolean lockAcquired = false;
        try {
            lockAcquired = serverApiClient.acquireLock(leader);
            if (!lockAcquired) {
                logger.warn("Could not acquire lock to execute trade {}", tradeId);
                return false;
            }

            Player p1 = playerRepository.findById(proposal.getProposingPlayerId()).orElse(null);
            Player p2 = playerRepository.findById(proposal.getTargetPlayerId()).orElse(null);

            if (p1 == null || p2 == null) {
                logger.error("Could not find one or both players for trade {}", tradeId);
                return false;
            }

            if (!p1.hasCards(proposal.getOfferedCardIds()) || !p2.hasCards(proposal.getRequestedCardIds())) {
                logger.warn("Trade {} invalid: one or both players missing cards.", tradeId);
                notifyPlayer(p1.getId(), "UPDATE:TRADE_COMPLETE:FAILED_MISSING_CARDS");
                notifyPlayer(p2.getId(), "UPDATE:TRADE_COMPLETE:FAILED_MISSING_CARDS");
                return false;
            }

            List<Card> p1OfferedCards = p1.getCardCollection().stream().filter(c -> proposal.getOfferedCardIds().contains(c.getId())).collect(Collectors.toList());
            List<Card> p2RequestedCards = p2.getCardCollection().stream().filter(c -> proposal.getRequestedCardIds().contains(c.getId())).collect(Collectors.toList());

            p1.getCardCollection().removeAll(p1OfferedCards);
            p1.getCardCollection().addAll(p2RequestedCards);
            
            p2.getCardCollection().removeAll(p2RequestedCards);
            p2.getCardCollection().addAll(p1OfferedCards);

            playerRepository.save(p1);
            playerRepository.save(p2);

            proposal.setStatus(TradeProposal.Status.COMPLETED);
            logger.info("Trade {} executed successfully.", tradeId);
            
            notifyPlayer(p1.getId(), "UPDATE:TRADE_COMPLETE:SUCCESS");
            notifyPlayer(p2.getId(), "UPDATE:TRADE_COMPLETE:SUCCESS");
            
            return true;

        } finally {
            if (lockAcquired) {
                serverApiClient.releaseLock(leader);
            }
        }
    }
    
    /**
     * Handles a trade proposal from one player to another
     */
    private void handleTradeProposal(String proposerId, String targetId, String[] offeredCardIds, String[] requestedCardIds) {
        Player proposer = playerRepository.findById(proposerId).orElse(null);
        Player target = playerRepository.findById(targetId).orElse(null);
        
        if (proposer == null) {
            notifyPlayer(proposerId, "ERROR:Proposer player not found.");
            return;
        }
        
        if (target == null) {
            notifyPlayer(proposerId, "ERROR:Target player not found.");
            return;
        }
        
        // Validate that the proposer has the cards they want to offer
        List<String> offeredCardIdsList = Arrays.asList(offeredCardIds);
        if (!proposer.hasCards(offeredCardIdsList)) {
            notifyPlayer(proposerId, "ERROR:Proposer does not have all the offered cards");
            return;
        }
        
        // Validate that the target has the cards they are requested to give
        List<String> requestedCardIdsList = Arrays.asList(requestedCardIds);
        if (!target.hasCards(requestedCardIdsList)) {
            notifyPlayer(proposerId, "ERROR:Target player does not have all the requested cards");
            return;
        }
        
        // Create the trade proposal
        TradeProposal proposal = new TradeProposal(proposerId, targetId, 
                                                 Arrays.asList(offeredCardIds), 
                                                 Arrays.asList(requestedCardIds));
        tradeService.createTrade(proposal);
        
        // Notify the target player about the trade proposal
        String tradeMessage = String.format("UPDATE:TRADE_PROPOSAL:%s:%s:%s", 
                                           proposal.getTradeId(), proposerId, 
                                           String.join(",", offeredCardIds) + ":" + String.join(",", requestedCardIds));
        notifyPlayer(targetId, tradeMessage);
        
        // Confirm to the proposer that the trade was sent
        notifyPlayer(proposerId, "SUCCESS:Trade proposal sent to " + target.getNickname());
        
        logger.info("Trade proposal created: {} proposes {} for {} with target {}", 
                   proposerId, Arrays.toString(offeredCardIds), 
                   Arrays.toString(requestedCardIds), targetId);
    }
    
    /**
     * Handles trade acceptance by the target player
     */
    private void handleTradeAcceptance(String playerId, String tradeId) {
        Optional<TradeProposal> proposalOpt = tradeService.findTradeById(tradeId);
        if (proposalOpt.isEmpty()) {
            notifyPlayer(playerId, "ERROR:Trade proposal not found.");
            return;
        }
        
        TradeProposal proposal = proposalOpt.get();
        
        // Check if the player is the target of the trade
        if (!proposal.getTargetPlayerId().equals(playerId)) {
            notifyPlayer(playerId, "ERROR:You are not authorized to accept this trade.");
            return;
        }
        
        // Check if the trade is still pending
        if (proposal.getStatus() != TradeProposal.Status.PENDING) {
            notifyPlayer(playerId, "ERROR:This trade is no longer available for acceptance.");
            return;
        }
        
        // Update the status to accepted
        proposal.setStatus(TradeProposal.Status.ACCEPTED);
        
        // Notify the proposer that the trade was accepted
        notifyPlayer(proposal.getProposingPlayerId(), "UPDATE:TRADE_ACCEPTED:" + tradeId);
        
        // Execute the trade atomically
        boolean success = executeTrade(tradeId);
        
        if (!success) {
            package controller;

import api.ServerApiClient;
import api.registry.ServerRegistry;
import model.*;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import pubsub.IEventManager;
import repository.CardRepository;
import repository.GameSessionRepository;
import repository.PlayerRepository;
import service.deck.DeckService;
import service.election.LeaderElectionService;
import service.matchmaking.MatchmakingService;
import service.store.PurchaseResult;
import service.store.StoreService;
import service.trade.TradeService;

import java.util.*;
import java.util.stream.Collectors;

@Profile("server")
@Service
public class GameFacade {
    private final MatchmakingService matchmakingService;
    private final StoreService storeService;
    private final PlayerRepository playerRepository;
    private final repository.JpaPlayerRepository jpaPlayerRepository;
    private final IEventManager eventManager;
    private final ServerRegistry serverRegistry;
    private final ServerApiClient serverApiClient;
    private final TradeService tradeService;
    private final LeaderElectionService leaderElectionService;
    private final CardRepository cardRepository;
    private final DeckService deckService;
    private final GameSessionRepository gameSessionRepository;
    private final RedissonClient redissonClient;

    @Value("${server.name}")
    private String serverName;
    @Value("${server.port}")
    private String serverPort;
    private String selfUrl;

    private static final Logger logger = LoggerFactory.getLogger(GameFacade.class);

    @Autowired
    public GameFacade(MatchmakingService matchmakingService, StoreService storeService,
                      PlayerRepository playerRepository, repository.JpaPlayerRepository jpaPlayerRepository,
                      IEventManager eventManager, ServerRegistry serverRegistry, ServerApiClient serverApiClient,
                      TradeService tradeService, LeaderElectionService leaderElectionService,
                      CardRepository cardRepository, DeckService deckService, GameSessionRepository gameSessionRepository,
                      RedissonClient redissonClient) {
        this.matchmakingService = matchmakingService;
        this.storeService = storeService;
        this.playerRepository = playerRepository;
        this.jpaPlayerRepository = jpaPlayerRepository;
        this.eventManager = eventManager;
        this.serverRegistry = serverRegistry;
        this.serverApiClient = serverApiClient;
        this.tradeService = tradeService;
        this.leaderElectionService = leaderElectionService;
        this.cardRepository = cardRepository;
        this.deckService = deckService;
        this.gameSessionRepository = gameSessionRepository;
        this.redissonClient = redissonClient;
    }

    private String getSelfUrl() {
        if (selfUrl == null) {
            selfUrl = "http://" + serverName + ":" + serverPort;
        }
        return selfUrl;
    }

    public IEventManager getEventManager() {
        return this.eventManager;
    }

    public void registerPlayer(String playerId) {
        logger.info("Player registered in facade: {}", playerId);
    }

    public void unregisterPlayer(String playerId) {
        // This is not efficient. A secondary index should be used to track games per player.
        // For example, a Redis Set for each player containing the matchIds they are in.
        Iterable<String> keys = redissonClient.getKeys().getKeysByPattern("game_session:*");
        for (String key : keys) {
            String matchId = key.substring("game_session:".length());
            Optional<GameSession> sessionOpt = gameSessionRepository.findById(matchId);
            if (sessionOpt.isPresent()) {
                GameSession session = sessionOpt.get();
                if (session.getPlayer1().getId().equals(playerId) || session.getPlayer2().getId().equals(playerId)) {
                    String opponentId = session.getPlayer1().getId().equals(playerId) ?
                            session.getPlayer2().getId() : session.getPlayer1().getId();
                    notifyPlayer(opponentId, "UPDATE:GAME_OVER:OPPONENT_DISCONNECT");
                    gameSessionRepository.deleteById(matchId);
                    logger.info("Game {} removed due to player {} disconnection", matchId, playerId);
                }
            }
        }
        logger.info("Player unregistered and games cleaned up: {}", playerId);
    }

    public void enterMatchmaking(Player player) {
        matchmakingService.addPlayerToQueue(player);
        logger.info("Player {} added to matchmaking queue", player.getId());
        tryToCreateMatch();
    }

    public void enterMatchmaking(Player player, String deckId) {
        matchmakingService.addPlayerToQueueWithDeck(player, deckId);
        logger.info("Player {} added to matchmaking queue with deck {}", player.getId(), deckId);
        tryToCreateMatch();
    }

    public void tryToCreateMatch() {
        Optional<Match> localMatch = matchmakingService.findMatch();
        if (localMatch.isPresent()) {
            startMatch(localMatch.get());
            return;
        }

        Optional<Player> localPlayerOpt = matchmakingService.findAndLockPartner();
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
                        return;
                    }
                } catch (Exception e) {
                    logger.warn("Could not request partner from server {}: {}", serverUrl, e.getMessage());
                }
            }

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

        GameSession session = new GameSession(matchId, p1, p2, deckP1, deckP2, this, cardRepository);
        gameSessionRepository.save(session);

        session.startGame();

        logger.info("New match created between {} and {} with ID {}", p1.getId(), p2.getId(), matchId);
    }

    private void startMatchWithDecks(Match match, String deckId1, String deckId2) {
        Player p1 = match.getPlayer1();
        Player p2 = match.getPlayer2();

        String matchId = UUID.randomUUID().toString();

        List<Card> deckP1 = getDeckCards(p1.getId(), deckId1);
        List<Card> deckP2 = getDeckCards(p2.getId(), deckId2);

        if (deckP1 == null) {
            deckP1 = getDefaultDeckCards(p1.getId());
            if (deckP1 == null) {
                deckP1 = new ArrayList<>(p1.getCardCollection());
            }
        }

        if (deckP2 == null) {
            deckP2 = getDefaultDeckCards(p2.getId());
            if (deckP2 == null) {
                deckP2 = new ArrayList<>(p2.getCardCollection());
            }
        }

        GameSession session = new GameSession(matchId, p1, p2, deckP1, deckP2, this, cardRepository);
        gameSessionRepository.save(session);

        session.startGame();

        logger.info("New match created between {} and {} with ID {} using decks {} and {}",
                p1.getId(), p2.getId(), matchId, deckId1, deckId2);
    }

    private List<Card> getDeckCards(String playerId, String deckId) {
        Optional<model.Deck> deckOpt = deckService.getDeckForPlayer(deckId, playerId);
        if (deckOpt.isPresent()) {
            return deckOpt.get().getCards();
        }
        return null;
    }

    private List<Card> getDefaultDeckCards(String playerId) {
        Optional<model.Deck> deckOpt = deckService.getDefaultDeck(playerId);
        if (deckOpt.isPresent()) {
            return deckOpt.get().getCards();
        }
        return null;
    }

    public void notifyPlayer(String playerId, String message) {
        eventManager.publish(playerId, message);
    }

    public void notifyPlayers(List<String> playerIds, String message) {
        for (String playerId : playerIds) {
            notifyPlayer(playerId, message);
        }
    }

    public void processGameCommand(String[] command) {
        if (command.length < 3) {
            logger.warn("Invalid command structure: {}", String.join(":", command));
            return;
        }
        String playerId = command[1];
        String action = command[2];

        Player player = playerRepository.findById(playerId).orElse(null);
        if (player == null && !"CHARACTER_SETUP".equals(action)) {
            notifyPlayer(playerId, "ERROR:Player not found. Please set up your character first.");
            return;
        }

        switch (action) {
            case "CHARACTER_SETUP":
                if (command.length < 6) {
                    notifyPlayer(playerId, "ERROR:Incomplete character setup command. Expected: NICKNAME:RACE:CLASS");
                    return;
                }
                Player newPlayer = new Player(playerId, command[3]);
                newPlayer.setCharacter(command[4], command[5]);
                playerRepository.save(newPlayer);
                notifyPlayer(playerId, "SUCCESS:Character created.");
                break;

            case "MATCHMAKING":
                if (command.length > 3 && "ENTER".equals(command[3])) {
                    String deckId = null;
                    if (command.length > 4) {
                        deckId = command[4];
                    }

                    if (deckId != null && !deckId.isEmpty()) {
                        if (deckService.isValidDeckForGame(deckId, playerId)) {
                            enterMatchmaking(player, deckId);
                            notifyPlayer(playerId, "SUCCESS:Entered matchmaking queue with deck: " + deckId);
                        } else {
                            notifyPlayer(playerId, "ERROR:Invalid or non-existent deck: " + deckId);
                        }
                    } else {
                        enterMatchmaking(player);
                        notifyPlayer(playerId, "SUCCESS:Entered matchmaking queue with default deck.");
                    }
                }
                break;

            case "PLAY_CARD":
                if (command.length < 5) {
                    notifyPlayer(playerId, "ERROR:Incomplete PLAY_CARD command.");
                    return;
                }
                String matchId = command[3];
                String cardId = command[4];
                Optional<GameSession> sessionOpt = gameSessionRepository.findById(matchId);
                if (sessionOpt.isPresent()) {
                    GameSession session = sessionOpt.get();
                    session.playCard(playerId, cardId);
                    gameSessionRepository.save(session);
                } else {
                    notifyPlayer(playerId, "ERROR:Match not found for PLAY_CARD command.");
                }
                break;

            case "STORE":
                if (command.length > 4 && "BUY".equals(command[3])) {
                    String packType = command[4];
                    PurchaseResult result = buyPack(player, packType);
                    if (result.isSuccess()) {
                        notifyPlayer(playerId, "SUCCESS:Pack purchased. You got " + result.getCards().size() + " cards.");
                    } else {
                        notifyPlayer(playerId, "ERROR:Purchase failed: " + result.getStatus());
                    }
                }
                break;

            case "TRADE":
                if (command.length < 4) {
                    notifyPlayer(playerId, "ERROR:Incomplete TRADE command.");
                    return;
                }

                String tradeAction = command[3];
                if ("PROPOSE".equals(tradeAction) && command.length >= 7) {
                    String targetPlayerId = command[4];
                    String[] offeredCards = command[5].split(",");
                    String[] requestedCards = command[6].split(",");

                    handleTradeProposal(playerId, targetPlayerId, offeredCards, requestedCards);
                } else if ("ACCEPT".equals(tradeAction) && command.length >= 5) {
                    String tradeId = command[4];
                    handleTradeAcceptance(playerId, tradeId);
                } else if ("REJECT".equals(tradeAction) && command.length >= 5) {
                    String tradeId = command[4];
                    handleTradeRejection(playerId, tradeId);
                } else {
                    notifyPlayer(playerId, "ERROR:Invalid trade command format or action.");
                }
                break;

            case "PRIVATE_MESSAGE":
                if (command.length < 4) {
                    notifyPlayer(playerId, "ERROR:Incomplete PRIVATE_MESSAGE command.");
                    return;
                }

                String recipientId = command[3];
                String messageContent = String.join(":", java.util.Arrays.copyOfRange(command, 4, command.length));

                handlePrivateMessage(playerId, recipientId, messageContent);
                break;

            default:
                notifyPlayer(playerId, "ERROR:Unknown command '" + action + "'.");
                break;
        }
    }

    private void handlePrivateMessage(String senderId, String recipientId, String content) {
        if (eventManager instanceof pubsub.RedisEventManager) {
            pubsub.RedisEventManager redisEventManager = (pubsub.RedisEventManager) eventManager;
            redisEventManager.sendPrivateMessage(senderId, recipientId, content);
            notifyPlayer(senderId, "PRIVATE_MESSAGE_SENT:" + recipientId + ":" + content);
        } else {
            String channel = "private-messages:" + recipientId;
            String message = "PRIVATE:" + senderId + ":" + content;
            eventManager.publish(channel, message);
            notifyPlayer(senderId, "PRIVATE_MESSAGE_SENT:" + recipientId + ":" + content);
        }
    }

    public Player findPlayerById(String playerId) {
        return playerRepository.findById(playerId).orElse(null);
    }

    public PurchaseResult buyPack(Player player, String packType) {
        PurchaseResult result = storeService.purchaseCardPack(player, packType);
        if (result.isSuccess()) {
            playerRepository.save(player);
            logger.info("Player {} bought pack of type {}", player.getId(), packType);
        }
        return result;
    }

    public void finishGame(String matchId, String winnerId, String loserId) {
        if (gameSessionRepository.findById(matchId).isEmpty()) {
            logger.warn("Attempt to finish non-existent match: {}", matchId);
            return;
        }
        gameSessionRepository.deleteById(matchId);

        Optional<Player> winnerOpt = playerRepository.findById(winnerId);
        if (winnerOpt.isPresent()) {
            Player winner = winnerOpt.get();
            int pointsEarned = 10;
            winner.setUpgradePoints(winner.getUpgradePoints() + pointsEarned);
            playerRepository.update(winner);
            logger.info("Match {} finished. {} won {} points!", matchId, winner.getNickname(), pointsEarned);
        }

        notifyPlayer(winnerId, "UPDATE:GAME_OVER:VICTORY");
        notifyPlayer(loserId, "UPDATE:GAME_OVER:DEFEAT");

        logger.info("Match {} finished. Winner: {}, Loser: {}", matchId, winnerId, loserId);
    }

    public boolean executeTrade(String tradeId) {
        Optional<TradeProposal> proposalOpt = tradeService.findTradeById(tradeId);
        if (proposalOpt.isEmpty()) {
            logger.warn("Attempted to execute non-existent trade: {}", tradeId);
            return false;
        }
        TradeProposal proposal = proposalOpt.get();

        if (proposal.getStatus() != TradeProposal.Status.ACCEPTED) {
            logger.warn("Trade {} status is not ACCEPTED, cannot execute", tradeId);
            return false;
        }

        String leader = leaderElectionService.getLeader();
        boolean lockAcquired = false;
        try {
            lockAcquired = serverApiClient.acquireLock(leader);
            if (!lockAcquired) {
                logger.warn("Could not acquire lock to execute trade {}", tradeId);
                return false;
            }

            Player p1 = playerRepository.findById(proposal.getProposingPlayerId()).orElse(null);
            Player p2 = playerRepository.findById(proposal.getTargetPlayerId()).orElse(null);

            if (p1 == null || p2 == null) {
                logger.error("Could not find one or both players for trade {}", tradeId);
                return false;
            }

            if (!p1.hasCards(proposal.getOfferedCardIds()) || !p2.hasCards(proposal.getRequestedCardIds())) {
                logger.warn("Trade {} invalid: one or both players missing cards.", tradeId);
                notifyPlayer(p1.getId(), "UPDATE:TRADE_COMPLETE:FAILED_MISSING_CARDS");
                notifyPlayer(p2.getId(), "UPDATE:TRADE_COMPLETE:FAILED_MISSING_CARDS");
                return false;
            }

            List<Card> p1OfferedCards = p1.getCardCollection().stream().filter(c -> proposal.getOfferedCardIds().contains(c.getId())).collect(Collectors.toList());
            List<Card> p2RequestedCards = p2.getCardCollection().stream().filter(c -> proposal.getRequestedCardIds().contains(c.getId())).collect(Collectors.toList());

            p1.getCardCollection().removeAll(p1OfferedCards);
            p1.getCardCollection().addAll(p2RequestedCards);

            p2.getCardCollection().removeAll(p2RequestedCards);
            p2.getCardCollection().addAll(p1OfferedCards);

            playerRepository.save(p1);
            playerRepository.save(p2);

            proposal.setStatus(TradeProposal.Status.COMPLETED);
            logger.info("Trade {} executed successfully.", tradeId);

            notifyPlayer(p1.getId(), "UPDATE:TRADE_COMPLETE:SUCCESS");
            notifyPlayer(p2.getId(), "UPDATE:TRADE_COMPLETE:SUCCESS");

            return true;

        } finally {
            if (lockAcquired) {
                serverApiClient.releaseLock(leader);
            }
        }
    }

    private void handleTradeProposal(String proposerId, String targetId, String[] offeredCardIds, String[] requestedCardIds) {
        Player proposer = playerRepository.findById(proposerId).orElse(null);
        Player target = playerRepository.findById(targetId).orElse(null);

        if (proposer == null) {
            notifyPlayer(proposerId, "ERROR:Proposer player not found.");
            return;
        }

        if (target == null) {
            notifyPlayer(proposerId, "ERROR:Target player not found.");
            return;
        }

        List<String> offeredCardIdsList = Arrays.asList(offeredCardIds);
        if (!proposer.hasCards(offeredCardIdsList)) {
            notifyPlayer(proposerId, "ERROR:Proposer does not have all the offered cards");
            return;
        }

        List<String> requestedCardIdsList = Arrays.asList(requestedCardIds);
        if (!target.hasCards(requestedCardIdsList)) {
            notifyPlayer(proposerId, "ERROR:Target player does not have all the requested cards");
            return;
        }

        TradeProposal proposal = new TradeProposal(proposerId, targetId,
                Arrays.asList(offeredCardIds),
                Arrays.asList(requestedCardIds));
        tradeService.createTrade(proposal);

        String tradeMessage = String.format("UPDATE:TRADE_PROPOSAL:%s:%s:%s",
                proposal.getTradeId(), proposerId,
                String.join(",", offeredCardIds) + ":" + String.join(",", requestedCardIds));
        notifyPlayer(targetId, tradeMessage);

        notifyPlayer(proposerId, "SUCCESS:Trade proposal sent to " + target.getNickname());

        logger.info("Trade proposal created: {} proposes {} for {} with target {}",
                proposerId, Arrays.toString(offeredCardIds),
                Arrays.toString(requestedCardIds), targetId);
    }

    private void handleTradeAcceptance(String playerId, String tradeId) {
        Optional<TradeProposal> proposalOpt = tradeService.findTradeById(tradeId);
        if (proposalOpt.isEmpty()) {
            notifyPlayer(playerId, "ERROR:Trade proposal not found.");
            return;
        }

        TradeProposal proposal = proposalOpt.get();

        if (!proposal.getTargetPlayerId().equals(playerId)) {
            notifyPlayer(playerId, "ERROR:You are not authorized to accept this trade.");
            return;
        }

        if (proposal.getStatus() != TradeProposal.Status.PENDING) {
            notifyPlayer(playerId, "ERROR:This trade is no longer available for acceptance.");
            return;
        }

        proposal.setStatus(TradeProposal.Status.ACCEPTED);

        notifyPlayer(proposal.getProposingPlayerId(), "UPDATE:TRADE_ACCEPTED:" + tradeId);

        boolean success = executeTrade(tradeId);

        if (!success) {
            proposal.setStatus(TradeProposal.Status.PENDING);
            notifyPlayer(playerId, "ERROR:Trade execution failed.");
            notifyPlayer(proposal.getProposingPlayerId(), "ERROR:Trade execution failed.");
        }
    }

    private void handleTradeRejection(String playerId, String tradeId) {
        Optional<TradeProposal> proposalOpt = tradeService.findTradeById(tradeId);
        if (proposalOpt.isEmpty()) {
            notifyPlayer(playerId, "ERROR:Trade proposal not found.");
            return;
        }

        TradeProposal proposal = proposalOpt.get();

        if (!proposal.getTargetPlayerId().equals(playerId) && !proposal.getProposingPlayerId().equals(playerId)) {
            notifyPlayer(playerId, "ERROR:You are not authorized to reject this trade.");
            return;
        }

        if (proposal.getStatus() != TradeProposal.Status.PENDING) {
            notifyPlayer(playerId, "ERROR:This trade is no longer available for rejection.");
            return;
        }

        proposal.setStatus(TradeProposal.Status.REJECTED);

        notifyPlayer(proposal.getTargetPlayerId(), "UPDATE:TRADE_REJECTED:" + tradeId);
        notifyPlayer(proposal.getProposingPlayerId(), "UPDATE:TRADE_REJECTED_BY_TARGET:" + tradeId);

        logger.info("Trade {} rejected by player {}", tradeId, playerId);
    }

    @Scheduled(fixedRate = 2000)
    public void scheduledMatchmaking() {
        tryToCreateMatch();
    }

    @Scheduled(cron = "0 0 0 * * ?")
    public void awardDailyRewards() {
        logger.info("Starting daily reward distribution task");

        try {
            List<Player> allPlayers = jpaPlayerRepository.findAll();

            int dailyReward = 50;
            for (Player player : allPlayers) {
                player.setCoins(player.getCoins() + dailyReward);
                playerRepository.save(player);
                notifyPlayer(player.getId(), "DAILY_REWARD:" + dailyReward + ":Thank you for playing! Daily reward awarded.");
                logger.debug("Daily reward of {} coins awarded to player {}", dailyReward, player.getId());
            }

            logger.info("Completed daily reward distribution for {} players", allPlayers.size());
        } catch (Exception e) {
            logger.error("Error during daily reward distribution: {}", e.getMessage(), e);
        }
    }

    @Scheduled(fixedRate = 2000)
    public void scheduledResourceRegeneration() {
        // This is not efficient. A better approach would be to have a separate service
        // that subscribes to game start events and manages the game loop.
        Iterable<String> keys = redissonClient.getKeys().getKeysByPattern("game_session:*");
        for (String key : keys) {
            String matchId = key.substring("game_session:".length());
            Optional<GameSession> sessionOpt = gameSessionRepository.findById(matchId);
            if (sessionOpt.isPresent()) {
                GameSession session = sessionOpt.get();
                // session.regenerateResources(); // This method was removed
                gameSessionRepository.save(session);
            }
        }
    }

    @Scheduled(fixedRate = 1000)
    public void scheduledTurnTimerCheck() {
        // This is not efficient.
        Iterable<String> keys = redissonClient.getKeys().getKeysByPattern("game_session:*");
        for (String key : keys) {
            String matchId = key.substring("game_session:".length());
            Optional<GameSession> sessionOpt = gameSessionRepository.findById(matchId);
            if (sessionOpt.isPresent()) {
                GameSession session = sessionOpt.get();
                session.forceEndTurn();
                gameSessionRepository.save(session);
            }
        }
    }
}
        }
    }
    
    /**
     * Handles trade rejection by the target player
     */
    private void handleTradeRejection(String playerId, String tradeId) {
        Optional<TradeProposal> proposalOpt = tradeService.findTradeById(tradeId);
        if (proposalOpt.isEmpty()) {
            notifyPlayer(playerId, "ERROR:Trade proposal not found.");
            return;
        }
        
        TradeProposal proposal = proposalOpt.get();
        
        // Check if the player is the target of the trade
        if (!proposal.getTargetPlayerId().equals(playerId) && !proposal.getProposingPlayerId().equals(playerId)) {
            notifyPlayer(playerId, "ERROR:You are not authorized to reject this trade.");
            return;
        }
        
        // Check if the trade is still pending
        if (proposal.getStatus() != TradeProposal.Status.PENDING) {
            notifyPlayer(playerId, "ERROR:This trade is no longer available for rejection.");
            return;
        }
        
        // Update the status to rejected
        proposal.setStatus(TradeProposal.Status.REJECTED);
        
        // Notify both players that the trade was rejected
        notifyPlayer(proposal.getTargetPlayerId(), "UPDATE:TRADE_REJECTED:" + tradeId);
        notifyPlayer(proposal.getProposingPlayerId(), "UPDATE:TRADE_REJECTED_BY_TARGET:" + tradeId);
        
        logger.info("Trade {} rejected by player {}", tradeId, playerId);
    }
    
    /**
     * Periodically attempts to create matches from the matchmaking queue.
     * This method is scheduled to run at a fixed rate (e.g., every 2 seconds)
     * to continuously process pending matchmaking requests.
     */
    @Scheduled(fixedRate = 2000) // Try to create matches every 2 seconds
    public void scheduledMatchmaking() {
        tryToCreateMatch(); // Attempt to create matches periodically
    }
    
    /**
     * Daily reward task that runs once per day to award active players
     */
    /**
     * Awards daily rewards to all active players.
     * This method is scheduled to run once every day at midnight.
     * It iterates through all registered players, grants them a daily coin reward,
     * and notifies them about the reward.
     */
    @Scheduled(cron = "0 0 0 * * ?") // Run at midnight every day
    public void awardDailyRewards() {
        logger.info("Starting daily reward distribution task");
        
        try {
            // Get all existing players
            List<Player> allPlayers = jpaPlayerRepository.findAll();
            
            int dailyReward = 50; // Number of coins to award daily
            for (Player player : allPlayers) {
                // Add daily reward to player
                player.setCoins(player.getCoins() + dailyReward);
                
                // Save the updated player
                playerRepository.save(player);
                
                // Notify player about the reward
                notifyPlayer(player.getId(), "DAILY_REWARD:" + dailyReward + ":Thank you for playing! Daily reward awarded.");
                
                logger.debug("Daily reward of {} coins awarded to player {}", dailyReward, player.getId());
            }
            
            logger.info("Completed daily reward distribution for {} players", allPlayers.size());
        } catch (Exception e) {
            logger.error("Error during daily reward distribution: {}", e.getMessage(), e);
        }
    }

    /**
     * Periodically regenerates resources for all active games.
     * This method is scheduled to run at a fixed rate (e.g., every 2 seconds)
     * to increment the players' resource pools.
     */
    @Scheduled(fixedRate = 2000) // Regenerate resources every 2 seconds
    public void scheduledResourceRegeneration() {
        for (GameSession session : activeGames.values()) {
            session.regenerateResources();
        }
    }

    /**
     * Periodically checks for expired turn timers in active games.
     * If a timer has expired, it forces the current player to take a default action.
     */
    @Scheduled(fixedRate = 1000) // Check every second
    public void scheduledTurnTimerCheck() {
        for (GameSession session : activeGames.values()) {
            session.forceEndTurn();
        }
    }
}
