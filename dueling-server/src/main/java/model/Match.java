package model;

import java.util.UUID;

/**
 * Represents a match between two players in the Dueling Protocol game.
 * Each match has a unique identifier and the two participating players.
 */
public class Match {
    private String matchId;
    private Player player1;
    private Player player2;
    
    /**
     * Constructor to create a new match between two players.
     * Automatically generates a unique identifier for the match.
     *
     * @param player1 the first player in the match
     * @param player2 the second player in the match
     */
    public Match(Player player1, Player player2) {
        this.matchId = UUID.randomUUID().toString();
        this.player1 = player1;
        this.player2 = player2;
    }
    
    // Getters
    /**
     * Returns the unique identifier of the match.
     *
     * @return the match ID
     */
    public String getMatchId() { return matchId; }
    
    /**
     * Returns the first player in the match.
     *
     * @return the first player
     */
    public Player getPlayer1() { return player1; }
    
    /**
     * Returns the second player in the match.
     *
     * @return the second player
     */
    public Player getPlayer2() { return player2; }
}