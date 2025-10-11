package websocket;

import controller.GameFacade;
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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Profile("server")
@Component
public class GameWebSocketHandler extends TextWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(GameWebSocketHandler.class);
    private final GameFacade gameFacade;
    private final IEventManager eventManager;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    
    private final Map<String, String> sessionToPlayerId = new ConcurrentHashMap<>();
    private final Map<String, PrintWriter> playerWriters = new ConcurrentHashMap<>();

    @Autowired
    public GameWebSocketHandler(GameFacade gameFacade, UserRepository userRepository, JwtUtil jwtUtil) {
        this.gameFacade = gameFacade;
        this.eventManager = gameFacade.getEventManager();
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // Extract JWT token from the connection parameters
        String token = getJwtTokenFromSession(session);
        if (token == null) {
            logger.error("JWT token not found in WebSocket URI. Closing session {}.", session.getId());
            session.close(CloseStatus.BAD_DATA.withReason("Authentication token is required"));
            return;
        }

        // Validate the JWT token
        if (!jwtUtil.validateToken(token)) {
            logger.error("Invalid JWT token for session {}.", session.getId());
            session.close(CloseStatus.BAD_DATA.withReason("Invalid authentication token"));
            return;
        }

        String username = jwtUtil.getUsernameFromToken(token);
        
        // Find user by username to get their playerId
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            logger.error("User not found for username: {} in session {}.", username, session.getId());
            session.close(CloseStatus.BAD_DATA.withReason("User not found"));
            return;
        }

        String playerId = user.getPlayerId();
        logger.info("WebSocket connection established for player {} (user: {}): session {}", playerId, username, session.getId());
        sessionToPlayerId.put(session.getId(), playerId);

        PrintWriter writer = new PrintWriter(new WebSocketWriter(session));
        playerWriters.put(playerId, writer);

        gameFacade.registerPlayer(playerId);
        eventManager.subscribe(playerId, writer);
        
        // Subscribe to private messages for this player
        if (eventManager instanceof pubsub.RedisEventManager) {
            pubsub.RedisEventManager redisEventManager = (pubsub.RedisEventManager) eventManager;
            redisEventManager.subscribeToPrivateMessages(playerId, privateMessage -> {
                String message = "PRIVATE_MESSAGE:" + privateMessage.getSenderId() + ":" + privateMessage.getContent();
                writer.println(message);
                writer.flush();
            });
            
            // Subscribe to user notifications for this player
            String notificationChannel = "user-notifications:" + playerId;
            redisEventManager.subscribe(notificationChannel, writer);
        }
        
        writer.println("SUCCESS:CONNECTED");
        writer.flush();
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String playerId = sessionToPlayerId.get(session.getId());
        if (playerId == null) {
            logger.warn("Received message from unknown session: {}", session.getId());
            return;
        }

        String payload = message.getPayload();
        logger.debug("Received command from player {}: {}", playerId, payload);
        String[] command = payload.split(":");
        
        String[] facadeCommand = new String[command.length + 2];
        facadeCommand[0] = "GAME";
        facadeCommand[1] = playerId;
        System.arraycopy(command, 0, facadeCommand, 2, command.length);

        gameFacade.processGameCommand(facadeCommand);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String playerId = sessionToPlayerId.remove(session.getId());
        if (playerId != null) {
            logger.info("WebSocket connection closed for player {}: session {} with status {}", playerId, session.getId(), status);
            
            PrintWriter writer = playerWriters.remove(playerId);
            if (writer != null) {
                eventManager.unsubscribe(playerId, writer);
                
                // Unsubscribe from private messages for this player
                if (eventManager instanceof pubsub.RedisEventManager) {
                    pubsub.RedisEventManager redisEventManager = (pubsub.RedisEventManager) eventManager;
                    redisEventManager.unsubscribeFromPrivateMessages(playerId);
                }
                
                // Unsubscribe from user notifications for this player
                if (eventManager instanceof pubsub.RedisEventManager) {
                    pubsub.RedisEventManager redisEventManager = (pubsub.RedisEventManager) eventManager;
                    String notificationChannel = "user-notifications:" + playerId;
                    redisEventManager.unsubscribe(notificationChannel, writer);
                }
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

        public WebSocketWriter(WebSocketSession session) {
            this.session = session;
        }

        @Override
        public void flush() {
            try {
                if (session.isOpen()) {
                    String message = this.toString();
                    if (!message.isEmpty()) {
                        session.sendMessage(new TextMessage(message));
                        getBuffer().setLength(0);
                    }
                }
            } catch (IOException e) {
                // Using a proper logger is better, but System.err is fine for this context
                System.err.println("Error sending message via WebSocket for session " + session.getId() + ": " + e.getMessage());
            }
        }
    }
}
