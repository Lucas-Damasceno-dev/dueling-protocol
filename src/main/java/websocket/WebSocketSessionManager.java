package websocket;

import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Component
public class WebSocketSessionManager {
    private static final Logger logger = LoggerFactory.getLogger(WebSocketSessionManager.class);
    
    private final RedissonClient redissonClient;
    
    // Local cache for quick access to active sessions
    private final ConcurrentHashMap<String, String> localSessionToPlayerId = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, java.io.PrintWriter> localPlayerWriters = new ConcurrentHashMap<>();

    @Autowired
    public WebSocketSessionManager(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    /**
     * Store the mapping between WebSocket session ID and player ID in Redis
     * 
     * @param sessionId The WebSocket session ID
     * @param playerId The player ID associated with this session
     */
    public void storeSessionMapping(String sessionId, String playerId) {
        RMap<String, String> sessionMap = redissonClient.getMap("websocket:sessions");
        sessionMap.put(sessionId, playerId);
        localSessionToPlayerId.put(sessionId, playerId);
        logger.debug("Stored session mapping: {} -> {}", sessionId, playerId);
    }

    /**
     * Retrieve the player ID associated with a WebSocket session ID
     * 
     * @param sessionId The WebSocket session ID
     * @return The player ID associated with this session, or null if not found
     */
    public String getPlayerIdBySessionId(String sessionId) {
        // Check local cache first
        String playerId = localSessionToPlayerId.get(sessionId);
        if (playerId != null) {
            return playerId;
        }
        
        // If not in local cache, check Redis
        RMap<String, String> sessionMap = redissonClient.getMap("websocket:sessions");
        playerId = sessionMap.get(sessionId);
        if (playerId != null) {
            localSessionToPlayerId.put(sessionId, playerId);
        }
        return playerId;
    }

    /**
     * Remove the mapping for a WebSocket session ID
     * 
     * @param sessionId The WebSocket session ID to remove
     * @return The player ID that was associated with this session, or null if not found
     */
    public String removeSessionMapping(String sessionId) {
        localSessionToPlayerId.remove(sessionId);
        
        RMap<String, String> sessionMap = redissonClient.getMap("websocket:sessions");
        return sessionMap.remove(sessionId);
    }

    /**
     * Store the PrintWriter for a player ID in local cache
     * 
     * @param playerId The player ID
     * @param writer The PrintWriter for this player
     */
    public void storePlayerWriter(String playerId, java.io.PrintWriter writer) {
        localPlayerWriters.put(playerId, writer);
    }

    /**
     * Retrieve the PrintWriter for a player ID
     * 
     * @param playerId The player ID
     * @return The PrintWriter for this player, or null if not found
     */
    public java.io.PrintWriter getPlayerWriter(String playerId) {
        return localPlayerWriters.get(playerId);
    }

    /**
     * Remove the PrintWriter for a player ID
     * 
     * @param playerId The player ID
     * @return The PrintWriter that was associated with this player, or null if not found
     */
    public java.io.PrintWriter removePlayerWriter(String playerId) {
        return localPlayerWriters.remove(playerId);
    }
    
    /**
     * Check if a player has an active session (either local or in Redis)
     * 
     * @param playerId The player ID to check
     * @return true if the player has an active session, false otherwise
     */
    public boolean hasActiveSession(String playerId) {
        // Check local cache first
        if (localPlayerWriters.containsKey(playerId)) {
            return true;
        }
        
        // Check Redis for session mappings
        RMap<String, String> sessionMap = redissonClient.getMap("websocket:sessions");
        return sessionMap.readAllValues().contains(playerId);
    }
}