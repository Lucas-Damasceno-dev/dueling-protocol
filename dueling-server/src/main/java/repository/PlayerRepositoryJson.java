package repository;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import model.Player;
import java.io.*;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

/**
 * JSON-based implementation of the PlayerRepository interface.
 * This class persists player data to individual JSON files in a "players" directory.
 */
@org.springframework.context.annotation.Profile("!distributed-db") // Use this when NOT using distributed-db profile
@Repository
public class PlayerRepositoryJson implements PlayerRepository {
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final String basePath = "players/";
    
    private static final Logger logger = LoggerFactory.getLogger(PlayerRepositoryJson.class);

    /**
     * Sanitizes a player ID to prevent path traversal attacks.
     * Only allows alphanumeric characters, hyphens, underscores, and periods.
     * 
     * @param playerId the player ID to sanitize
     * @return a sanitized version of the player ID
     */
    private String sanitizePlayerId(String playerId) {
        if (playerId == null) {
            throw new IllegalArgumentException("Player ID cannot be null");
        }
        // Remove any path traversal sequences
        String sanitized = playerId.replaceAll("\\.\\./", "").replaceAll("\\.\\.\\\\", "");
        // Only allow safe characters
        sanitized = sanitized.replaceAll("[^a-zA-Z0-9\\-_\\.]", "_");
        // Ensure it doesn't start with a dot to prevent hidden files
        if (sanitized.startsWith(".")) {
            sanitized = "_" + sanitized.substring(1);
        }
        return sanitized;
    }

    /**
     * {@inheritDoc}
     * Saves a player to a JSON file named after their ID.
     *
     * @param player the player to save
     */
    @Override
    public synchronized void save(Player player) {
        try {
            String sanitizedId = sanitizePlayerId(player.getId());
            File dir = new File(basePath);
            if (!dir.exists()) {
                if (!dir.mkdirs()) {
                    logger.error("Could not create directory: {}", basePath);
                    return;
                }
            }
            FileWriter writer = new FileWriter(basePath + sanitizedId + ".json");
            gson.toJson(player, writer);
            writer.close();
            logger.debug("Player {} saved successfully", player.getId());
        } catch (IOException e) {
            logger.error("Error saving player {}: {}", player.getId(), e.getMessage(), e);
        }
    }

    /**
     * {@inheritDoc}
     * Loads a player from a JSON file by their ID.
     *
     * @param id the unique identifier of the player
     * @return an Optional containing the player if found, or empty if not found
     */
    @Override
    public synchronized Optional<Player> findById(String id) {
        try {
            String sanitizedId = sanitizePlayerId(id);
            File file = new File(basePath + sanitizedId + ".json");
            if (!file.exists()) {
                logger.debug("Player file {} not found", id);
                return Optional.empty();
            }
            FileReader reader = new FileReader(file);
            Player player = gson.fromJson(reader, Player.class);
            reader.close();
            logger.debug("Player {} loaded successfully", id);
            return Optional.ofNullable(player);
        } catch (Exception e) {
            logger.error("Error loading player {}: {}", id, e.getMessage(), e);
            return Optional.empty();
        }
    }

    /**
     * {@inheritDoc}
     * Updates a player by saving them to their JSON file.
     *
     * @param player the player to update
     */
    @Override
    public synchronized void update(Player player) {
        save(player);
    }
}

