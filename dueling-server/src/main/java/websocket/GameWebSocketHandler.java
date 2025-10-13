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
        String token = getJwtTokenFromSession(session);
        if (token == null) {
            session.close(CloseStatus.BAD_DATA.withReason("Authentication token is required"));
            return;
        }

        if (!jwtUtil.validateToken(token)) {
            session.close(CloseStatus.BAD_DATA.withReason("Invalid authentication token"));
            return;
        }

        String username = jwtUtil.getUsernameFromToken(token);
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            session.close(CloseStatus.BAD_DATA.withReason("User not found"));
            return;
        }

        String playerId = String.valueOf(user.getId()); // Assumindo que o ID do usuário é o ID do jogador
        sessionManager.registerSession(session, playerId);

        PrintWriter writer = new PrintWriter(new WebSocketWriter(session));
        sessionManager.storePlayerWriter(playerId, writer);

        gameFacade.registerPlayer(playerId);
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
            session.sendMessage(new TextMessage("PONG"));
            return;
        }

        String playerId = sessionManager.getPlayerId(session.getId().toString());
        if (playerId == null) {
            logger.warn("Received message from unknown session: {}", session.getId().toString());
            return;
        }

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
                logger.warn("Error sending message via WebSocket for session {}: {}", session.getId().toString(), e.getMessage());
            }
        }
    }
}