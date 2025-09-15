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
     * Attempts to find a match between players in the queue.
     *
     * @return an Optional containing a Match if two players are available, or empty if not enough players
     */
    Optional<Match> findMatch();
}