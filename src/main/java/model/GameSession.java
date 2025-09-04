package model;

import java.util.*;

public class GameSession {
    private final Player player1;
    private final Player player2;
    private final List<Card> deckP1;
    private final List<Card> deckP2;
    private final List<Card> handP1;
    private final List<Card> handP2;
    private int turn;
    private final Map<String, List<String>> pendingActions;

    private long lastActionTimeP1;
    private long lastActionTimeP2;
    private static final long GLOBAL_COOLDOWN_MS = 3000;

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
        for (Map.Entry<String, List<String>> entry : pendingActions.entrySet()) {
            String playerId = entry.getKey();
            Player caster = playerId.equals(player1.getId()) ? player1 : player2;
            Player target = playerId.equals(player1.getId()) ? player2 : player1;
            
            for (String cardId : entry.getValue()) {
                /*// Card card = findCardById(cardId); // Você precisaria de uma forma de encontrar o card
                // if (card == null) continue;

                // switch (card.getCardType()) {
                //     case ATTACK:
                //         new AttackEffect().execute(this, caster, target, card);
                //         break;
                //     case MAGIC:
                //         // new MagicEffect().execute(this, caster, target, card);
                //         break;
                //     case EQUIPMENT:
                //         // new EquipmentEffect().execute(this, caster, target, card);
                //         break;
                //     // Outros casos...
                //     default:
                //         System.out.println("Efeito para " + card.getCardType() + " não implementado.");
                //         break;
                // }*/
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