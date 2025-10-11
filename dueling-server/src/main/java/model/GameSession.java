package model;

import controller.GameFacade;
import model.service.CardEffectService;
import model.service.PlayerStateManager;
import model.service.ScenarioManager;
import model.service.TurnManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repository.CardRepository;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import java.io.Serializable;

public class GameSession implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(GameSession.class);

    private final String matchId;
    private final transient GameFacade gameFacade;
    private final transient CardRepository cardRepository;
    private boolean gameEnded = false;

    private final PlayerStateManager playerStateManager;
    private final TurnManager turnManager;
    private final CardEffectService cardEffectService;
    private final ScenarioManager scenarioManager;

    private boolean isResponseWindowActive = false;
    private long responseWindowEndTime;
    private Card cardToCounter;
    private Player originalCaster;

    public GameSession(String matchId, Player p1, Player p2, List<Card> deckP1, List<Card> deckP2, GameFacade facade, CardRepository cardRepository) {
        this.matchId = matchId;
        this.gameFacade = facade;
        this.cardRepository = cardRepository;

        this.playerStateManager = new PlayerStateManager(p1, p2, deckP1, deckP2, facade);
        this.turnManager = new TurnManager(p1, p2);
        this.cardEffectService = new CardEffectService();
        this.scenarioManager = new ScenarioManager();
    }

    public synchronized void startGame() {
        playerStateManager.initializeDecks();
        logger.info("Match {} started between {} and {}", matchId, getPlayer1().getId(), getPlayer2().getId());

        ResourceType resourceTypeP1 = getPlayer1().getResourceType();
        ResourceType resourceTypeP2 = getPlayer2().getResourceType();

        String gameStartMsgP1 = String.format("UPDATE:GAME_START:%s:%s:%s:%s:%s:%s",
                matchId, getPlayer2().getNickname(), resourceTypeP1.name(), resourceTypeP2.name(), resourceTypeP1.colorHex, resourceTypeP2.colorHex);
        gameFacade.notifyPlayer(getPlayer1().getId(), gameStartMsgP1);

        String gameStartMsgP2 = String.format("UPDATE:GAME_START:%s:%s:%s:%s:%s:%s",
                matchId, getPlayer1().getNickname(), resourceTypeP2.name(), resourceTypeP1.name(), resourceTypeP2.colorHex, resourceTypeP1.colorHex);
        gameFacade.notifyPlayer(getPlayer2().getId(), gameStartMsgP2);

        playerStateManager.drawCards(getPlayer1(), 5);
        playerStateManager.drawCards(getPlayer2(), 5);
        playerStateManager.regenerateResources();

        startNewTurn();
    }

    private void startNewTurn() {
        if (gameEnded) return;

        applyAndTickScenario();
        if (gameEnded) return;

        turnManager.startNewTurn();
        playerStateManager.setNextAttackBonus(getPlayer1().getId(), 0);
        playerStateManager.setNextAttackBonus(getPlayer2().getId(), 0);

        String message = String.format("UPDATE:NEW_TURN:%s:%d", turnManager.getCurrentPlayerId(), turnManager.getTurnEndTime());
        gameFacade.notifyPlayers(Arrays.asList(getPlayer1().getId(), getPlayer2().getId()), message);
        logger.info("Match {}: Starting turn {} for player {}. Turn ends at {}.", matchId, turnManager.getTurn(), turnManager.getCurrentPlayerId(), turnManager.getTurnEndTime());
    }

    private void switchTurn() {
        turnManager.switchTurn(getPlayer1().getId(), getPlayer2().getId());
        startNewTurn();
    }

    public synchronized void forceEndTurn() {
        if (gameEnded || !turnManager.isTurnExpired()) return;

        String currentPlayerId = turnManager.getCurrentPlayerId();
        logger.info("Match {}: Turn timer expired for player {}. Forcing end of turn.", matchId, currentPlayerId);

        List<Card> hand = playerStateManager.getHand(currentPlayerId);
        int currentResource = playerStateManager.getResource(currentPlayerId);

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

        Player caster = getPlayer(playerId);
        List<Card> hand = playerStateManager.getHand(playerId);
        Optional<Card> cardToPlayOpt = hand.stream().filter(c -> c.getId().equals(cardId)).findFirst();

        if (cardToPlayOpt.isEmpty()) {
            if (!isAutoPlay) gameFacade.notifyPlayer(playerId, "ERROR:Card not in hand");
            return;
        }
        Card card = cardToPlayOpt.get();

        // Handle counter spell
        if (isResponseWindowActive && card.getCardType() == Card.CardType.COUNTER_SPELL) {
            if (playerId.equals(originalCaster.getId())) {
                gameFacade.notifyPlayer(playerId, "ERROR:You cannot counter your own spell");
                return;
            }
            playCounterSpell(caster, card);
            return;
        }

        if (isResponseWindowActive) {
            gameFacade.notifyPlayer(playerId, "ERROR:You must wait for the response window to close");
            return;
        }

        if (!isAutoPlay && !playerId.equals(turnManager.getCurrentPlayerId())) {
            gameFacade.notifyPlayer(playerId, "ERROR:NOT_YOUR_TURN");
            return;
        }

        int currentResource = playerStateManager.getResource(playerId);
        if (currentResource < card.getManaCost()) {
            if (!isAutoPlay) gameFacade.notifyPlayer(playerId, "ERROR:INSUFFICIENT_RESOURCE");
            return;
        }

        playerStateManager.spendResource(playerId, card.getManaCost());
        playerStateManager.removeCardFromHand(playerId, card);
        turnManager.recordPlayedCard(card);

        if (card.getCardType() == Card.CardType.MAGIC) {
            this.isResponseWindowActive = true;
            this.responseWindowEndTime = System.currentTimeMillis() + 5000; // 5 second window
            this.cardToCounter = card;
            this.originalCaster = caster;
            gameFacade.notifyPlayer(getOpponent(playerId).getId(), "UPDATE:RESPONSE_WINDOW_OPEN:" + card.getName());
            logger.info("Response window opened for card {}", card.getName());
        } else {
            executeCardEffect(caster, getOpponent(playerId), card);
        }
    }

    private void playCounterSpell(Player counterPlayer, Card counterCard) {
        playerStateManager.spendResource(counterPlayer.getId(), counterCard.getManaCost());
        playerStateManager.removeCardFromHand(counterPlayer.getId(), counterCard);
        turnManager.recordPlayedCard(counterCard);

        logger.info("{} countered {} with {}", counterPlayer.getNickname(), cardToCounter.getName(), counterCard.getName());
        gameFacade.notifyPlayers(Arrays.asList(getPlayer1().getId(), getPlayer2().getId()), "UPDATE:SPELL_COUNTERED:" + cardToCounter.getName());

        this.isResponseWindowActive = false;
        this.cardToCounter = null;
        this.originalCaster = null;

        // It's still the original caster's turn
        if (!gameEnded) {
            switchTurn();
        }
    }

    public void resolveResponseWindow() {
        if (isResponseWindowActive && System.currentTimeMillis() >= responseWindowEndTime) {
            logger.info("Response window for {} closed", cardToCounter.getName());
            isResponseWindowActive = false;
            executeCardEffect(originalCaster, getOpponent(originalCaster.getId()), cardToCounter);
            cardToCounter = null;
            originalCaster = null;
        }
    }

    private void executeCardEffect(Player caster, Player target, Card card) {
        CardEffect effect = cardEffectService.getCardEffect(card);
        if (effect != null) {
            effect.execute(this, caster, target, card);
        }
        notifyAction(caster, target, card);
        notifyHealthUpdate(caster);
        notifyHealthUpdate(target);
        playerStateManager.regenerateResources();

        checkGameStatus();
        if (!gameEnded) {
            switchTurn();
        }
    }

    private void applyAndTickScenario() {
        int damage = scenarioManager.tickScenario();
        if (damage > 0) {
            Player p1 = getPlayer1();
            Player p2 = getPlayer2();
            p1.setHealthPoints(p1.getHealthPoints() - damage);
            p2.setHealthPoints(p2.getHealthPoints() - damage);
            logger.info("Scenario '{}' deals {} damage to both players.", scenarioManager.getActiveScenario().getName(), damage);
            notifyHealthUpdate(p1);
            notifyHealthUpdate(p2);
        }

        if (!scenarioManager.isScenarioActive()) {
            Card activeScenario = scenarioManager.getActiveScenario();
            if (activeScenario != null) {
                logger.info("Scenario '{}' has ended.", activeScenario.getName());
                gameFacade.notifyPlayers(Arrays.asList(getPlayer1().getId(), getPlayer2().getId()), "UPDATE:SCENARIO_END:" + activeScenario.getName());
                scenarioManager.clearScenario();
            }
        }
        checkGameStatus();
    }

    public void checkGameStatus() {
        if (gameEnded) return;
        if (getPlayer1().getHealthPoints() <= 0) {
            gameEnded = true;
            logger.info("Match {} finished. Winner: {}", matchId, getPlayer2().getId());
            gameFacade.finishGame(matchId, getPlayer2().getId(), getPlayer1().getId());
        } else if (getPlayer2().getHealthPoints() <= 0) {
            gameEnded = true;
            logger.info("Match {} finished. Winner: {}", matchId, getPlayer1().getId());
            gameFacade.finishGame(matchId, getPlayer1().getId(), getPlayer2().getId());
        }
    }

    public void notifyHealthUpdate(Player playerToUpdate) {
        String message = String.format("UPDATE:HEALTH:%s:%d",
            playerToUpdate.getId(), playerToUpdate.getHealthPoints());
        gameFacade.notifyPlayers(Arrays.asList(getPlayer1().getId(), getPlayer2().getId()), message);
    }

    private void notifyAction(Player caster, Player target, Card card) {
        String message = String.format("UPDATE:ACTION:%s:used:'''%s''':on:%s",
            caster.getNickname(), card.getName(), target.getNickname());
        gameFacade.notifyPlayers(Arrays.asList(getPlayer1().getId(), getPlayer2().getId()), message);
    }

    public Player getPlayer(String playerId) {
        return getPlayer1().getId().equals(playerId) ? getPlayer1() : getPlayer2();
    }

    public Player getOpponent(String playerId) {
        return getPlayer1().getId().equals(playerId) ? getPlayer2() : getPlayer1();
    }
    
    public Player getPlayer1() {
        return playerStateManager.getPlayer1();
    }

    public Player getPlayer2() {
        return playerStateManager.getPlayer2();
    }

    public String getMatchId() {
        return matchId;
    }

    public boolean isGameEnded() {
        return gameEnded;
    }

    public PlayerStateManager getPlayerStateManager() {
        return playerStateManager;
    }

    public TurnManager getTurnManager() {
        return turnManager;
    }

    public ScenarioManager getScenarioManager() {
        return scenarioManager;
    }

    // Methods needed by CardEffects
    public void drawCards(Player player, int count) {
        playerStateManager.drawCards(player, count);
    }

    public void setNextAttackBonus(String playerId, int bonus) {
        playerStateManager.setNextAttackBonus(playerId, bonus);
    }

    public int consumeNextAttackBonus(String playerId) {
        return playerStateManager.consumeNextAttackBonus(playerId);
    }

    public void setActiveScenario(Card card, int duration) {
        scenarioManager.setActiveScenario(card, duration);
        gameFacade.notifyPlayers(Arrays.asList(getPlayer1().getId(), getPlayer2().getId()), "UPDATE:SCENARIO_START:" + card.getName());
    }
}
