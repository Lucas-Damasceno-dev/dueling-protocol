package repository;

import model.Player;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * PostgreSQL-based implementation of the PlayerRepository interface.
 * Uses JPA to persist player data in a centralized database.
 */
@Repository
@Primary  // This makes this implementation preferred over others
@Profile({"!test", "distributed-db"})  // Use this when not in test and distributed-db profile is active
public class PlayerRepositoryPostgreSQL implements PlayerRepository {
    
    @Autowired
    private JpaPlayerRepository jpaPlayerRepository;
    
    /**
     * Saves a player to the database.
     * This operation will create a new player if one doesn't exist, or update an existing one.
     *
     * @param player the player to save
     */
    @Override
    public void save(Player player) {
        jpaPlayerRepository.save(player);
    }
    
    /**
     * Finds a player by their unique identifier.
     *
     * @param id the unique identifier of the player
     * @return an Optional containing the player if found, or empty if not found
     */
    @Override
    public Optional<Player> findById(String id) {
        return jpaPlayerRepository.findById(id);
    }
    
    /**
     * Updates an existing player in the database.
     * In JPA, save() handles both create and update operations.
     *
     * @param player the player to update
     */
    @Override
    public void update(Player player) {
        jpaPlayerRepository.save(player);
    }
}