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

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
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

    @Value("${server.name}")
    private String serverName;
    @Value("${server.port}")
    private String serverPort;
    private String selfUrl;

    private static final Logger logger = LoggerFactory.getLogger(GameFacade.class);

    private final ChatGroupService chatGroupService;

    @Autowired
    public GameFacade(MatchmakingService matchmakingService, StoreService storeService,
                      PlayerRepository playerRepository, repository.JpaPlayerRepository jpaPlayerRepository,
                      IEventManager eventManager, ServerRegistry serverRegistry, ServerApiClient serverApiClient,
                      TradeService tradeService, LeaderElectionService leaderElectionService,
                      CardRepository cardRepository, DeckService deckService, GameSessionRepository gameSessionRepository,
                      RedissonClient redissonClient, RankingService rankingService, AchievementService achievementService,
                      ChatGroupService chatGroupService, InGameChatService inGameChatService, EmoteService emoteService) {
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
