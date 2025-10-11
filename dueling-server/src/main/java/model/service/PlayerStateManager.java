package model.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import controller.GameFacade;
import model.Card;
import model.Player;


import java.io.Serializable;
public class PlayerStateManager implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(PlayerStateManager.class);

    private final GameFacade gameFacade;
    private final Player player1;
    private final Player player2;
    private final List<Card> deckP1;
    private final List<Card> deckP2;
    private final List<Card> handP1;
    private final List<Card> handP2;
    private int resourceP1;
    private int resourceP2;
    private int nextAttackBonusP1 = 0;
    private int nextAttackBonusP2 = 0;

    public PlayerStateManager(Player p1, Player p2, List<Card> deckP1, List<Card> deckP2, GameFacade facade) {
        this.player1 = p1;
        this.player2 = p2;
        this.deckP1 = new ArrayList<>(deckP1);
        this.deckP2 = new ArrayList<>(deckP2);
        this.handP1 = new ArrayList<>();
        this.handP2 = new ArrayList<>();
        this.gameFacade = facade;
        this.resourceP1 = 3;
        this.resourceP2 = 3;
    }

    public void initializeDecks() {
        Collections.shuffle(deckP1);
        Collections.shuffle(deckP2);
    }

    public void drawCards(Player player, int count) {
        List<Card> hand = player.getId().equals(player1.getId()) ? this.handP1 : this.handP2;
        List<Card> deck = player.getId().equals(player1.getId()) ? this.deckP1 : this.deckP2;
        
        List<Card> drawnCards = new ArrayList<>();
        for (int i = 0; i < count && !deck.isEmpty(); i++) {
            Card newCard = deck.remove(0);
            hand.add(newCard);
            drawnCards.add(newCard);
        }

        if (!drawnCards.isEmpty()) {
            gameFacade.notifyPlayer(player.getId(), "UPDATE:DRAW_CARDS:" + getCardIds(hand));
            logger.debug("Player {} drew {} cards. {} cards remaining in deck.", player.getId(), drawnCards.size(), deck.size());
        }
    }

    public void regenerateResources() {
        boolean updated = false;
        if (resourceP1 < 10) { resourceP1++; updated = true; }
        if (resourceP2 < 10) { resourceP2++; updated = true; }

        if (updated) {
            String message = String.format("UPDATE:RESOURCE:%d:%d", resourceP1, resourceP2);
            gameFacade.notifyPlayers(java.util.Arrays.asList(player1.getId(), player2.getId()), message);
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
    
    public Player getPlayer1() { return player1; }
    public Player getPlayer2() { return player2; }
    public List<Card> getHand(String playerId) {
        return player1.getId().equals(playerId) ? handP1 : handP2;
    }
    public int getResource(String playerId) {
        return player1.getId().equals(playerId) ? resourceP1 : resourceP2;
    }
    public void spendResource(String playerId, int amount) {
        if (player1.getId().equals(playerId)) {
            resourceP1 -= amount;
        } else {
            resourceP2 -= amount;
        }
    }
    public void removeCardFromHand(String playerId, Card card) {
        getHand(playerId).remove(card);
    }

    private String getCardIds(List<Card> cards) {
        return cards.stream().map(Card::getId).collect(Collectors.joining(","));
    }
}
