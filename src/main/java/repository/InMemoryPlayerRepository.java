package repository;

import model.Player;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * In-memory implementation of the PlayerRepository interface.
 * Stores player data in a HashMap for quick access during gameplay.
 * Note: This implementation does not persist data between server restarts.
 */
public class InMemoryPlayerRepository implements PlayerRepository {
    private final Map<String, Player> players = new HashMap<>();
    
    /**
     * {@inheritDoc}
     * Saves a player to the in-memory store.
     *
     * @param player the player to save
     */
    @Override
    public void save(Player player) {
        players.put(player.getId(), player);
    }
    
    /**
     * {@inheritDoc}
     * Finds a player by their unique identifier.
     *
     * @param id the unique identifier of the player
     * @return an Optional containing the player if found, or empty if not found
     */
    @Override
    public Optional<Player> findById(String id) {
        return Optional.ofNullable(players.get(id));
    }
    
    /**
     * {@inheritDoc}
     * Updates an existing player in the in-memory store.
     *
     * @param player the player to update
     */
    @Override
    public void update(Player player) {
        players.put(player.getId(), player);
    }
}