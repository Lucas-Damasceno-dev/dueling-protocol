package repository;

import model.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * JPA repository interface for Player entities.
 * Extends JpaRepository to provide standard CRUD operations.
 */
@Repository
public interface JpaPlayerRepository extends JpaRepository<Player, String> {
    // The basic CRUD operations are inherited from JpaRepository:
    // - save(Player player) - saves or updates a player
    // - findById(String id) - finds a player by ID
    // - deleteById(String id) - deletes a player by ID
    // - findAll() - finds all players, etc.
    
    // Additional custom methods can be added here if needed
    // For example: Optional<Player> findByNickname(String nickname);
}