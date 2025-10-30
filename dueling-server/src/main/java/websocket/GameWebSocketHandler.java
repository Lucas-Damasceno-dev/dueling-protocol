package websocket;

import controller.GameFacade;
import model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

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
public class GameWebSocketHandler extends TextWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(GameWebSocketHandler.class);
    private final GameFacade gameFacade;
    private final IEventManager eventManager;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final WebSocketSessionManager sessionManager;

    @Autowired
    public GameWebSocketHandler(GameFacade gameFacade, UserRepository userRepository, JwtUtil jwtUtil, WebSocketSessionManager sessionManager) {
        this.gameFacade = gameFacade;
        this.eventManager = gameFacade.getEventManager();
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
        this.sessionManager = sessionManager;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String username = null;

        logger.debug("WebSocket connection attempt from session: {}", session.getId());
        
        // 1. Get username from session attributes (populated by HttpHandshakeInterceptor)
        Object userIdFromAttribute = session.getAttributes().get("userId");
        if (userIdFromAttribute != null) {
            username = userIdFromAttribute.toString();
            logger.info("Authenticated user '{}' via session attribute from handshake interceptor.", username);
        }

        // 2. Fallback for direct connections (no gateway) or if interceptor fails
        if (username == null) {
            logger.warn("Could not find user in session attribute. Falling back to token validation.");
            String token = getJwtTokenFromSession(session);
            if (token == null) {
                logger.error("Authentication failed: No token found in query parameters for session: {}", session.getId());
                session.close(CloseStatus.POLICY_VIOLATION.withReason("Authentication token is required"));
                return;
            }

            try {
                if (!jwtUtil.validateToken(token)) {
                    logger.error("Authentication failed: Invalid token for session: {}", session.getId());
                    session.close(CloseStatus.POLICY_VIOLATION.withReason("Invalid authentication token"));
                    return;
                }
                username = jwtUtil.getUsernameFromToken(token);
                logger.info("Successfully authenticated user '{}' using JWT token from query parameter.", username);
            } catch (Exception e) {
                logger.error("Token validation failed for session: {}", session.getId(), e);
                session.close(CloseStatus.POLICY_VIOLATION.withReason("Token validation error"));
                return;
            }
        }

        if (username == null) {
            logger.error("Unable to determine user for session {}. Closing connection.", session.getId());
            session.close(CloseStatus.POLICY_VIOLATION.withReason("User could not be authenticated"));
            return;
        }

        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            logger.error("User '{}' not found in database. Closing session {}.", username, session.getId());
            session.close(CloseStatus.POLICY_VIOLATION.withReason("User not found"));
            return;
        }

        String playerId = String.valueOf(user.getId());
        sessionManager.registerSession(session, playerId);

        PrintWriter writer = new PrintWriter(new WebSocketWriter(session));
        sessionManager.storePlayerWriter(playerId, writer);

        gameFacade.registerPlayer(playerId);
        eventManager.subscribe(playerId, writer);

        logger.info("WebSocket connection established for player {} (user: {}): session {}", playerId, username, session.getId());
        logger.debug("Debug: Registered session {} for player {} with writer {}", session.getId(), playerId, writer.hashCode());
        logger.debug("About to send SUCCESS:CONNECTED to player {}", playerId);
        
        // Enviar mensagem de conexão e garantir que foi entregue
        try {
            writer.println("SUCCESS:CONNECTED");
            writer.flush();
            logger.debug("SUCCESS:CONNECTED successfully sent to player {}", playerId);
        } catch (Exception e) {
            logger.error("Failed to send SUCCESS:CONNECTED to player {}: {}", playerId, e.getMessage());
            try {
                session.close(CloseStatus.SERVER_ERROR.withReason("Cannot establish communication"));
            } catch (Exception closeEx) {
                logger.error("Failed to close session for player {}: {}", playerId, closeEx.getMessage());
            }
            return;
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        logger.debug("Received WebSocket message: {} from session {}", message.getPayload(), session.getId());
        sessionManager.updateSessionActivity(session.getId().toString());
        String payload = message.getPayload();

        if ("PING".equals(payload)) {
            session.sendMessage(new TextMessage("PONG"));
            return;
        }

        String playerId = sessionManager.getPlayerId(session.getId().toString());
        if (playerId == null) {
            logger.warn("Received message from unknown session: {}", session.getId().toString());
            return;
        }

        logger.debug("Received command from player {}: {} (session: {})", playerId, payload, session.getId());
        String[] command = payload.split(":");

        String[] facadeCommand = new String[command.length + 2];
        facadeCommand[0] = "GAME";
        facadeCommand[1] = playerId;
        System.arraycopy(command, 0, facadeCommand, 2, command.length);

        logger.debug("Processing facade command for player {}: [{}]", playerId, String.join(", ", facadeCommand));
        gameFacade.processGameCommand(facadeCommand);
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

            gameFacade.unregisterPlayer(playerId);
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
        private static final Logger logger = LoggerFactory.getLogger(WebSocketWriter.class);

        public WebSocketWriter(WebSocketSession session) {
            this.session = session;
            logger.debug("WebSocketWriter created for session: {}", session.getId());
        }

        @Override
        public void flush() {
            try {
                logger.debug("WebSocketWriter.flush() called for session: {}, message: {}", session.getId(), this.toString());
                if (session.isOpen()) {
                    String message = this.toString();
                    if (!message.isEmpty()) {
                        logger.debug("Sending message via WebSocket: {} to session: {}", message, session.getId());
                        session.sendMessage(new TextMessage(message));
                        getBuffer().setLength(0);
                    } else {
                        logger.debug("No message to send, buffer is empty");
                    }
                } else {
                    logger.warn("Session is not open, cannot send message: {}", this.toString());
                }
            } catch (IOException e) {
                logger.warn("Error sending message via WebSocket for session {}: {}", session.getId().toString(), e.getMessage());
            }
        }
    }
}
