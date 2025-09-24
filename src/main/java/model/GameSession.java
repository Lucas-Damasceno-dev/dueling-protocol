package model;

import java.util.*;
import model.CardEffect;
import model.AttackEffect;

public class GameSession {
    private Player player1;
    private Player player2;
    private List<Card> deckP1;
    private List<Card> deckP2;
    private List<Card> handP1;
    private List<Card> handP2;
    private int turn;
    private long startTime;
    private Map<String, List<String>> pendingActions;

    public GameSession(Player p1, Player p2, List<Card> deckP1, List<Card> deckP2) {
        this.player1 = p1;
        this.player2 = p2;
        this.deckP1 = new ArrayList<>(deckP1);
        this.deckP2 = new ArrayList<>(deckP2);
        this.handP1 = new ArrayList<>();
        this.handP2 = new ArrayList<>();
        this.turn = 1;
        this.pendingActions = new HashMap<>();
    }

    public void startGame() {
        Collections.shuffle(deckP1);
        Collections.shuffle(deckP2);
        player1.setHealthPoints(100);
        player2.setHealthPoints(100);
        drawCards(player1.getId(), 5);
        drawCards(player2.getId(), 5);
        this.startTime = System.currentTimeMillis();
    }

    public void drawCards(String playerId, int count) {
        List<Card> deck = playerId.equals(player1.getId()) ? deckP1 : deckP2;
        List<Card> hand = playerId.equals(player1.getId()) ? handP1 : handP2;
        for (int i = 0; i < count && !deck.isEmpty(); i++) {
            hand.add(deck.remove(0));
        }
    }

    public boolean playCard(String playerId, String cardId) {
        List<Card> hand = playerId.equals(player1.getId()) ? handP1 : handP2;
        Card card = hand.stream().filter(c -> c.getId().equals(cardId)).findFirst().orElse(null);
        if (card == null) return false;
        hand.remove(card);
        pendingActions.computeIfAbsent(playerId, k -> new ArrayList<>()).add(cardId);
        return true;
    }

    public void resolveActions() {
        for (Map.Entry<String, List<String>> entry : pendingActions.entrySet()) {
            String playerId = entry.getKey();
            Player caster = playerId.equals(player1.getId()) ? player1 : player2;
            Player target = playerId.equals(player1.getId()) ? player2 : player1;
            List<Card> hand = playerId.equals(player1.getId()) ? handP1 : handP2;
            for (String cardId : entry.getValue()) {
                Card card = hand.stream().filter(c -> c.getId().equals(cardId)).findFirst().orElse(null);
                if (card == null) continue;
                CardEffect effect = null;
                switch (card.getCardType()) {
                    case ATTACK:
                        effect = new AttackEffect();
                        break;
                    default:
                        break;
                }
                if (effect != null) {
                    effect.execute(this, caster, target, card);
                }
            }
        }
        pendingActions.clear();
        turn++;
    }

    public Player getPlayer1() { return player1; }
    public Player getPlayer2() { return player2; }
    public List<Card> getHandP1() { return handP1; }
    public List<Card> getHandP2() { return handP2; }
    public int getTurn() { return turn; }
}
