package model.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import model.Card;
import model.Player;

import java.io.Serializable;

public class TurnManager implements Serializable {
    private static final long serialVersionUID = 1L;
    private int turn;
    private String currentPlayerId;
    private long turnEndTime;
    private static final int TURN_DURATION_SECONDS = 20;
    private List<Card> playedCardsThisTurn;

    public TurnManager(Player player1, Player player2) {
        this.turn = 1;
        this.currentPlayerId = new Random().nextBoolean() ? player1.getId() : player2.getId();
        this.playedCardsThisTurn = new ArrayList<>();
    }

    public void startNewTurn() {
        this.turnEndTime = System.currentTimeMillis() + (TURN_DURATION_SECONDS * 1000);
        this.playedCardsThisTurn.clear();
    }

    public void switchTurn(String p1Id, String p2Id) {
        turn++;
        currentPlayerId = currentPlayerId.equals(p1Id) ? p2Id : p1Id;
        startNewTurn();
    }

    public void recordPlayedCard(Card card) {
        playedCardsThisTurn.add(card);
    }

    public List<Card> getPlayedCardsThisTurn() {
        return playedCardsThisTurn;
    }

    public String getCurrentPlayerId() {
        return currentPlayerId;
    }

    public int getTurn() {
        return turn;
    }

    public long getTurnEndTime() {
        return turnEndTime;
    }

    public boolean isTurnExpired() {
        return System.currentTimeMillis() >= turnEndTime;
    }
}
