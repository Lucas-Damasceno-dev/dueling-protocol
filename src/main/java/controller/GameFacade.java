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
import service.election.LeaderElectionService;
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
    private final IEventManager eventManager;
    private final ServerRegistry serverRegistry;
    private final ServerApiClient serverApiClient;
    private final TradeService tradeService;
    private final LeaderElectionService leaderElectionService;
    private final CardRepository cardRepository;

    @Value("${server.name}")
    private String serverName;
    @Value("${server.port}")
    private String serverPort;
    private String selfUrl;

    private static final Logger logger = LoggerFactory.getLogger(GameFacade.class);
    private final Map<String, GameSession> activeGames = new ConcurrentHashMap<>();

    @Autowired
    public GameFacade(MatchmakingService matchmakingService, StoreService storeService,
                      PlayerRepository playerRepository, IEventManager eventManager,
                      ServerRegistry serverRegistry, ServerApiClient serverApiClient,
                      TradeService tradeService, LeaderElectionService leaderElectionService,
                      CardRepository cardRepository) {
        this.matchmakingService = matchmakingService;
        this.storeService = storeService;
        this.playerRepository = playerRepository;
        this.eventManager = eventManager;
        this.serverRegistry = serverRegistry;
        this.serverApiClient = serverApiClient;
        this.tradeService = tradeService;
        this.leaderElectionService = leaderElectionService;
        this.cardRepository = cardRepository;
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
                if (command.length < 5) {
                    notifyPlayer(playerId, "ERROR:Incomplete character setup command.");
                    return;
                }
                Player newPlayer = new Player(playerId, command[3]);
                playerRepository.save(newPlayer);
                notifyPlayer(playerId, "SUCCESS:Character created.");
                break;

            case "MATCHMAKING":
                if (command.length > 3 && "ENTER".equals(command[3])) {
                    enterMatchmaking(player);
                    notifyPlayer(playerId, "SUCCESS:Entered matchmaking queue.");
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
                if (session != null && session.playCard(playerId, cardId)) {
                    logger.info("Player {} played card {} in match {}", playerId, cardId, matchId);
                    notifyPlayer(playerId, "SUCCESS:Move executed.");
                } else {
                    logger.warn("Invalid move or player on cooldown. Player: {}, Card: {}, Match: {}", 
                               playerId, cardId, matchId);
                    notifyPlayer(playerId, "ERROR:Invalid move or player on cooldown.");
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

            default:
                notifyPlayer(playerId, "ERROR:Unknown command '" + action + "'.");
                break;
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

            boolean p1HasCards = p1.getCardCollection().stream().map(Card::getId).collect(Collectors.toList()).containsAll(proposal.getOfferedCardIds());
            boolean p2HasCards = p2.getCardCollection().stream().map(Card::getId).collect(Collectors.toList()).containsAll(proposal.getRequestedCardIds());

            if (!p1HasCards || !p2HasCards) {
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
        List<String> proposerCardIds = proposer.getCardCollection().stream().map(Card::getId).collect(Collectors.toList());
        for (String cardId : offeredCardIds) {
            if (!proposerCardIds.contains(cardId)) {
                notifyPlayer(proposerId, "ERROR:Proposer does not have card " + cardId);
                return;
            }
        }
        
        // Validate that the target has the cards they are requested to give
        List<String> targetCardIds = target.getCardCollection().stream().map(Card::getId).collect(Collectors.toList());
        for (String cardId : requestedCardIds) {
            if (!targetCardIds.contains(cardId)) {
                notifyPlayer(proposerId, "ERROR:Target player does not have card " + cardId);
                return;
            }
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
            // If execution failed, revert the status
            proposal.setStatus(TradeProposal.Status.PENDING);
            notifyPlayer(playerId, "ERROR:Trade execution failed.");
            notifyPlayer(proposal.getProposingPlayerId(), "ERROR:Trade execution failed.");
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
    
    @Scheduled(fixedRate = 2000) // Try to create matches every 2 seconds
    public void scheduledMatchmaking() {
        tryToCreateMatch(); // Attempt to create matches periodically
    }
}
