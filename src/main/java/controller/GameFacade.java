package controller;

import service.matchmaking.MatchmakingService;
import service.matchmaking.ConcurrentMatchmakingService;
import service.store.StoreService;
import service.store.StoreServiceImpl;
import repository.PlayerRepository;
import repository.PlayerRepositoryJson;
import model.Player;
import model.GameSession;
import model.Card;

import java.io.PrintWriter;
import java.util.*;

public class GameFacade {
    private final MatchmakingService matchmakingService;
    private final StoreService storeService;
    private final PlayerRepository playerRepository;
    private final Map<String, GameSession> activeGames = new HashMap<>();

    public GameFacade() {
        this.matchmakingService = ConcurrentMatchmakingService.getInstance();
        this.storeService = new StoreServiceImpl();
        this.playerRepository = new PlayerRepositoryJson();
    }

    public void enterMatchmaking(Player player) {
        matchmakingService.addPlayerToQueue(player);
    }

    public void tryToCreateMatch(PrintWriter out) {
        matchmakingService.findMatch().ifPresent(match -> {
            Player p1 = match.getPlayer1();
            Player p2 = match.getPlayer2();
            String matchId = UUID.randomUUID().toString();
            List<Card> deckP1 = new ArrayList<>(p1.getCardCollection());
            List<Card> deckP2 = new ArrayList<>(p2.getCardCollection());
            
            GameSession session = new GameSession(p1, p2, deckP1, deckP2);
            session.startGame();
            activeGames.put(matchId, session);
        
            out.println("UPDATE:GAME_START:" + matchId + ":" + p2.getNickname());
            out.println("UPDATE:DRAW_CARDS:" + getCardIds(session.getHandP1()));
        });
    }

    private String getCardIds(List<Card> cards) {
        StringBuilder sb = new StringBuilder();
        for (Card c : cards) {
            sb.append(c.getId()).append(",");
        }
        return sb.length() > 0 ? sb.substring(0, sb.length() - 1) : "";
    }

    public void processGameCommand(String[] command, PrintWriter out) {
        String subAction = command[1];
        if ("PLAY_CARD".equals(subAction)) {
            String matchId = command[2];
            String playerId = command[3];
            String cardId = command[4];
            GameSession session = activeGames.get(matchId);
            if (session != null && session.playCard(playerId, cardId)) {
                out.println("UPDATE:OPPONENT_ACTION:Jogada realizada pelo jogador " + playerId);
            } else {
                out.println("ERROR:Jogada inválida ou jogador em tempo de recarga.");
            }
        }
    }

    public void buyPack(Player player, String packType) {
        storeService.purchaseCardPack(player, packType);
        playerRepository.update(player);
    }

    public void finishGame(String matchId, String winnerId) {
        activeGames.remove(matchId);
        Optional<Player> winnerOpt = playerRepository.findById(winnerId);
        
        if (winnerOpt.isPresent()) {
            Player winner = winnerOpt.get();
            int pointsEarned = 10;
            winner.setUpgradePoints(winner.getUpgradePoints() + pointsEarned);
            playerRepository.update(winner);
            System.out.println("Partida finalizada. " + winner.getNickname() + " ganhou " + pointsEarned + " pontos!");
        }
    }
}