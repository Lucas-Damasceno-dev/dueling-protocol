package model.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import controller.GameFacade;
import model.Card;
import model.Player;


import java.io.Serializable;
public class PlayerStateManager implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(PlayerStateManager.class);

    @JsonIgnore
    private transient GameFacade gameFacade;
    private final Player player1;
    private final Player player2;
    private List<Card> deckP1;
    private List<Card> deckP2;
    private List<Card> handP1;
    private List<Card> handP2;
    private int resourceP1;
    private int resourceP2;
    private int nextAttackBonusP1 = 0;
    private int nextAttackBonusP2 = 0;

    @JsonCreator
    public PlayerStateManager(
            @JsonProperty("player1") Player player1,
            @JsonProperty("player2") Player player2,
            @JsonProperty("deckP1") List<Card> deckP1,
            @JsonProperty("deckP2") List<Card> deckP2,
            @JsonProperty("handP1") List<Card> handP1,
            @JsonProperty("handP2") List<Card> handP2,
            @JsonProperty("resourceP1") int resourceP1,
            @JsonProperty("resourceP2") int resourceP2,
            @JsonProperty("nextAttackBonusP1") int nextAttackBonusP1,
            @JsonProperty("nextAttackBonusP2") int nextAttackBonusP2) {
        this.player1 = player1;
        this.player2 = player2;
        this.deckP1 = deckP1 != null ? new ArrayList<>(deckP1) : new ArrayList<>();
        this.deckP2 = deckP2 != null ? new ArrayList<>(deckP2) : new ArrayList<>();
        this.handP1 = handP1 != null ? new ArrayList<>(handP1) : new ArrayList<>();
        this.handP2 = handP2 != null ? new ArrayList<>(handP2) : new ArrayList<>();
        this.resourceP1 = resourceP1;
        this.resourceP2 = resourceP2;
        this.nextAttackBonusP1 = nextAttackBonusP1;
        this.nextAttackBonusP2 = nextAttackBonusP2;
        
        logger.info("[DESERIALIZATION] PlayerStateManager created. handP1 size: {}, handP2 size: {}, deckP1 size: {}, deckP2 size: {}", 
            this.handP1.size(), this.handP2.size(), this.deckP1.size(), this.deckP2.size());
    }

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
            Card newCard = deck.removeFirst();
            hand.add(newCard);
            drawnCards.add(newCard);
        }

        if (!drawnCards.isEmpty() && gameFacade != null) {
            String handIds = getCardIds(hand);
            gameFacade.notifyPlayer(player.getId(), "UPDATE:DRAW_CARDS:" + handIds);
            logger.info("Player {} drew {} cards. Hand now contains: [{}]. {} cards remaining in deck.", 
                player.getId(), drawnCards.size(), handIds, deck.size());
        }
    }

    public void regenerateResources() {
        boolean updated = false;
        if (resourceP1 < 10) { resourceP1++; updated = true; }
        if (resourceP2 < 10) { resourceP2++; updated = true; }

        if (updated && gameFacade != null) {
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
    
    // Getters explícitos para serialização JSON
    public List<Card> getDeckP1() { return deckP1; }
    public List<Card> getDeckP2() { return deckP2; }
    public List<Card> getHandP1() { return handP1; }
    public List<Card> getHandP2() { return handP2; }
    public int getResourceP1() { return resourceP1; }
    public int getResourceP2() { return resourceP2; }
    public int getNextAttackBonusP1() { return nextAttackBonusP1; }
    public int getNextAttackBonusP2() { return nextAttackBonusP2; }
    
    public List<Card> getHand(String playerId) {
        List<Card> hand = player1.getId().equals(playerId) ? handP1 : handP2;
        logger.debug("[GET_HAND] Player {}: hand size = {}, cards = {}", playerId, hand.size(), 
            hand.stream().map(Card::getId).collect(Collectors.joining(",")));
        return hand;
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
        List<Card> hand = getHand(playerId);
        // Remove first occurrence by ID to handle deserialized cards correctly
        for (int i = 0; i < hand.size(); i++) {
            if (hand.get(i).getId().equals(card.getId())) {
                hand.remove(i);
                break;
            }
        }
    }

    private String getCardIds(List<Card> cards) {
        return cards.stream().map(Card::getId).collect(Collectors.joining(","));
    }
}
