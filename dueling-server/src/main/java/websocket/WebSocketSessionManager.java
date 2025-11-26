package websocket;

import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.io.PrintWriter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class WebSocketSessionManager {
    private static final Logger logger = LoggerFactory.getLogger(WebSocketSessionManager.class);

    private final RedissonClient redissonClient;

    // Mapeamentos locais para sessões ativas nesta instância (SEMPRE disponível)
    private final ConcurrentHashMap<String, WebSocketSession> activeSessions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> sessionToPlayerId = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, PrintWriter> playerWriters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> sessionActivity = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Boolean> playerInMatch = new ConcurrentHashMap<>();

    public WebSocketSessionManager(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    public void registerSession(WebSocketSession session, String playerId) {
        String sessionId = session.getId();
        
        // SEMPRE armazena localmente PRIMEIRO
        activeSessions.put(sessionId, session);
        sessionToPlayerId.put(sessionId, playerId);
        sessionActivity.put(sessionId, System.currentTimeMillis());
        playerInMatch.put(playerId, false);

        logger.info("Registered session {} for player {} (local)", sessionId, playerId);

        // Tenta armazenar no Redis, mas NÃO falha se Redis estiver indisponível
        tryStoreInRedis(sessionId, playerId);
    }

    private void tryStoreInRedis(String sessionId, String playerId) {
        try {
            RMap<String, String> redisSessionMap = redissonClient.getMap("websocket:sessions");
            redisSessionMap.put(sessionId, playerId);
            logger.debug("Stored session {} in Redis", sessionId);
        } catch (Exception e) {
            // Redis indisponível - NÃO é crítico, sessão ainda funciona localmente
            logger.warn("Failed to store session {} in Redis (continuing with local storage): {}", 
                sessionId, e.getMessage());
        }
    }

    public String unregisterSession(String sessionId) {
        // Remove localmente PRIMEIRO
        String playerId = sessionToPlayerId.get(sessionId);
        
        activeSessions.remove(sessionId);
        sessionActivity.remove(sessionId);
        if (playerId != null) {
            playerInMatch.remove(playerId);
        }
        String removedPlayerId = sessionToPlayerId.remove(sessionId);

        if (removedPlayerId != null) {
            playerWriters.remove(removedPlayerId);
            logger.info("Unregistered session {} for player {} (local)", sessionId, removedPlayerId);
            
            // Tenta remover do Redis, mas não falha se indisponível
            tryRemoveFromRedis(sessionId);
        }
        return removedPlayerId;
    }

    private void tryRemoveFromRedis(String sessionId) {
        try {
            RMap<String, String> redisSessionMap = redissonClient.getMap("websocket:sessions");
            redisSessionMap.remove(sessionId);
            logger.debug("Removed session {} from Redis", sessionId);
        } catch (Exception e) {
            // Redis indisponível - não é crítico
            logger.warn("Failed to remove session {} from Redis: {}", sessionId, e.getMessage());
        }
    }

    public void updateSessionActivity(String sessionId) {
        if (sessionActivity.containsKey(sessionId)) {
            sessionActivity.put(sessionId, System.currentTimeMillis());
            logger.trace("Updated activity for session {}", sessionId);
        }
    }

    public String getPlayerId(String sessionId) {
        // SEMPRE busca localmente primeiro
        String playerId = sessionToPlayerId.get(sessionId);
        
        if (playerId == null) {
            // Fallback: tenta buscar no Redis (caso servidor tenha reiniciado)
            playerId = tryGetPlayerIdFromRedis(sessionId);
        }
        
        return playerId;
    }

    private String tryGetPlayerIdFromRedis(String sessionId) {
        try {
            RMap<String, String> redisSessionMap = redissonClient.getMap("websocket:sessions");
            String playerId = redisSessionMap.get(sessionId);
            
            if (playerId != null) {
                logger.info("Recovered playerId {} for session {} from Redis", playerId, sessionId);
            }
            
            return playerId;
        } catch (Exception e) {
            logger.warn("Failed to get playerId from Redis for session {}: {}", 
                sessionId, e.getMessage());
            return null;
        }
    }

    public WebSocketSession getSession(String sessionId) {
        return activeSessions.get(sessionId);
    }

    public Map<String, WebSocketSession> getActiveSessions() {
        return Map.copyOf(activeSessions);
    }

    public Map<String, Long> getSessionActivity() {
        return Map.copyOf(sessionActivity);
    }
    
    public void storePlayerWriter(String playerId, PrintWriter writer) {
        playerWriters.put(playerId, writer);
        logger.debug("Stored writer for player {}", playerId);
    }

    public PrintWriter getPlayerWriter(String playerId) {
        return playerWriters.get(playerId);
    }

    public PrintWriter removePlayerWriter(String playerId) {
        PrintWriter writer = playerWriters.remove(playerId);
        if (writer != null) {
            logger.debug("Removed writer for player {}", playerId);
        }
        return writer;
    }
    
    public boolean isPlayerInMatch(String playerId) {
        Boolean inMatch = playerInMatch.get(playerId);
        return inMatch != null && inMatch;
    }
    
    public void setPlayerInMatch(String playerId, boolean inMatch) {
        if (sessionToPlayerId.containsValue(playerId)) {
            playerInMatch.put(playerId, inMatch);
            logger.debug("Set player {} in match status: {}", playerId, inMatch);
        }
    }

    /**
     * Reconecta sessões do Redis após falha
     * Usado durante recuperação de failover
     */
    public void reconnectSessionsFromRedis() {
        try {
            RMap<String, String> redisSessionMap = redissonClient.getMap("websocket:sessions");
            int reconnected = 0;
            
            for (Map.Entry<String, String> entry : redisSessionMap.entrySet()) {
                String sessionId = entry.getKey();
                String playerId = entry.getValue();
                
                // Se não temos localmente, recupera do Redis
                if (!sessionToPlayerId.containsKey(sessionId)) {
                    sessionToPlayerId.put(sessionId, playerId);
                    sessionActivity.put(sessionId, System.currentTimeMillis());
                    reconnected++;
                }
            }
            
            if (reconnected > 0) {
                logger.info("Reconnected {} sessions from Redis after recovery", reconnected);
            }
        } catch (Exception e) {
            logger.error("Failed to reconnect sessions from Redis: {}", e.getMessage());
        }
    }
}
