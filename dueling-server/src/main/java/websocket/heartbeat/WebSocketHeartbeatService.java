package websocket.heartbeat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import websocket.WebSocketSessionManager;

import java.io.IOException;
import java.util.Map;

@Service
public class WebSocketHeartbeatService {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketHeartbeatService.class);
    private static final long MATCH_INACTIVITY_TIMEOUT_MS = 30000; // 30 segundos during matches
    private static final TextMessage PONG_MESSAGE = new TextMessage("PONG");

    private final WebSocketSessionManager sessionManager;

    public WebSocketHeartbeatService(WebSocketSessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    @Scheduled(fixedRate = 10000) // Executa a cada 10 segundos
    public void checkSessions() {
        long now = System.currentTimeMillis();
        Map<String, WebSocketSession> sessions = sessionManager.getActiveSessions();
        Map<String, Long> activity = sessionManager.getSessionActivity();

        logger.trace("Checking {} active WebSocket sessions for inactivity.", sessions.size());

        for (WebSocketSession session : sessions.values()) {
            if (!session.isOpen()) {
                continue;
            }

            String playerId = sessionManager.getPlayerId(session.getId());
            if (playerId == null) {
                continue; // Skip if we can't identify the player
            }

            boolean playerInMatch = sessionManager.isPlayerInMatch(playerId);

            // For menu sessions, skip timeout checking entirely (no timeout in menu)
            if (!playerInMatch) {
                // For players in menu, only send PONG if needed for connection health
                // but don't check for timeouts
                try {
                    session.sendMessage(PONG_MESSAGE);
                } catch (IOException e) {
                    logger.warn("Failed to send PONG to session {}: {}", session.getId(), e.getMessage());
                }
                continue;
            }

            // Only apply timeout logic for players in matches
            Long lastActivity = activity.get(session.getId());
            if (lastActivity == null || (now - lastActivity) > MATCH_INACTIVITY_TIMEOUT_MS) {
                logger.warn("Closing session {} due to inactivity during match.", session.getId());
                try {
                    session.close(CloseStatus.SESSION_NOT_RELIABLE.withReason("Inactivity timeout during match"));
                } catch (IOException e) {
                    logger.error("Error closing inactive session {}: {}", session.getId(), e.getMessage());
                }
            } else {
                try {
                    // Envia uma mensagem PONG para manter a conexão viva e permitir que o cliente detecte problemas
                    // during matches when timeout is active
                    session.sendMessage(PONG_MESSAGE);
                } catch (IOException e) {
                    logger.warn("Failed to send PONG to session {}: {}", session.getId(), e.getMessage());
                }
            }
        }
    }
}
