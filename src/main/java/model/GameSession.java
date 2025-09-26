package model;

import controller.GameFacade;
import repository.CardRepository;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

// Import card effects
import model.AttackEffect;
import model.DefenseEffect;
import model.MagicEffect;
import model.AttributeEffect;
import model.ScenarioEffect;
import model.EquipmentEffect;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents an active game session between two players.
 * Manages the game state, card decks, hands, and game flow.
 */
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
    
    private static final Logger logger = LoggerFactory.getLogger(GameSession.class);

    /**
     * Constructs a new game session between two players.
     *
     * @param matchId the unique identifier for this match
     * @param p1 the first player
     * @param p2 the second player
     * @param deckP1 the deck of cards for player 1
     * @param deckP2 the deck of cards for player 2
     * @param facade the game facade for communication
     */
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

    /**
     * Starts the game session by shuffling decks and dealing initial cards.
     */
    public void startGame() {
        Collections.shuffle(deckP1);
        Collections.shuffle(deckP2);
        long currentTime = System.currentTimeMillis();
        this.lastActionTimeP1 = currentTime;
        this.lastActionTimeP2 = currentTime;
        drawCards(player1.getId(), 5);
        drawCards(player2.getId(), 5);
        logger.info("Match {} started between {} and {}", matchId, player1.getId(), player2.getId());
    }
    
    /**
     * Draws cards from the player's deck into their hand.
     *
     * @param playerId the ID of the player drawing cards
     * @param count the number of cards to draw
     */
    public void drawCards(String playerId, int count) {
        List<Card> deck = playerId.equals(player1.getId()) ? deckP1 : deckP2;
        List<Card> hand = playerId.equals(player1.getId()) ? handP1 : handP2;
        for (int i = 0; i < count && !deck.isEmpty(); i++) {
            hand.add(deck.remove(0));
        }
        logger.debug("Player {} drew {} cards", playerId, count);
    }

    /**
     * Plays a card from the player's hand.
     * Enforces a global cooldown to prevent spamming actions.
     *
     * @param playerId the ID of the player playing the card
     * @param cardId the ID of the card to play
     * @return true if the card was played successfully, false otherwise
     */
    public boolean playCard(String playerId, String cardId) {
        long currentTime = System.currentTimeMillis();
        
        if (playerId.equals(player1.getId())) {
            if (currentTime - lastActionTimeP1 < GLOBAL_COOLDOWN_MS) {
                logger.debug("{} is on cooldown", player1.getNickname());
                return false;
            }
            lastActionTimeP1 = currentTime;
        } else {
            if (currentTime - lastActionTimeP2 < GLOBAL_COOLDOWN_MS) {
                logger.debug("{} is on cooldown", player2.getNickname());
                return false;
            }
            lastActionTimeP2 = currentTime;
        }

        List<Card> hand = playerId.equals(player1.getId()) ? handP1 : handP2;
        Card card = hand.stream().filter(c -> c.getId().equals(cardId)).findFirst().orElse(null);
        if (card == null) {
            logger.warn("Card {} not found in player {}'s hand", cardId, playerId);
            return false;
        }

        hand.remove(card);
        pendingActions.computeIfAbsent(playerId, k -> new ArrayList<>()).add(card.getId());
        
        logger.info("Player {} played card {}", playerId, cardId);
        resolveActions();
        return true;
    }

    /**
     * Resolves all pending actions for the current turn.
     * Executes card effects and checks for game end conditions.
     */
    public void resolveActions() {
        if (gameEnded) return;

        for (Map.Entry<String, List<String>> entry : pendingActions.entrySet()) {
            String playerId = entry.getKey();
            Player caster = playerId.equals(player1.getId()) ? player1 : player2;
            Player target = playerId.equals(player1.getId()) ? player2 : player1;
            
            for (String cardId : entry.getValue()) {
                Optional<Card> cardOpt = CardRepository.findById(cardId);
                if (cardOpt.isEmpty()) {
                    logger.warn("Card {} not found in repository", cardId);
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
    
    /**
     * Gets the appropriate card effect implementation based on card type.
     *
     * @param card the card for which to get the effect
     * @return the card effect implementation, or null if not found
     */
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

    /**
     * Notifies players of a card action.
     *
     * @param caster the player who cast the card
     * @param target the target of the card effect
     * @param card the card that was played
     */
    private void notifyAction(Player caster, Player target, Card card) {
        String message = String.format("UPDATE:ACTION:%s:used:'%s':on:%s", 
            caster.getNickname(), card.getName(), target.getNickname());
        
        gameFacade.notifyPlayers(Arrays.asList(player1.getId(), player2.getId()), message);
    }

    /**
     * Notifies players of a health update.
     *
     * @param playerToUpdate the player whose health was updated
     */
    private void notifyHealthUpdate(Player playerToUpdate) {
        String message = String.format("UPDATE:HEALTH:%s:%d", 
            playerToUpdate.getId(), playerToUpdate.getHealthPoints());
        
        gameFacade.notifyPlayers(Arrays.asList(player1.getId(), player2.getId()), message);
    }

    /**
     * Checks if the game has ended due to a player's health reaching zero.
     * If the game has ended, notifies players and updates player records.
     */
    private void checkGameStatus() {
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
    
    /**
     * Gets the first player in this session.
     *
     * @return the first player
     */
    public Player getPlayer1() { return player1; }
    
    /**
     * Gets the second player in this session.
     *
     * @return the second player
     */
    public Player getPlayer2() { return player2; }
    
    /**
     * Gets the hand of the first player.
     *
     * @return the first player's hand
     */
    public List<Card> getHandP1() { return handP1; }
    
    /**
     * Gets the hand of the second player.
     *
     * @return the second player's hand
     */
    public List<Card> getHandP2() { return handP2; }
    
    /**
     * Gets the current turn number.
     *
     * @return the current turn number
     */
    public int getTurn() { return turn; }
}