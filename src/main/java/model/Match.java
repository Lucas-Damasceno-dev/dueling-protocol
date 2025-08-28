package model;

import java.util.UUID;

public class Match {
    private String matchId;
    private Player player1;
    private Player player2;
    
    public Match(Player player1, Player player2) {
        this.matchId = UUID.randomUUID().toString();
        this.player1 = player1;
        this.player2 = player2;
    }
    
    // Getters
    public String getMatchId() { return matchId; }
    public Player getPlayer1() { return player1; }
    public Player getPlayer2() { return player2; }
}