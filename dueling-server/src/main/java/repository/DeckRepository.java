package repository;

import model.Deck;
import model.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Deck entity operations.
 * Provides CRUD operations and custom queries for managing decks.
 */
@Repository
public interface DeckRepository extends JpaRepository<Deck, String> {
    
    /**
     * Find all decks belonging to a specific player.
     * 
     * @param player The player whose decks to retrieve
     * @return List of decks belonging to the player
     */
    List<Deck> findByPlayer(Player player);
    
    /**
     * Find all decks by player ID.
     * 
     * @param playerId The ID of the player
     * @return List of decks belonging to the player
     */
    List<Deck> findByPlayerId(String playerId);
    
    /**
     * Find a deck by its name and player.
     * 
     * @param name The name of the deck
     * @param player The player who owns the deck
     * @return Optional containing the deck if found
     */
    Optional<Deck> findByNameAndPlayer(String name, Player player);
    
    /**
     * Find a deck by its name and player ID.
     * 
     * @param name The name of the deck
     * @param playerId The ID of the player
     * @return Optional containing the deck if found
     */
    Optional<Deck> findByNameAndPlayerId(String name, String playerId);
    
    /**
     * Check if a deck with the given name exists for a specific player.
     * 
     * @param name The name of the deck
     * @param playerId The ID of the player
     * @return true if a deck with the given name exists for the player
     */
    boolean existsByNameAndPlayerId(String name, String playerId);
    
    /**
     * Find the default deck for a player.
     * 
     * @param player The player whose default deck to retrieve
     * @return Optional containing the default deck if found
     */
    Optional<Deck> findByPlayerAndIsDefaultTrue(Player player);
    
    /**
     * Find the default deck by player ID.
     * 
     * @param playerId The ID of the player
     * @return Optional containing the default deck if found
     */
    Optional<Deck> findByPlayerIdAndIsDefaultTrue(String playerId);
    
    /**
     * Count the number of decks for a specific player.
     * 
     * @param player The player whose deck count to retrieve
     * @return Number of decks the player has
     */
    long countByPlayer(Player player);
    
    /**
     * Count the number of decks for a specific player ID.
     * 
     * @param playerId The ID of the player
     * @return Number of decks the player has
     */
    long countByPlayerId(String playerId);
    
    /**
     * Delete all decks belonging to a specific player.
     * 
     * @param player The player whose decks to delete
     */
    void deleteByPlayer(Player player);
    
    /**
     * Delete all decks by player ID.
     * 
     * @param playerId The ID of the player whose decks to delete
     */
    void deleteByPlayerId(String playerId);
    
    /**
     * Find a deck by its ID and player.
     * 
     * @param id The ID of the deck
     * @param player The player who should own the deck
     * @return Optional containing the deck if found
     */
    Optional<Deck> findByIdAndPlayer(String id, Player player);
    
    /**
     * Find a deck by its ID and player ID.
     * 
     * @param id The ID of the deck
     * @param playerId The ID of the player
     * @return Optional containing the deck if found
     */
    Optional<Deck> findByIdAndPlayerId(String id, String playerId);
    
    /**
     * Set a specific deck as the default for a player, and unset all others.
     * 
     * @param id The ID of the deck to set as default
     * @param playerId The ID of the player
     */
    @Query("UPDATE Deck d SET d.isDefault = CASE WHEN d.id = :id THEN true ELSE false END WHERE d.player.id = :playerId")
    void setDefaultDeck(@Param("id") String id, @Param("playerId") String playerId);
}