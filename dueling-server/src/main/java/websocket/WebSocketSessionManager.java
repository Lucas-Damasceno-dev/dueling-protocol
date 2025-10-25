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

    // Mapeamentos locais para sessões ativas nesta instância
    private final ConcurrentHashMap<String, WebSocketSession> activeSessions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> sessionToPlayerId = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, PrintWriter> playerWriters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> sessionActivity = new ConcurrentHashMap<>();

    public WebSocketSessionManager(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    public void registerSession(WebSocketSession session, String playerId) {
        String sessionId = session.getId();
        activeSessions.put(sessionId, session);
        sessionToPlayerId.put(sessionId, playerId);
        sessionActivity.put(sessionId, System.currentTimeMillis());

        // Armazena o mapeamento em Redis para ser visível globalmente
        RMap<String, String> redisSessionMap = redissonClient.getMap("websocket:sessions");
        redisSessionMap.put(sessionId, playerId);

        logger.debug("Registered session {} for player {}", sessionId, playerId);
    }

    public String unregisterSession(String sessionId) {
        activeSessions.remove(sessionId);
        sessionActivity.remove(sessionId);
        String playerId = sessionToPlayerId.remove(sessionId);

        if (playerId != null) {
            playerWriters.remove(playerId);
            // Remove o mapeamento do Redis
            RMap<String, String> redisSessionMap = redissonClient.getMap("websocket:sessions");
            redisSessionMap.remove(sessionId);
            logger.debug("Unregistered session {} for player {}", sessionId, playerId);
        }
        return playerId;
    }

    public void updateSessionActivity(String sessionId) {
        if (sessionActivity.containsKey(sessionId)) {
            sessionActivity.put(sessionId, System.currentTimeMillis());
        }
    }

    public String getPlayerId(String sessionId) {
        return sessionToPlayerId.get(sessionId);
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
    }

    public PrintWriter getPlayerWriter(String playerId) {
        return playerWriters.get(playerId);
    }

    public PrintWriter removePlayerWriter(String playerId) {
        return playerWriters.remove(playerId);
    }
}
