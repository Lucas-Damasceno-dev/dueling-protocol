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
    
    @org.springframework.beans.factory.annotation.Value("${websocket.auth.required:true}")
    private boolean authRequired;

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

        logger.info("WebSocket connection attempt from session: {}", session.getId());
        
        // 1. Get username from session attributes (populated by HttpHandshakeInterceptor)
        Object userIdFromAttribute = session.getAttributes().get("userId");
        if (userIdFromAttribute != null) {
            username = userIdFromAttribute.toString();
            logger.info("Authenticated user '{}' via session attribute from handshake interceptor.", username);
        }

        // 2. Fallback for direct connections (no gateway) or if interceptor fails
        if (username == null) {
            logger.info("Could not find user in session attribute. Falling back to token validation.");
            String token = getJwtTokenFromSession(session);
            if (token == null) {
                // Allow anonymous connections if auth is not required (test mode)
                if (!authRequired) {
                    String anonymousId = "anonymous_" + session.getId();
                    logger.warn("Authentication NOT required (test mode). Allowing anonymous connection for session: {}", session.getId());
                    // Store session without authentication - playerId will be extracted from first message
                    sessionManager.registerSession(session, anonymousId);
                    
                    // Setup PrintWriter for anonymous session so it can receive messages
                    PrintWriter writer = new PrintWriter(new WebSocketWriter(session));
                    sessionManager.storePlayerWriter(anonymousId, writer);
                    logger.info("Anonymous session setup complete with PrintWriter");
                    return;
                } else {
                    logger.error("Authentication failed: No token found in query parameters for session: {}", session.getId());
                    session.close(CloseStatus.POLICY_VIOLATION.withReason("Authentication token is required"));
                    return;
                }
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

        String playerId = user.getPlayerId();
        sessionManager.registerSession(session, playerId);

        PrintWriter writer = new PrintWriter(new WebSocketWriter(session));
        sessionManager.storePlayerWriter(playerId, writer);

        System.out.println("[DEBUG] About to call gameFacade.registerPlayer for player: " + playerId);
        gameFacade.registerPlayer(playerId);
        System.out.println("[DEBUG] Called gameFacade.registerPlayer, now subscribing for player: " + playerId);
        eventManager.subscribe(playerId, writer);
        System.out.println("[DEBUG] Subscribed player: " + playerId);

        logger.info("WebSocket connection established for player {} (user: {}): session {}", playerId, username, session.getId());
        logger.info("info: Registered session {} for player {} with writer {}", session.getId(), playerId, writer.hashCode());
        logger.info("About to send SUCCESS:CONNECTED to player {}", playerId);
        
        // Enviar mensagem de conexão e garantir que foi entregue
        try {
            writer.println("SUCCESS:CONNECTED");
            writer.flush();
            logger.info("SUCCESS:CONNECTED successfully sent to player {}", playerId);
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
        logger.info("Received WebSocket message: {} from session {}", message.getPayload(), session.getId());
        sessionManager.updateSessionActivity(session.getId().toString());
        String payload = message.getPayload();

        if ("PING".equals(payload)) {
            try {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage("PONG"));
                }
            } catch (IOException e) {
                logger.info("Failed to send PONG to session {}: {}", session.getId(), e.getMessage());
            }
            return;
        }

        String playerId = sessionManager.getPlayerId(session.getId().toString());
        
        // Handle anonymous sessions (test mode)
        if (playerId != null && playerId.startsWith("anonymous_")) {
            // Extract playerId from message: GAME:playerId:ACTION:...
            String[] messageParts = payload.split(":", 3);
            if (messageParts.length >= 2 && "GAME".equals(messageParts[0])) {
                String extractedPlayerId = messageParts[1];
                logger.info("Anonymous session - extracted playerId from message: {}", extractedPlayerId);
                
                // Get existing writer and transfer to new playerId
                PrintWriter existingWriter = sessionManager.getPlayerWriter(playerId);
                
                // Update session mapping with real playerId
                sessionManager.registerSession(session, extractedPlayerId);
                
                // Transfer writer to real playerId
                if (existingWriter != null) {
                    sessionManager.storePlayerWriter(extractedPlayerId, existingWriter);
                    logger.info("Transferred PrintWriter from {} to {}", playerId, extractedPlayerId);
                }
                
                playerId = extractedPlayerId;
            } else {
                logger.warn("Anonymous session but cannot extract playerId from message: {}", payload);
                return;
            }
        }
        
        if (playerId == null) {
            logger.info("Received message from unknown session: {}", session.getId().toString());
            return;
        }

        logger.info("Received command from player {}: {} (session: {})", playerId, payload, session.getId());
        String[] command = payload.split(":");

        // If message already starts with "GAME:playerId:", just use it as-is
        String[] facadeCommand;
        if (command.length >= 2 && "GAME".equals(command[0]) && playerId.equals(command[1])) {
            facadeCommand = command;
            logger.info("Message already in facade format, using as-is");
        } else {
            // Add GAME and playerId prefix
            facadeCommand = new String[command.length + 2];
            facadeCommand[0] = "GAME";
            facadeCommand[1] = playerId;
            System.arraycopy(command, 0, facadeCommand, 2, command.length);
        }

        logger.info("Processing facade command for player {}: [{}]", playerId, String.join(", ", facadeCommand));
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
            logger.info("WebSocketWriter created for session: {}", session.getId());
        }

        @Override
        public void flush() {
            try {
                logger.info("WebSocketWriter.flush() called for session: {}, message: {}", session.getId(), this.toString());
                if (session.isOpen()) {
                    String message = this.toString();
                    if (!message.isEmpty()) {
                        logger.info("Sending message via WebSocket: {} to session: {}", message, session.getId());
                        synchronized (session) {
                            if (session.isOpen()) {
                                session.sendMessage(new TextMessage(message));
                            }
                        }
                        getBuffer().setLength(0);
                    } else {
                        logger.info("No message to send, buffer is empty");
                    }
                } else {
                    logger.info("Session is not open, cannot send message");
                }
            } catch (IOException e) {
                if (e.getMessage() != null && 
                    (e.getMessage().contains("Broken pipe") || 
                     e.getMessage().contains("Connection reset"))) {
                    logger.info("Connection already closed for session {}", session.getId());
                } else {
                    logger.info("Error sending message via WebSocket for session {}: {}", session.getId().toString(), e.getMessage());
                }
            } catch (IllegalStateException e) {
                logger.info("Session {} already closed", session.getId());
            }
        }
    }
}
