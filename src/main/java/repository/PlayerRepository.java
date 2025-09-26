package repository;

import model.Player;
import java.util.Optional;

/**
 * Interface for player data access operations.
 * Defines the contract for saving, finding, and updating player information.
 */
public interface PlayerRepository {
    /**
     * Saves a player to the repository.
     *
     * @param player the player to save
     */
    void save(Player player);
    
    /**
     * Finds a player by their unique identifier.
     *
     * @param id the unique identifier of the player
     * @return an Optional containing the player if found, or empty if not found
     */
    Optional<Player> findById(String id);
    
    /**
     * Updates an existing player in the repository.
     *
     * @param player the player to update
     */
    void update(Player player);
}