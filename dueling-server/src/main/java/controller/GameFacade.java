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
import service.chat.ChatGroupService;
import service.deck.DeckService;
import service.election.LeaderElectionService;
import service.emote.EmoteService;
import service.ingamechat.InGameChatService;
import service.matchmaking.MatchmakingService;
import service.store.PurchaseResult;
import service.store.StoreService;
import service.trade.TradeService;

import service.ranking.RankingService;
import service.achievement.AchievementService;
import websocket.WebSocketSessionManager;

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
    private final InGameChatService inGameChatService;
    private final EmoteService emoteService;
    private final RankingService rankingService;
    private final AchievementService achievementService;
    private final WebSocketSessionManager sessionManager;

    @Value("${server.name}")
    private String serverName;
    @Value("${server.port}")
    private String serverPort;
    private String selfUrl;

    private static final Logger logger = LoggerFactory.getLogger(GameFacade.class);

    private final ChatGroupService chatGroupService;
    private final service.lock.LockService lockService;

    @Autowired
    public GameFacade(MatchmakingService matchmakingService, StoreService storeService,
                      PlayerRepository playerRepository, repository.JpaPlayerRepository jpaPlayerRepository,
                      IEventManager eventManager, ServerRegistry serverRegistry, ServerApiClient serverApiClient,
                      TradeService tradeService, LeaderElectionService leaderElectionService,
                      CardRepository cardRepository, DeckService deckService, GameSessionRepository gameSessionRepository,
                      RedissonClient redissonClient, RankingService rankingService, AchievementService achievementService,
                      ChatGroupService chatGroupService, InGameChatService inGameChatService, EmoteService emoteService,
                      service.lock.LockService lockService, WebSocketSessionManager sessionManager) {
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
        this.inGameChatService = inGameChatService;
        this.chatGroupService = chatGroupService;
        this.emoteService = emoteService;
        this.rankingService = rankingService;
        this.achievementService = achievementService;
        this.lockService = lockService;
        this.sessionManager = sessionManager;
    }

    private String getSelfUrl() {
        if (selfUrl == null) {
            // Use localhost for local-distributed mode
            String hostname = serverName.contains("server-") ? "localhost" : serverName;
            selfUrl = "http://" + hostname + ":" + serverPort;
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
        Iterable<String> keys = redissonClient.getKeys().getKeysByPattern("game_session:*");
        for (String key : keys) {
            String matchId = key.substring("game_session:".length());
            Optional<GameSession> sessionOpt = gameSessionRepository.findById(matchId);
            if (sessionOpt.isPresent()) {
                GameSession session = sessionOpt.get();
                if (session.getPlayer1().getId().equals(playerId) || session.getPlayer2().getId().equals(playerId)) {
                    String opponentId = session.getPlayer1().getId().equals(playerId) ?
                            session.getPlayer2().getId() : session.getPlayer1().getId();
                    
                    // Update opponent's match status
                    sessionManager.setPlayerInMatch(opponentId, false);
                    
                    notifyPlayer(opponentId, "UPDATE:GAME_OVER:OPPONENT_DISCONNECT");
                    gameSessionRepository.deleteById(matchId);
                    logger.info("Game {} removed due to player {} disconnection", matchId, playerId);
                }
            }
        }
        logger.info("Player unregistered and games cleaned up: {}", playerId);
    }

    public void enterMatchmaking(Player player) {
        logger.info("[MATCHMAKING] Player {} entering matchmaking", player.getId());
        
        // Check if player is already in a match
        boolean inMatch = sessionManager.isPlayerInMatch(player.getId());
        logger.info("[MATCHMAKING] Player {} in match? {}", player.getId(), inMatch);
        if (inMatch) {
            logger.warn("[MATCHMAKING] Player {} tried to enter matchmaking while in a match", player.getId());
            notifyPlayer(player.getId(), "ERROR:You are already in a match. Please finish your current match first.");
            return;
        }
        
        // Check if player is already in the matchmaking queue
        boolean inQueue = matchmakingService.isPlayerInQueue(player);
        logger.info("[MATCHMAKING] Player {} in queue? {}", player.getId(), inQueue);
        if (inQueue) {
            logger.warn("[MATCHMAKING] Player {} tried to enter matchmaking while already in queue", player.getId());
            notifyPlayer(player.getId(), "ERROR:You are already in the matchmaking queue.");
            return;
        }
        
        matchmakingService.addPlayerToQueue(player);
        logger.info("[MATCHMAKING] Player {} added to matchmaking queue - triggering match creation", player.getId());
        tryToCreateMatch();
    }

    public void enterMatchmaking(Player player, String deckId) {
        // Check if player is already in a match
        if (sessionManager.isPlayerInMatch(player.getId())) {
            logger.warn("Player {} tried to enter matchmaking while in a match", player.getId());
            notifyPlayer(player.getId(), "ERROR:You are already in a match. Please finish your current match first.");
            return;
        }
        
        // Check if player is already in the matchmaking queue
        if (matchmakingService.isPlayerInQueue(player)) {
            logger.warn("Player {} tried to enter matchmaking while already in queue", player.getId());
            notifyPlayer(player.getId(), "ERROR:You are already in the matchmaking queue.");
            return;
        }
        
        matchmakingService.addPlayerToQueueWithDeck(player, deckId);
        logger.info("Player {} added to matchmaking queue with deck {}", player.getId(), deckId);
        tryToCreateMatch();
    }

    public void tryToCreateMatch() {
        // First, try to find any immediate local matches
        Optional<Match> localMatch = matchmakingService.findMatch();
        if (localMatch.isPresent()) {
            logger.info("[MATCH] Local match found, starting match");
            startMatch(localMatch.get());
            return;
        }

        // Try two-way matching: local player with remote partner AND remote player with local partner
        List<String> remoteServers = new ArrayList<>(serverRegistry.getRegisteredServers());
        String selfUrl = getSelfUrl();
        remoteServers.remove(selfUrl);
        logger.info("[MATCH] Attempting cross-server matching. Self: {}, Remote servers: {}", selfUrl, remoteServers);
        
        // Strategy 1: Try to find a local player and match with a remote partner
        Optional<Player> localPlayerOpt = matchmakingService.findAndLockPartner();
        if (localPlayerOpt.isPresent()) {
            Player p1 = localPlayerOpt.get();
            logger.info("[MATCH] Strategy 1: Found local player {}, searching remote partner on {} servers", 
                p1.getNickname(), remoteServers.size());
            boolean matchFound = false;
            
            for (String serverUrl : remoteServers) {
                try {
                    logger.info("[MATCH] Requesting partner from remote server: {}", serverUrl);
                    Player p2 = serverApiClient.findAndLockPartner(serverUrl);
                    if (p2 != null) {
                        logger.info("[MATCH] ✓ Found remote partner {} for {} on server {}", 
                            p2.getNickname(), p1.getNickname(), serverUrl);
                        startMatch(new Match(p1, p2));
                        matchFound = true;
                        break;
                    } else {
                        logger.info("[MATCH] No partner available on server {}", serverUrl);
                    }
                } catch (Exception e) {
                    logger.warn("[MATCH] Could not request partner from server {}: {}", serverUrl, e.getMessage());
                }
            }

            // If no remote partner found, put local player back in queue with cooldown
            if (!matchFound) {
                logger.info("[MATCH] No remote partner found for {}. Returning to queue with cooldown.", p1.getNickname());
                matchmakingService.returnPlayerToQueue(p1);
            }
        } else {
            logger.debug("[MATCH] Strategy 1: No local player available to match");
        }
        
        // Strategy 2: Try to get a remote player and match with a local partner
        for (String serverUrl : remoteServers) {
            try {
                Player remotePlayer = serverApiClient.findAndLockPartner(getSelfUrl());
                if (remotePlayer != null) {
                    logger.info("Received remote player {} from server {} for local matching", remotePlayer.getNickname(), serverUrl);
                    
                    // Try to find a local partner for the remote player
                    Optional<Player> localPlayerOpt2 = matchmakingService.findAndLockPartner();
                    if (localPlayerOpt2.isPresent()) {
                        Player localPlayer = localPlayerOpt2.get();
                        logger.info("Matching remote player {} with local player {}", remotePlayer.getNickname(), localPlayer.getNickname());
                        startMatch(new Match(remotePlayer, localPlayer));
                        return; // Exit early if we found a match
                    } else {
                        // If no local partner available, put the remote player in our queue
                        logger.info("No local partner for remote player {}, adding to local queue", remotePlayer.getNickname());
                        matchmakingService.addPlayerToQueue(remotePlayer);
                    }
                }
            } catch (Exception e) {
                logger.warn("Could not receive partner from server {}: {}", serverUrl, e.getMessage());
            }
        }
        
        // Improve coordination between servers by triggering their matching as well
        // This helps when both players connect to different servers simultaneously
        for (String serverUrl : remoteServers) {
            // Trigger the remote server to try matchmaking too
            try {
                // Since we can't directly trigger their scheduling, we'll just ensure
                // both servers are actively trying to match players
                // This is achieved through the regular scheduled calls
            } catch (Exception e) {
                logger.debug("Could not coordinate with remote server {}: {}", serverUrl, e.getMessage());
            }
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

        // Update player match status
        sessionManager.setPlayerInMatch(p1.getId(), true);
        sessionManager.setPlayerInMatch(p2.getId(), true);

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

        // Update player match status
        sessionManager.setPlayerInMatch(p1.getId(), true);
        sessionManager.setPlayerInMatch(p2.getId(), true);

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
        logger.debug("notifyPlayer: Attempting to send message to playerId {}: {}", playerId, message);
        eventManager.publish(playerId, message);
        logger.debug("notifyPlayer: Message published for playerId {}", playerId);
    }

    public void notifyPlayers(List<String> playerIds, String message) {
        for (String playerId : playerIds) {
            notifyPlayer(playerId, message);
        }
    }

    public void processGameCommand(String[] command) {
        logger.debug("processGameCommand called with command: [{}]", String.join(":", command));
        if (command.length < 3) {
            logger.warn("Invalid command structure: {}", String.join(":", command));
            return;
        }
        String playerId = command[1];
        String action = command[2];
        logger.debug("Processing action '{}' for player {}", action, playerId);

        Player player = playerRepository.findById(playerId).orElse(null);
        if (player == null && !"CHARACTER_SETUP".equals(action)) {
            notifyPlayer(playerId, "ERROR:Player not found. Please set up your character first.");
            return;
        }

        switch (action) {
            case "CHARACTER_SETUP":
                // Check if player already has a character
                if (player != null) {
                    notifyPlayer(playerId, "ERROR:Character already exists for this player. Character creation is only allowed once.");
                    return;
                }
                logger.debug("Processing CHARACTER_SETUP for player {}: nickname={}, race={}, class={}", playerId, command[3], command[4], command[5]);
                if (command.length < 6) {
                    logger.warn("Incomplete CHARACTER_SETUP command for player {}: {}", playerId, String.join(":", command));
                    notifyPlayer(playerId, "ERROR:Incomplete character setup command. Expected: NICKNAME:RACE:CLASS");
                    return;
                }
                Player newPlayer = new Player(playerId, command[3]);
                newPlayer.setCharacter(command[4], command[5]);
                playerRepository.save(newPlayer);
                // Refresh the player object for use in subsequent operations
                player = newPlayer;
                logger.debug("Character created for player {}, sending response: SUCCESS:Character created.", playerId);
                notifyPlayer(playerId, "SUCCESS:Character created.");
                logger.debug("Response sent to player {}", playerId);
                break;

            case "MATCHMAKING":
                String matchmakingAction = command.length > 3 ? command[3].trim() : "";
                logger.info("[MATCHMAKING] Action: '{}', command length: {}", matchmakingAction, command.length);
                if (command.length > 3 && "ENTER".equals(matchmakingAction)) {
                    // Check if player is already in a match
                    if (sessionManager.isPlayerInMatch(playerId)) {
                        notifyPlayer(playerId, "ERROR:You are already in a match. Please finish your current match first.");
                        break;
                    }
                    
                    // Check if player is already in the matchmaking queue
                    if (matchmakingService.isPlayerInQueue(player)) {
                        notifyPlayer(playerId, "ERROR:You are already in the matchmaking queue.");
                        break;
                    }
                    
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
                logger.debug("STORE command received. Command length: {}, Full command: [{}]", command.length, String.join(":", command));
                if (command.length > 4 && "BUY".equals(command[3])) {
                    String packType = command[4];
                    logger.debug("Processing BUY request for pack type: {} by player: {}", packType, playerId);
                    PurchaseResult result = buyPack(player, packType);
                    if (result.isSuccess()) {
                        List<Card> cards = result.getCards();
                        logger.info("Purchase successful for player {}: received {} cards from {} pack", playerId, cards.size(), packType);
                        StringBuilder cardList = new StringBuilder("SUCCESS:Pack purchased. Cards received: ");
                        for (int i = 0; i < cards.size(); i++) {
                            Card card = cards.get(i);
                            // Use underscores instead of colons to avoid parsing conflicts
                            cardList.append(card.getName()).append("(ID_").append(card.getId()).append(")");
                            if (i < cards.size() - 1) {
                                cardList.append(", ");
                            }
                        }
                        notifyPlayer(playerId, cardList.toString());
                    } else {
                        logger.warn("Purchase failed for player {}: {}", playerId, result.getStatus());
                        notifyPlayer(playerId, "ERROR:Purchase failed: " + result.getStatus());
                    }
                } else {
                    logger.warn("Invalid STORE command format. Expected: STORE:BUY:PACKTYPE, received: [{}]", String.join(":", command));
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
                    String[] offeredCards = command[5].trim().split(",");
                    String[] requestedCards = command[6].trim().split(",");

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

            case "IN_GAME_CHAT":
                if (command.length < 4) {
                    notifyPlayer(playerId, "ERROR:Incomplete IN_GAME_CHAT command.");
                    return;
                }
                String matchIdChat = command[2];
                String chatMessage = String.join(":", java.util.Arrays.copyOfRange(command, 3, command.length));
                inGameChatService.handleInGameChatMessage(playerId, matchIdChat, chatMessage);
                break;

            case "SEND_EMOTE":
                if (command.length < 5) {
                    notifyPlayer(playerId, "ERROR:Incomplete SEND_EMOTE command.");
                    return;
                }
                String channelType = command[2];
                String channelId = command[3];
                String emoteId = command[4];
                emoteService.handleSendEmote(playerId, channelType, channelId, emoteId);
                break;

            case "SHOW_CARDS":
                // Send the player's card collection
                List<Card> playerCards = player.getCardCollection();
                if (playerCards.isEmpty()) {
                    notifyPlayer(playerId, "INFO:Your card collection is empty. Buy a card pack first!");
                } else {
                    StringBuilder cardList = new StringBuilder("INFO:YOUR_CARDS:");
                    for (int i = 0; i < playerCards.size(); i++) {
                        Card card = playerCards.get(i);
                        cardList.append(card.getId()).append("(").append(card.getName()).append(")");
                        if (i < playerCards.size() - 1) {
                            cardList.append(";");
                        }
                    }
                    notifyPlayer(playerId, cardList.toString());
                }
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
        logger.debug("buyPack called for player {} with packType: {}", player.getId(), packType);
        PurchaseResult result = storeService.purchaseCardPack(player, packType);
        if (result.isSuccess()) {
            playerRepository.save(player);
            logger.info("Player {} bought pack of type {}", player.getId(), packType);
        } else {
            logger.warn("Failed to buy pack for player {}: {}", player.getId(), result.getStatus());
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

        // Update player match status
        sessionManager.setPlayerInMatch(winnerId, false);
        sessionManager.setPlayerInMatch(loserId, false);

        notifyPlayer(winnerId, "UPDATE:GAME_OVER:VICTORY");
        notifyPlayer(loserId, "UPDATE:GAME_OVER:DEFEAT");

        logger.info("Match {} finished. Winner: {}, Loser: {}", matchId, winnerId, loserId);
    }

    public boolean executeTrade(String tradeId) {
        logger.info("[TRADE-EXEC] === Starting trade execution ===");
        logger.info("[TRADE-EXEC] Trade ID: {}", tradeId);
        
        Optional<TradeProposal> proposalOpt = tradeService.findTradeById(tradeId);
        if (proposalOpt.isEmpty()) {
            logger.warn("[TRADE-EXEC] Attempted to execute non-existent trade: {}", tradeId);
            return false;
        }
        TradeProposal proposal = proposalOpt.get();
        logger.info("[TRADE-EXEC] Trade found - Proposer: {}, Target: {}, Status: {}", 
                proposal.getProposingPlayerId(), proposal.getTargetPlayerId(), proposal.getStatus());

        if (proposal.getStatus() != TradeProposal.Status.ACCEPTED) {
            logger.warn("[TRADE-EXEC] Trade {} status is not ACCEPTED, cannot execute. Current status: {}", 
                    tradeId, proposal.getStatus());
            return false;
        }

        boolean lockAcquired = false;
        try {
            logger.info("[TRADE-EXEC] Attempting to acquire distributed lock for trade {}", tradeId);
            lockAcquired = lockService.acquire();
            if (!lockAcquired) {
                logger.warn("[TRADE-EXEC] Could not acquire lock to execute trade {}", tradeId);
                return false;
            }
            logger.info("[TRADE-EXEC] Lock acquired for trade {}", tradeId);

            // Re-fetch proposal inside the lock to ensure we have the latest state
            Optional<TradeProposal> lockedProposalOpt = tradeService.findTradeById(tradeId);
            if (lockedProposalOpt.isEmpty() || lockedProposalOpt.get().getStatus() != TradeProposal.Status.ACCEPTED) {
                logger.warn("[TRADE-EXEC] Trade {} is no longer valid for execution after acquiring lock.", tradeId);
                return false;
            }
            TradeProposal lockedProposal = lockedProposalOpt.get();

            logger.info("[TRADE-EXEC] Fetching players from repository");
            Player p1 = playerRepository.findById(lockedProposal.getProposingPlayerId()).orElse(null);
            Player p2 = playerRepository.findById(lockedProposal.getTargetPlayerId()).orElse(null);

            if (p1 == null || p2 == null) {
                logger.error("[TRADE-EXEC] Could not find one or both players for trade {}. P1: {}, P2: {}", 
                        tradeId, p1 != null ? "found" : "null", p2 != null ? "found" : "null");
                return false;
            }
            logger.info("[TRADE-EXEC] Both players found - P1: {} ({}), P2: {} ({})", 
                    p1.getId(), p1.getNickname(), p2.getId(), p2.getNickname());

            // Synchronize on players to prevent concurrent modifications to their collections
            logger.info("[TRADE-EXEC] Acquiring synchronized locks on both players");
            synchronized (p1) {
                synchronized (p2) {
                    logger.info("[TRADE-EXEC] Verifying card ownership");
                    logger.info("[TRADE-EXEC] P1 offered cards: {}", lockedProposal.getOfferedCardIds());
                    logger.info("[TRADE-EXEC] P2 requested cards: {}", lockedProposal.getRequestedCardIds());
                    logger.info("[TRADE-EXEC] P1 has {} cards in collection", p1.getCardCollection().size());
                    logger.info("[TRADE-EXEC] P2 has {} cards in collection", p2.getCardCollection().size());
                    
                    if (!p1.hasCards(lockedProposal.getOfferedCardIds()) || !p2.hasCards(lockedProposal.getRequestedCardIds())) {
                        logger.warn("[TRADE-EXEC] Trade {} invalid: one or both players missing cards.", tradeId);
                        logger.warn("[TRADE-EXEC] P1 has offered cards: {}", p1.hasCards(lockedProposal.getOfferedCardIds()));
                        logger.warn("[TRADE-EXEC] P2 has requested cards: {}", p2.hasCards(lockedProposal.getRequestedCardIds()));
                        notifyPlayer(p1.getId(), "UPDATE:TRADE_COMPLETE:FAILED_MISSING_CARDS");
                        notifyPlayer(p2.getId(), "UPDATE:TRADE_COMPLETE:FAILED_MISSING_CARDS");
                        return false;
                    }
                    logger.info("[TRADE-EXEC] Both players have required cards");

                    logger.info("[TRADE-EXEC] Filtering cards to exchange");
                    List<Card> p1OfferedCards = p1.getCardCollection().stream().filter(c -> lockedProposal.getOfferedCardIds().contains(c.getId())).collect(Collectors.toList());
                    List<Card> p2RequestedCards = p2.getCardCollection().stream().filter(c -> lockedProposal.getRequestedCardIds().contains(c.getId())).collect(Collectors.toList());
                    logger.info("[TRADE-EXEC] P1 offering {} cards, P2 offering {} cards", 
                            p1OfferedCards.size(), p2RequestedCards.size());

                    logger.info("[TRADE-EXEC] Exchanging cards");
                    p1.getCardCollection().removeAll(p1OfferedCards);
                    p1.getCardCollection().addAll(p2RequestedCards);
                    logger.info("[TRADE-EXEC] P1 cards updated. New collection size: {}", p1.getCardCollection().size());

                    p2.getCardCollection().removeAll(p2RequestedCards);
                    p2.getCardCollection().addAll(p1OfferedCards);
                    logger.info("[TRADE-EXEC] P2 cards updated. New collection size: {}", p2.getCardCollection().size());

                    logger.info("[TRADE-EXEC] Saving players to repository");
                    playerRepository.save(p1);
                    logger.info("[TRADE-EXEC] P1 saved");
                    playerRepository.save(p2);
                    logger.info("[TRADE-EXEC] P2 saved");
                }
            }

            lockedProposal.setStatus(TradeProposal.Status.COMPLETED);
            logger.info("[TRADE-EXEC] Trade {} status changed to COMPLETED", tradeId);

            logger.info("[TRADE-EXEC] Notifying both players of success");
            notifyPlayer(p1.getId(), "UPDATE:TRADE_COMPLETE:SUCCESS");
            logger.info("[TRADE-EXEC] P1 notified");
            notifyPlayer(p2.getId(), "UPDATE:TRADE_COMPLETE:SUCCESS");
            logger.info("[TRADE-EXEC] P2 notified");

            logger.info("[TRADE-EXEC] === Trade execution completed successfully ===");
            return true;

        } finally {
            if (lockAcquired) {
                logger.info("[TRADE-EXEC] Releasing distributed lock for trade {}", tradeId);
                lockService.release();
            }
        }
    }

    private void handleTradeProposal(String proposerId, String targetId, String[] offeredCardIds, String[] requestedCardIds) {
        logger.info("[TRADE] === Starting trade proposal ===");
        logger.info("[TRADE] Proposer: {}, Target: {}", proposerId, targetId);
        logger.info("[TRADE] Offered cards: {}, Requested cards: {}", Arrays.toString(offeredCardIds), Arrays.toString(requestedCardIds));
        
        Player proposer = playerRepository.findById(proposerId).orElse(null);
        Player target = playerRepository.findById(targetId).orElse(null);

        if (proposer == null) {
            logger.warn("[TRADE] Proposer player not found: {}", proposerId);
            notifyPlayer(proposerId, "ERROR:Proposer player not found.");
            return;
        }
        logger.info("[TRADE] Proposer found: {} (nickname: {})", proposerId, proposer.getNickname());

        if (target == null) {
            logger.warn("[TRADE] Target player not found: {}", targetId);
            notifyPlayer(proposerId, "ERROR:Target player not found.");
            return;
        }
        logger.info("[TRADE] Target found: {} (nickname: {})", targetId, target.getNickname());

        List<String> offeredCardIdsList = Arrays.asList(offeredCardIds);
        if (!proposer.hasCards(offeredCardIdsList)) {
            logger.warn("[TRADE] Proposer {} does not have all offered cards: {}", proposerId, offeredCardIdsList);
            notifyPlayer(proposerId, "ERROR:Proposer does not have all the offered cards");
            return;
        }
        logger.info("[TRADE] Proposer has all offered cards");

        List<String> requestedCardIdsList = Arrays.asList(requestedCardIds);
        if (!target.hasCards(requestedCardIdsList)) {
            logger.warn("[TRADE] Target {} does not have all requested cards: {}", targetId, requestedCardIdsList);
            notifyPlayer(proposerId, "ERROR:Target player does not have all the requested cards");
            return;
        }
        logger.info("[TRADE] Target has all requested cards");

        TradeProposal proposal = new TradeProposal(proposerId, targetId,
                Arrays.asList(offeredCardIds),
                Arrays.asList(requestedCardIds));
        tradeService.createTrade(proposal);
        logger.info("[TRADE] Trade proposal created with ID: {}", proposal.getTradeId());

        String tradeMessage = String.format("UPDATE:TRADE_PROPOSAL:%s:%s:%s",
                proposal.getTradeId(), proposerId,
                String.join(",", offeredCardIds) + ":" + String.join(",", requestedCardIds));
        
        logger.info("[TRADE] Notifying target player {} with message: {}", targetId, tradeMessage);
        notifyPlayer(targetId, tradeMessage);
        logger.info("[TRADE] Target notification sent via EventManager");

        logger.info("[TRADE] Notifying proposer {} with success message", proposerId);
        notifyPlayer(proposerId, "SUCCESS:Trade proposal sent to " + target.getNickname());
        logger.info("[TRADE] Proposer notification sent");

        logger.info("[TRADE] === Trade proposal completed successfully ===");
    }

    private void handleTradeAcceptance(String playerId, String tradeId) {
        logger.info("[TRADE] === Starting trade acceptance ===");
        logger.info("[TRADE] Player {} attempting to accept trade {}", playerId, tradeId);
        
        Optional<TradeProposal> proposalOpt = tradeService.findTradeById(tradeId);
        if (proposalOpt.isEmpty()) {
            logger.warn("[TRADE] Trade proposal not found: {}", tradeId);
            notifyPlayer(playerId, "ERROR:Trade proposal not found.");
            return;
        }

        TradeProposal proposal = proposalOpt.get();
        logger.info("[TRADE] Trade found - Proposer: {}, Target: {}, Status: {}", 
                proposal.getProposingPlayerId(), proposal.getTargetPlayerId(), proposal.getStatus());

        if (!proposal.getTargetPlayerId().equals(playerId)) {
            logger.warn("[TRADE] Player {} is not authorized to accept trade {} (target is {})", 
                    playerId, tradeId, proposal.getTargetPlayerId());
            notifyPlayer(playerId, "ERROR:You are not authorized to accept this trade.");
            return;
        }

        if (proposal.getStatus() != TradeProposal.Status.PENDING) {
            logger.warn("[TRADE] Trade {} is not in PENDING status: {}", tradeId, proposal.getStatus());
            notifyPlayer(playerId, "ERROR:This trade is no longer available for acceptance.");
            return;
        }

        proposal.setStatus(TradeProposal.Status.ACCEPTED);
        logger.info("[TRADE] Trade {} status changed to ACCEPTED", tradeId);

        logger.info("[TRADE] Notifying proposer {} that trade was accepted", proposal.getProposingPlayerId());
        notifyPlayer(proposal.getProposingPlayerId(), "UPDATE:TRADE_ACCEPTED:" + tradeId);

        logger.info("[TRADE] Executing trade {}", tradeId);
        boolean success = executeTrade(tradeId);

        if (!success) {
            logger.error("[TRADE] Trade execution failed for trade {}, reverting status", tradeId);
            proposal.setStatus(TradeProposal.Status.PENDING);
            notifyPlayer(playerId, "ERROR:Trade execution failed.");
            notifyPlayer(proposal.getProposingPlayerId(), "ERROR:Trade execution failed.");
        } else {
            logger.info("[TRADE] === Trade acceptance completed successfully ===");
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

    @Scheduled(fixedRate = 1000)
    public void scheduledTurnTimerCheck() {
        Iterable<String> keys = redissonClient.getKeys().getKeysByPattern("game_session:*");
        for (String key : keys) {
            String matchId = key.substring("game_session:".length());
            Optional<GameSession> sessionOpt = gameSessionRepository.findById(matchId);
            if (sessionOpt.isPresent()) {
                GameSession session = sessionOpt.get();
                session.forceEndTurn();
                session.resolveResponseWindow();
                gameSessionRepository.save(session);
            }
        }
    }
}
