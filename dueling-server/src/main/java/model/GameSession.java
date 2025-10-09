package model;

import controller.GameFacade;
import repository.CardRepository;
import java.util.*;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GameSession {
    private final String matchId;
    private final GameFacade gameFacade;
    private final CardRepository cardRepository;
    private final Player player1;
    private final Player player2;
    private final List<Card> deckP1;
    private final List<Card> deckP2;
    private final List<Card> handP1;
    private final List<Card> handP2;
    private int turn;
    private boolean gameEnded = false;

    private static final Logger logger = LoggerFactory.getLogger(GameSession.class);

    public GameSession(String matchId, Player p1, Player p2, List<Card> deckP1, List<Card> deckP2, GameFacade facade, CardRepository cardRepository) {
        this.matchId = matchId;
        this.player1 = p1;
        this.player2 = p2;
        this.deckP1 = new ArrayList<>(deckP1);
        this.deckP2 = new ArrayList<>(deckP2);
        this.handP1 = new ArrayList<>();
        this.handP2 = new ArrayList<>();
        this.turn = 1;
        this.gameFacade = facade;
        this.cardRepository = cardRepository;
    }

    public synchronized void startGame() {
        Collections.shuffle(deckP1);
        Collections.shuffle(deckP2);
        logger.info("Match {} started between {} and {}", matchId, player1.getId(), player2.getId());
        drawCards(player1, handP1, deckP1, 5);
        drawCards(player2, handP2, deckP2, 5);
        gameFacade.notifyPlayer(player1.getId(), "UPDATE:DRAW_CARDS:" + getCardIds(handP1));
        gameFacade.notifyPlayer(player2.getId(), "UPDATE:DRAW_CARDS:" + getCardIds(handP2));
    }

    public synchronized void playCard(String playerId, String cardId) {
        Player caster = player1.getId().equals(playerId) ? player1 : player2;
        Player target = player1.getId().equals(playerId) ? player2 : player1;
        List<Card> hand = player1.getId().equals(playerId) ? handP1 : handP2;

        Optional<Card> cardToPlay = hand.stream().filter(c -> c.getId().equals(cardId)).findFirst();
        if (cardToPlay.isPresent()) {
            Card card = cardToPlay.get();
            hand.remove(card);

            CardEffect effect = getCardEffect(card);
            if (effect != null) {
                effect.execute(this, caster, target, card);
            }
            notifyAction(caster, target, card);
            notifyHealthUpdate(caster);
            notifyHealthUpdate(target);
            checkGameStatus();
        } else {
            gameFacade.notifyPlayer(playerId, "ERROR: Card not in hand");
        }
    }

    public synchronized void drawCards(Player player, List<Card> hand, List<Card> deck, int count) {
        for (int i = 0; i < count && !deck.isEmpty(); i++) {
            hand.add(deck.remove(0));
        }
        logger.debug("Player {} drew {} cards. {} cards remaining in deck.", player.getId(), hand.size(), deck.size());
    }

    private CardEffect getCardEffect(Card card) {
        switch (card.getCardType()) {
            case ATTACK:
                return new AttackEffect();
            case DEFENSE:
                return new DefenseEffect();
            case MAGIC:
                return new MagicEffect();
            case ATTRIBUTE:
                return new AttributeEffect();
            case SCENARIO:
                return new ScenarioEffect();
            case EQUIPMENT:
                return new EquipmentEffect();
            default:
                logger.warn("Effect for {} not implemented", card.getCardType());
                return null;
        }
    }

    private void notifyAction(Player caster, Player target, Card card) {
        String message = String.format("UPDATE:ACTION:%s:used:'%s':on:%s",
            caster.getNickname(), card.getName(), target.getNickname());
        gameFacade.notifyPlayers(Arrays.asList(player1.getId(), player2.getId()), message);
    }

    private void notifyHealthUpdate(Player playerToUpdate) {
        String message = String.format("UPDATE:HEALTH:%s:%d",
            playerToUpdate.getId(), playerToUpdate.getHealthPoints());
        gameFacade.notifyPlayers(Arrays.asList(player1.getId(), player2.getId()), message);
    }

    private void checkGameStatus() {
        if (gameEnded) return;

        if (player1.getHealthPoints() <= 0) {
            gameEnded = true;
            logger.info("Match {} finished. Winner: {}", matchId, player2.getId());
            gameFacade.finishGame(matchId, player2.getId(), player1.getId());
        } else if (player2.getHealthPoints() <= 0) {
            gameEnded = true;
            logger.info("Match {} finished. Winner: {}", matchId, player1.getId());
            gameFacade.finishGame(matchId, player1.getId(), player2.getId());
        }
    }

    private String getCardIds(List<Card> cards) {
        return cards.stream().map(Card::getId).collect(Collectors.joining(","));
    }

    private String getCardNames(List<Card> cards) {
        return cards.stream().map(Card::getName).collect(Collectors.joining(", "));
    }

    public Player getPlayer1() { return player1; }
    public Player getPlayer2() { return player2; }
    public List<Card> getHandP1() { return handP1; }
    public List<Card> getHandP2() { return handP2; }
    public int getTurn() { return turn; }
}