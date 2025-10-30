package service.matchmaking;

import model.Player;
import model.Match;
import java.util.Optional;

/**
 * Interface for matchmaking services.
 * Defines the contract for adding players to a queue and finding matches.
 */
public interface MatchmakingService {
    /**
     * Adds a player to the matchmaking queue.
     *
     * @param player the player to add to the queue
     */
    void addPlayerToQueue(Player player);
    
    /**
     * Adds a player to the matchmaking queue with their selected deck.
     *
     * @param player the player to add to the queue
     * @param deckId the ID of the deck the player wants to use
     */
    void addPlayerToQueueWithDeck(Player player, String deckId);
    
    /**
     * Attempts to find a match between players in the queue.
     *
     * @return an Optional containing a Match if two players are available, or empty if not enough players
     */
    Optional<Match> findMatch();

    /**
     * Atomically finds and removes a single player from the queue to be matched remotely.
     *
     * @return an Optional containing a Player if one is available, or empty otherwise.
     */
    Optional<Player> findAndLockPartner();
    
    /**
     * Checks if a player is already in the matchmaking queue.
     *
     * @param player the player to check
     * @return true if the player is already in the queue, false otherwise
     */
    boolean isPlayerInQueue(Player player);
}