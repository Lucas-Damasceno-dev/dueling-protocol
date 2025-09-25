package model;

import controller.GameFacade;
import repository.CardRepository;
import java.util.*;

public class GameSession {
    private final String matchId;
    private final GameFacade gameFacade;
    private final Player player1;
    private final Player player2;
    private final List<Card> deckP1;
    private final List<Card> deckP2;
    private final List<Card> handP1;
    private final List<Card> handP2;
    private int turn;
    private final Map<String, List<String>> pendingActions;
    private boolean gameEnded = false;

    private long lastActionTimeP1;
    private long lastActionTimeP2;
    private static final long GLOBAL_COOLDOWN_MS = 3000;

    public GameSession(String matchId, Player p1, Player p2, List<Card> deckP1, List<Card> deckP2, GameFacade facade) {
        this.matchId = matchId;
        this.player1 = p1;
        this.player2 = p2;
        this.deckP1 = new ArrayList<>(deckP1);
        this.deckP2 = new ArrayList<>(deckP2);
        this.handP1 = new ArrayList<>();
        this.handP2 = new ArrayList<>();
        this.turn = 1;
        this.pendingActions = new HashMap<>();
        this.gameFacade = facade;
    }

    public void startGame() {
        Collections.shuffle(deckP1);
        Collections.shuffle(deckP2);
        long currentTime = System.currentTimeMillis();
        this.lastActionTimeP1 = currentTime;
        this.lastActionTimeP2 = currentTime;
        drawCards(player1.getId(), 5);
        drawCards(player2.getId(), 5);
    }
        public void drawCards(String playerId, int count) {
        List<Card> deck = playerId.equals(player1.getId()) ? deckP1 : deckP2;
        List<Card> hand = playerId.equals(player1.getId()) ? handP1 : handP2;
        for (int i = 0; i < count && !deck.isEmpty(); i++) {
            hand.add(deck.remove(0));
        }
    }

    public boolean playCard(String playerId, String cardId) {
        long currentTime = System.currentTimeMillis();
        
        if (playerId.equals(player1.getId())) {
            if (currentTime - lastActionTimeP1 < GLOBAL_COOLDOWN_MS) {
                System.out.println(player1.getNickname() + " está em tempo de recarga.");
                return false;
            }
            lastActionTimeP1 = currentTime;
        } else {
            if (currentTime - lastActionTimeP2 < GLOBAL_COOLDOWN_MS) {
                System.out.println(player2.getNickname() + " está em tempo de recarga.");
                return false;
            }
            lastActionTimeP2 = currentTime;
        }

        List<Card> hand = playerId.equals(player1.getId()) ? handP1 : handP2;
        Card card = hand.stream().filter(c -> c.getId().equals(cardId)).findFirst().orElse(null);
        if (card == null) return false;

        hand.remove(card);
        pendingActions.computeIfAbsent(playerId, k -> new ArrayList<>()).add(card.getId());
        
        resolveActions();
        return true;
    }


    public void resolveActions() {
        if (gameEnded) return;

        for (Map.Entry<String, List<String>> entry : pendingActions.entrySet()) {
            String playerId = entry.getKey();
            Player caster = playerId.equals(player1.getId()) ? player1 : player2;
            Player target = playerId.equals(player1.getId()) ? player2 : player1;
            
            for (String cardId : entry.getValue()) {
                Optional<Card> cardOpt = CardRepository.findById(cardId);
                if (cardOpt.isEmpty()) {
                    continue;
                }
                Card card = cardOpt.get();

                int targetHealthBefore = target.getHealthPoints();

                CardEffect effect = getCardEffect(card);
                if (effect != null) {
                    effect.execute(this, caster, target, card);
                }

                notifyAction(caster, target, card);

                if (target.getHealthPoints() != targetHealthBefore) {
                    notifyHealthUpdate(target);
                }
            }
        }

        pendingActions.clear();
        turn++;
        checkGameStatus();
    }
    
    private CardEffect getCardEffect(Card card) {
        switch (card.getCardType()) {
            case ATTACK:
                return new AttackEffect();
            case MAGIC:
                return new MagicEffect();
            case EQUIPMENT:
                return new EquipmentEffect();
            default:
                System.out.println("Efeito para " + card.getCardType() + " não implementado.");
                return null;
        }
    }

    private void notifyAction(Player caster, Player target, Card card) {
        String message = String.format("UPDATE:ACTION:%s:usou:'%s':em:%s", 
            caster.getNickname(), card.getName(), target.getNickname());
        
        gameFacade.notifyPlayers(Arrays.asList(player1.getId(), player2.getId()), message);
    }

    private void notifyHealthUpdate(Player playerToUpdate) {
        String message = String.format("UPDATE:HEALTH:%s:%d", 
            playerToUpdate.getId(), playerToUpdate.getHealthPoints());
        
        gameFacade.notifyPlayers(Arrays.asList(player1.getId(), player2.getId()), message);
    }

    private void checkGameStatus() {
        if (player1.getHealthPoints() <= 0) {
            gameEnded = true;
            gameFacade.finishGame(matchId, player2.getId(), player1.getId());
        } else if (player2.getHealthPoints() <= 0) {
            gameEnded = true;
            gameFacade.finishGame(matchId, player1.getId(), player2.getId());
        }
    }
    
    public Player getPlayer1() { return player1; }
    public Player getPlayer2() { return player2; }
    public List<Card> getHandP1() { return handP1; }
    public List<Card> getHandP2() { return handP2; }
    public int getTurn() { return turn; }

}