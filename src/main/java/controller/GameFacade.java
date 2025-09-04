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

public class GameFacade {
    private final MatchmakingService matchmakingService;
    private final StoreService storeService;
    private final PlayerRepository playerRepository;
    private final Map<String, GameSession> activeGames = new ConcurrentHashMap<>();
    private final Map<String, PrintWriter> activeClients = new ConcurrentHashMap<>();

    public GameFacade() {
        this.matchmakingService = ConcurrentMatchmakingService.getInstance();
        this.storeService = new StoreServiceImpl();
        this.playerRepository = new PlayerRepositoryJson();
    }

    public void registerClient(String playerId, PrintWriter writer) {
        activeClients.put(playerId, writer);
        System.out.println("Cliente registrado: " + playerId);
    }

    public void removeClient(String playerId) {
        activeClients.remove(playerId);
        System.out.println("Cliente removido: " + playerId);
    }

    public void enterMatchmaking(Player player) {
        matchmakingService.addPlayerToQueue(player);
        tryToCreateMatch();
    }

    public void tryToCreateMatch() {
        matchmakingService.findMatch().ifPresent(match -> {
            Player p1 = match.getPlayer1();
            Player p2 = match.getPlayer2();
            String matchId = UUID.randomUUID().toString();

            PrintWriter outP1 = activeClients.get(p1.getId());
            PrintWriter outP2 = activeClients.get(p2.getId());

            if (outP1 == null || outP2 == null) {
                return;
            }

            List<Card> deckP1 = new ArrayList<>(p1.getCardCollection());
            List<Card> deckP2 = new ArrayList<>(p2.getCardCollection());

            GameSession session = new GameSession(matchId, p1, p2, deckP1, deckP2, this);
            session.startGame();
            activeGames.put(matchId, session);

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
        String subAction = command[2];
        if ("PLAY_CARD".equals(subAction)) {
            String playerId = command[1];
            String matchId = command[3];
            String cardId = command[4];
            GameSession session = activeGames.get(matchId);
            if (session != null && session.playCard(playerId, cardId)) {
                out.println("SUCCESS:Jogada realizada.");
            } else {
                out.println("ERROR:Jogada inválida ou jogador em tempo de recarga.");
            }
        }
    }

    public void buyPack(Player player, String packType) {
        storeService.purchaseCardPack(player, packType);
        playerRepository.update(player);
    }

    public void finishGame(String matchId, String winnerId, String loserId) {
        if (activeGames.remove(matchId) == null) {
            return;
        }
        
        Optional<Player> winnerOpt = playerRepository.findById(winnerId);
        if (winnerOpt.isPresent()) {
            Player winner = winnerOpt.get();
            int pointsEarned = 10;
            winner.setUpgradePoints(winner.getUpgradePoints() + pointsEarned);
            playerRepository.update(winner);
            System.out.println("Partida finalizada. " + winner.getNickname() + " ganhou " + pointsEarned + " pontos!");
        }

        PrintWriter winnerOut = activeClients.get(winnerId);
        if (winnerOut != null) {
            winnerOut.println("UPDATE:GAME_OVER:VICTORY");
        }

        PrintWriter loserOut = activeClients.get(loserId);
        if (loserOut != null) {
            loserOut.println("UPDATE:GAME_OVER:DEFEAT");
        }
    }
}