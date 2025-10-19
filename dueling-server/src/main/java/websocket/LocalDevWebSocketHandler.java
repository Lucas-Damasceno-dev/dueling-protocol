package websocket;

import model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import pubsub.IEventManager;
import repository.UserRepository;
import security.JwtUtil;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

@Component
@Profile("local-dev")
public class LocalDevWebSocketHandler extends TextWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(LocalDevWebSocketHandler.class);
    private final IEventManager eventManager;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final WebSocketSessionManager sessionManager;

    @Autowired
    public LocalDevWebSocketHandler(UserRepository userRepository, JwtUtil jwtUtil, 
                                   WebSocketSessionManager sessionManager, IEventManager eventManager) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
        this.sessionManager = sessionManager;
        this.eventManager = eventManager;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String username = null;
        
        // First, try to get username from X-User-Id header (from gateway authentication)
        if (session.getHandshakeHeaders() != null) {
            java.util.List<String> userIdHeaders = session.getHandshakeHeaders().get("X-User-Id");
            if (userIdHeaders != null && !userIdHeaders.isEmpty()) {
                username = userIdHeaders.get(0);
                logger.info("Found username in X-User-Id header: {}", username);
            } else {
                logger.info("No X-User-Id header found in WebSocket handshake");
            }
        }
        
        // If not found in headers, try to extract from token in query parameters (for direct server connections)
        if (username == null) {
            String token = getJwtTokenFromSession(session);
            if (token == null) {
                logger.error("No authentication token found in WebSocket handshake headers or query parameters for session: {}", session.getId());
                session.close(CloseStatus.BAD_DATA.withReason("Authentication token is required"));
                return;
            }

            if (!jwtUtil.validateToken(token)) {
                logger.error("Invalid authentication token provided for session: {}", session.getId());
                session.close(CloseStatus.BAD_DATA.withReason("Invalid authentication token"));
                return;
            }

            username = jwtUtil.getUsernameFromToken(token);
            logger.info("Successfully authenticated WebSocket connection using JWT token for user: {}", username);
        } else {
            logger.info("Authenticated WebSocket connection using gateway headers for user: {}", username);
        }

        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            session.close(CloseStatus.BAD_DATA.withReason("User not found"));
            return;
        }

        String playerId = String.valueOf(user.getId());
        sessionManager.registerSession(session, playerId);

        PrintWriter writer = new PrintWriter(new WebSocketWriter(session));
        sessionManager.storePlayerWriter(playerId, writer);

        eventManager.subscribe(playerId, writer);

        logger.info("WebSocket connection established for player {} (user: {}): session {}", playerId, username, session.getId().toString());
        writer.println("SUCCESS:CONNECTED");
        writer.flush();
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        sessionManager.updateSessionActivity(session.getId().toString());
        String payload = message.getPayload();

        if ("PING".equals(payload)) {
            try {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage("PONG"));
                }
            } catch (IOException e) {
                logger.debug("Failed to send PONG to session {}: {}", session.getId(), e.getMessage());
            }
            return;
        }

        String playerId = sessionManager.getPlayerId(session.getId().toString());
        if (playerId == null) {
            logger.warn("Received message from unknown session: {}", session.getId().toString());
            return;
        }

        logger.debug("Received command from player {}: {}", playerId, payload);
        
        // Para desenvolvimento local, vamos apenas ecoar as mensagens recebidas
        // Em um ambiente real, aqui seria a lógica do jogo
        if (payload.startsWith("CHAT:")) {
            // Echo de mensagens de chat
            eventManager.publish(playerId, "ECHO:" + payload);
        } else if (payload.startsWith("GAME:")) {
            // Tratamento básico de comandos de jogo
            String[] parts = payload.split(":");
            if (parts.length > 1) {
                String action = parts[1];
                switch (action) {
                    case "START":
                        eventManager.publish(playerId, "GAME_STARTED:Welcome to local development mode!");
                        break;
                    case "END":
                        eventManager.publish(playerId, "GAME_ENDED:Thanks for playing!");
                        break;
                    default:
                        eventManager.publish(playerId, "UNKNOWN_COMMAND:" + action);
                        break;
                }
            }
        } else {
            eventManager.publish(playerId, "UNKNOWN_COMMAND");
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String playerId = sessionManager.unregisterSession(session.getId().toString());
        if (playerId != null) {
            logger.info("WebSocket connection closed for player {}: session {} with status {}", playerId, session.getId().toString(), status);

            PrintWriter writer = sessionManager.removePlayerWriter(playerId);
            if (writer != null) {
                eventManager.unsubscribe(playerId, writer);
            }
        }
    }

    private String getJwtTokenFromSession(WebSocketSession session) {
        String query = session.getUri().getQuery();
        if (query != null) {
            for (String param : query.split("&")) {
                String[] pair = param.split("=");
                if (pair.length == 2 && "token".equals(pair[0])) {
                    return pair[1];
                }
            }
        }
        return null;
    }

    private static class WebSocketWriter extends StringWriter {
        private final WebSocketSession session;

        public WebSocketWriter(WebSocketSession session) {
            this.session = session;
        }

        @Override
        public void flush() {
            try {
                if (session.isOpen()) {
                    String message = this.toString();
                    if (!message.isEmpty()) {
                        synchronized (session) {
                            if (session.isOpen()) {
                                session.sendMessage(new TextMessage(message));
                            }
                        }
                        getBuffer().setLength(0);
                    }
                }
            } catch (IOException e) {
                if (e.getMessage() != null && 
                    (e.getMessage().contains("Broken pipe") || 
                     e.getMessage().contains("Connection reset"))) {
                    logger.debug("Connection already closed for session {}", session.getId());
                } else {
                    logger.warn("Error sending message via WebSocket for session {}: {}", session.getId().toString(), e.getMessage());
                }
            } catch (IllegalStateException e) {
                logger.debug("Session {} already closed", session.getId());
            }
        }
    }
}