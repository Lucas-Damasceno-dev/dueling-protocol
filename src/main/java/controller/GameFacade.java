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

import java.io.PrintWriter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GameFacade {
    private final MatchmakingService matchmakingService;
    private final StoreService storeService;
    private final PlayerRepository playerRepository;
    private final Map<String, GameSession> activeGames = new ConcurrentHashMap<>();
    private final Map<String, PrintWriter> activeClients = new ConcurrentHashMap<>();
    
    private static final Logger logger = LoggerFactory.getLogger(GameFacade.class);

    public GameFacade() {
        this.matchmakingService = ConcurrentMatchmakingService.getInstance();
        this.storeService = new StoreServiceImpl();
        this.playerRepository = new PlayerRepositoryJson();
    }

    public void registerClient(String playerId, PrintWriter writer) {
        activeClients.put(playerId, writer);
        logger.info("Cliente registrado: {}", playerId);
    }

    public void removeClient(String playerId) {
        activeClients.remove(playerId);
        logger.info("Cliente removido: {}", playerId);
    }

    public void enterMatchmaking(Player player) {
        matchmakingService.addPlayerToQueue(player);
        logger.info("Jogador {} adicionado à fila de matchmaking", player.getId());
        tryToCreateMatch();
    }

    public void tryToCreateMatch() {
    matchmakingService.findMatch().ifPresent(match -> {
        Player p1 = match.getPlayer1();
        Player p2 = match.getPlayer2();

        if (!activeClients.containsKey(p1.getId()) || !activeClients.containsKey(p2.getId())) {
            logger.warn("Partida cancelada. Um ou ambos os jogadores ({}, {}) desconectaram antes do início.", p1.getId(), p2.getId());

            if (activeClients.containsKey(p1.getId())) {
                matchmakingService.addPlayerToQueue(p1);
                logger.info("Jogador {} retornado para a fila.", p1.getId());
            }
            if (activeClients.containsKey(p2.getId())) {
                matchmakingService.addPlayerToQueue(p2);
                logger.info("Jogador {} retornado para a fila.", p2.getId());
            }
            return; 
        }

        String matchId = UUID.randomUUID().toString();

        PrintWriter outP1 = activeClients.get(p1.getId());
        PrintWriter outP2 = activeClients.get(p2.getId());

        if (outP1 == null || outP2 == null) {
            logger.error("Falha crítica: Cliente ativo para {} ou {} não encontrado, mesmo após a verificação inicial.", p1.getId(), p2.getId());
            return;
        }

        List<Card> deckP1 = new ArrayList<>(p1.getCardCollection());
        List<Card> deckP2 = new ArrayList<>(p2.getCardCollection());

        GameSession session = new GameSession(matchId, p1, p2, deckP1, deckP2, this);
        session.startGame();
        activeGames.put(matchId, session);
        
        logger.info("Nova partida criada entre {} e {} com ID {}", p1.getId(), p2.getId(), matchId);

        outP1.println("UPDATE:GAME_START:" + matchId + ":" + p2.getNickname());
        outP2.println("UPDATE:GAME_START:" + matchId + ":" + p1.getNickname());

        outP1.println("UPDATE:DRAW_CARDS:" + getCardIds(session.getHandP1()));
        outP2.println("UPDATE:DRAW_CARDS:" + getCardIds(session.getHandP2()));
    });
}

    public void notifyPlayers(List<String> playerIds, String message) {
        for (String id : playerIds) {
            PrintWriter writer = activeClients.get(id);
            if (writer != null) {
                writer.println(message);
            }
        }
    }

    private String getCardIds(List<Card> cards) {
        StringBuilder sb = new StringBuilder();
        cards.forEach(c -> sb.append(c.getId()).append(","));
        return sb.length() > 0 ? sb.substring(0, sb.length() - 1) : "";
    }

    public void processGameCommand(String[] command, PrintWriter out) {
        if (command.length < 3) {
            logger.warn("Comando GAME inválido: {}", String.join(":", command));
            out.println("ERROR:Comando GAME inválido.");
            return;
        }
        
        String subAction = command[2];
        if ("PLAY_CARD".equals(subAction)) {
            if (command.length < 5) {
                logger.warn("Comando PLAY_CARD incompleto");
                out.println("ERROR:Comando PLAY_CARD incompleto.");
                return;
            }
            
            String playerId = command[1];
            String matchId = command[3];
            String cardId = command[4];
            GameSession session = activeGames.get(matchId);
            if (session != null && session.playCard(playerId, cardId)) {
                logger.info("Jogador {} jogou carta {} na partida {}", playerId, cardId, matchId);
                out.println("SUCCESS:Jogada realizada.");
            } else {
                logger.warn("Jogada inválida ou jogador em tempo de recarga. Jogador: {}, Carta: {}, Partida: {}", 
                           playerId, cardId, matchId);
                out.println("ERROR:Jogada inválida ou jogador em tempo de recarga.");
            }
        }
    }

    public void buyPack(Player player, String packType) {
        storeService.purchaseCardPack(player, packType);
        playerRepository.update(player);
        logger.info("Jogador {} comprou pacote do tipo {}", player.getId(), packType);
    }

    public void finishGame(String matchId, String winnerId, String loserId) {
        if (activeGames.remove(matchId) == null) {
            logger.warn("Tentativa de finalizar partida inexistente: {}", matchId);
            return;
        }
        
        Optional<Player> winnerOpt = playerRepository.findById(winnerId);
        if (winnerOpt.isPresent()) {
            Player winner = winnerOpt.get();
            int pointsEarned = 10;
            winner.setUpgradePoints(winner.getUpgradePoints() + pointsEarned);
            playerRepository.update(winner);
            logger.info("Partida {} finalizada. {} ganhou {} pontos!", matchId, winner.getNickname(), pointsEarned);
        }

        PrintWriter winnerOut = activeClients.get(winnerId);
        if (winnerOut != null) {
            winnerOut.println("UPDATE:GAME_OVER:VICTORY");
        }

        PrintWriter loserOut = activeClients.get(loserId);
        if (loserOut != null) {
            loserOut.println("UPDATE:GAME_OVER:DEFEAT");
        }
        
        logger.info("Partida {} finalizada. Vencedor: {}, Perdedor: {}", matchId, winnerId, loserId);
    }
}