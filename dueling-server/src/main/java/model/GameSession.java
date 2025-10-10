package model;

import controller.GameFacade;
import repository.CardRepository;
import java.util.*;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GameSession {
    private static final int TURN_DURATION_SECONDS = 20;
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
    private int resourceP1;
    private int resourceP2;
    private String currentPlayerId;
    private long turnEndTime;
    private int nextAttackBonusP1 = 0;
    private int nextAttackBonusP2 = 0;

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
        this.resourceP1 = 3;
        this.resourceP2 = 3;
        this.nextAttackBonusP1 = 0;
        this.nextAttackBonusP2 = 0;
    }

    public synchronized void startGame() {
        Collections.shuffle(deckP1);
        Collections.shuffle(deckP2);
        logger.info("Match {} started between {} and {}", matchId, player1.getId(), player2.getId());

        ResourceType resourceTypeP1 = player1.getResourceType();
        ResourceType resourceTypeP2 = player2.getResourceType();

        String gameStartMsgP1 = String.format("UPDATE:GAME_START:%s:%s:%s:%s:%s:%s",
                matchId, player2.getNickname(), resourceTypeP1.name(), resourceTypeP2.name(), resourceTypeP1.colorHex, resourceTypeP2.colorHex);
        gameFacade.notifyPlayer(player1.getId(), gameStartMsgP1);

        String gameStartMsgP2 = String.format("UPDATE:GAME_START:%s:%s:%s:%s:%s:%s",
                matchId, player1.getNickname(), resourceTypeP2.name(), resourceTypeP1.name(), resourceTypeP2.colorHex, resourceTypeP1.colorHex);
        gameFacade.notifyPlayer(player2.getId(), gameStartMsgP2);

        drawCards(player1, handP1, deckP1, 5);
        drawCards(player2, handP2, deckP2, 5);
        gameFacade.notifyPlayer(player1.getId(), "UPDATE:DRAW_CARDS:" + getCardIds(handP1));
        gameFacade.notifyPlayer(player2.getId(), "UPDATE:DRAW_CARDS:" + getCardIds(handP2));
        
        String resourceMessage = String.format("UPDATE:RESOURCE:%d:%d", resourceP1, resourceP2);
        gameFacade.notifyPlayers(Arrays.asList(player1.getId(), player2.getId()), resourceMessage);

        this.currentPlayerId = new Random().nextBoolean() ? player1.getId() : player2.getId();
        startNewTurn();
    }

    private void startNewTurn() {
        if (gameEnded) return;
        this.turnEndTime = System.currentTimeMillis() + (TURN_DURATION_SECONDS * 1000);
        this.nextAttackBonusP1 = 0; // Reset bonus each turn
        this.nextAttackBonusP2 = 0;

        String message = String.format("UPDATE:NEW_TURN:%s:%d", currentPlayerId, turnEndTime);
        gameFacade.notifyPlayers(Arrays.asList(player1.getId(), player2.getId()), message);
        logger.info("Match {}: Starting turn {} for player {}. Turn ends at {}.", matchId, turn, currentPlayerId, turnEndTime);
    }

    private void switchTurn() {
        turn++;
        currentPlayerId = currentPlayerId.equals(player1.getId()) ? player2.getId() : player1.getId();
        startNewTurn();
    }

    public synchronized void forceEndTurn() {
        if (gameEnded || System.currentTimeMillis() < turnEndTime) return;

        logger.info("Match {}: Turn timer expired for player {}. Forcing end of turn.", matchId, currentPlayerId);

        List<Card> hand = currentPlayerId.equals(player1.getId()) ? handP1 : handP2;
        int currentResource = currentPlayerId.equals(player1.getId()) ? resourceP1 : resourceP2;

        Optional<Card> cardToPlay = hand.stream()
            .filter(c -> c.getManaCost() <= currentResource)
            .min(Comparator.comparingInt(Card::getManaCost));

        if (cardToPlay.isPresent()) {
            logger.info("Match {}: Automatically playing card {} for player {}.", matchId, cardToPlay.get().getName(), currentPlayerId);
            playCard(currentPlayerId, cardToPlay.get().getId(), true);
        } else {
            logger.info("Match {}: No playable card for player {}. Switching turn.", matchId, currentPlayerId);
            switchTurn();
        }
    }

    public synchronized void playCard(String playerId, String cardId) {
        playCard(playerId, cardId, false);
    }

    private synchronized void playCard(String playerId, String cardId, boolean isAutoPlay) {
        if (gameEnded) return;

        if (!isAutoPlay && !playerId.equals(currentPlayerId)) {
            gameFacade.notifyPlayer(playerId, "ERROR:NOT_YOUR_TURN");
            return;
        }

        Player caster = player1.getId().equals(playerId) ? player1 : player2;
        Player target = player1.getId().equals(playerId) ? player2 : player1;
        List<Card> hand = player1.getId().equals(playerId) ? handP1 : handP2;
        int currentResource = player1.getId().equals(playerId) ? resourceP1 : resourceP2;

        Optional<Card> cardToPlayOpt = hand.stream().filter(c -> c.getId().equals(cardId)).findFirst();
        if (cardToPlayOpt.isEmpty()) {
            if (!isAutoPlay) gameFacade.notifyPlayer(playerId, "ERROR:Card not in hand");
            return;
        }

        Card card = cardToPlayOpt.get();
        if (currentResource < card.getManaCost()) {
            if (!isAutoPlay) gameFacade.notifyPlayer(playerId, "ERROR:INSUFFICIENT_RESOURCE");
            return;
        }

        if (player1.getId().equals(playerId)) {
            resourceP1 -= card.getManaCost();
        } else {
            resourceP2 -= card.getManaCost();
        }

        hand.remove(card);

        CardEffect effect = getCardEffect(card);
        if (effect != null) {
            effect.execute(this, caster, target, card);
        }
        notifyAction(caster, target, card);
        notifyHealthUpdate(caster);
        notifyHealthUpdate(target);
        String resourceMessage = String.format("UPDATE:RESOURCE:%d:%d", resourceP1, resourceP2);
        gameFacade.notifyPlayers(Arrays.asList(player1.getId(), player2.getId()), resourceMessage);
        
        checkGameStatus();
        if (!gameEnded) {
            switchTurn();
        }
    }

    public synchronized void regenerateResources() {
        if (gameEnded) return;
        boolean updated = false;
        if (resourceP1 < 10) { resourceP1++; updated = true; }
        if (resourceP2 < 10) { resourceP2++; updated = true; }

        if (updated) {
            String message = String.format("UPDATE:RESOURCE:%d:%d", resourceP1, resourceP2);
            gameFacade.notifyPlayers(Arrays.asList(player1.getId(), player2.getId()), message);
        }
    }

    public void setNextAttackBonus(String playerId, int bonus) {
        if (player1.getId().equals(playerId)) {
            this.nextAttackBonusP1 = bonus;
        } else {
            this.nextAttackBonusP2 = bonus;
        }
    }

    public int consumeNextAttackBonus(String playerId) {
        int bonus = 0;
        if (player1.getId().equals(playerId)) {
            bonus = this.nextAttackBonusP1;
            this.nextAttackBonusP1 = 0;
        } else {
            bonus = this.nextAttackBonusP2;
            this.nextAttackBonusP2 = 0;
        }
        return bonus;
    }

    public synchronized void drawCards(Player player, List<Card> hand, List<Card> deck, int count) {
        for (int i = 0; i < count && !deck.isEmpty(); i++) {
            hand.add(deck.remove(0));
        }
        logger.debug("Player {} drew {} cards. {} cards remaining in deck.", player.getId(), hand.size(), deck.size());
    }

    private CardEffect getCardEffect(Card card) {
        switch (card.getCardType()) {
            case ATTACK: return new AttackEffect();
            case DEFENSE: return new DefenseEffect();
            case MAGIC: return new MagicEffect();
            case ATTRIBUTE: return new AttributeEffect();
            case SCENARIO: return new ScenarioEffect();
            case EQUIPMENT: return new EquipmentEffect();
            default: logger.warn("Effect for {} not implemented", card.getCardType()); return null;
        }
    }

    private void notifyAction(Player caster, Player target, Card card) {
        String message = String.format("UPDATE:ACTION:%s:used:'''%s''':on:%s",
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