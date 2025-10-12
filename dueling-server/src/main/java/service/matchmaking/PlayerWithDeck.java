package service.matchmaking;

import model.Player;

/**
 * Wrapper class to hold a player and their selected deck for matchmaking.
 * This allows tracking which deck a player has selected when entering the matchmaking queue.
 */
public class PlayerWithDeck {
    private Player player;
    private String deckId;

    /**
     * Constructor to create a PlayerWithDeck instance.
     *
     * @param player The player entering matchmaking
     * @param deckId The ID of the deck the player wants to use (can be null for default)
     */
    public PlayerWithDeck(Player player, String deckId) {
        this.player = player;
        this.deckId = deckId;
    }

    // Getters and setters
    public Player getPlayer() {
        return player;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    public String getDeckId() {
        return deckId;
    }

    public void setDeckId(String deckId) {
        this.deckId = deckId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        PlayerWithDeck that = (PlayerWithDeck) o;
        
        if (player != null ? !player.equals(that.player) : that.player != null) return false;
        return deckId != null ? deckId.equals(that.deckId) : that.deckId == null;
    }

    @Override
    public int hashCode() {
        int result = player != null ? player.hashCode() : 0;
        result = 31 * result + (deckId != null ? deckId.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "PlayerWithDeck{" +
                "playerId='" + (player != null ? player.getId() : "null") + '\'' +
                ", deckId='" + deckId + '\'' +
                '}';
    }
}