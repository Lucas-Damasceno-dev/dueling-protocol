package controller;

import api.ServerApiClient;
import api.registry.ServerRegistry;
import model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import pubsub.EventManager;
import repository.PlayerRepository;
import service.election.LeaderElectionService;
import service.matchmaking.MatchmakingService;
import service.store.PurchaseResult;
import service.store.StoreService;
import service.trade.TradeService;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class GameFacade {
    private final MatchmakingService matchmakingService;
    private final StoreService storeService;
    private final PlayerRepository playerRepository;
    private final EventManager eventManager;
    private final ServerRegistry serverRegistry;
    private final ServerApiClient serverApiClient;
    private final TradeService tradeService;
    private final LeaderElectionService leaderElectionService;

    @Value("${server.name:server-1}")
    private String serverName;
    @Value("${server.port:8080}")
    private String serverPort;
    private String selfUrl;

    private static final Logger logger = LoggerFactory.getLogger(GameFacade.class);
    private final Map<String, GameSession> activeGames = new ConcurrentHashMap<>();

    @Autowired
    public GameFacade(MatchmakingService matchmakingService, StoreService storeService,
                      PlayerRepository playerRepository, EventManager eventManager,
                      ServerRegistry serverRegistry, ServerApiClient serverApiClient,
                      TradeService tradeService, LeaderElectionService leaderElectionService) {
        this.matchmakingService = matchmakingService;
        this.storeService = storeService;
        this.playerRepository = playerRepository;
        this.eventManager = eventManager;
        this.serverRegistry = serverRegistry;
        this.serverApiClient = serverApiClient;
        this.tradeService = tradeService;
        this.leaderElectionService = leaderElectionService;
    }

    private String getSelfUrl() {
        if (selfUrl == null) {
            selfUrl = "http://" + serverName + ":" + serverPort;
        }
        return selfUrl;
    }

    public EventManager getEventManager() {
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
        if (command.length < 2) { 
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
}