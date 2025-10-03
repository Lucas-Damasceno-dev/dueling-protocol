package websocket;

import controller.GameFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import pubsub.EventManager;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.context.annotation.Profile;

import org.springframework.context.annotation.Profile;

@Profile("server")
import org.springframework.context.annotation.Profile;

@Profile("server")
@Component
public class GameWebSocketHandler extends TextWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(GameWebSocketHandler.class);
    private final GameFacade gameFacade;
    private final EventManager eventManager;
    
    // Maps to manage sessions and their associated writers
    private final Map<String, String> sessionToPlayerId = new ConcurrentHashMap<>();
    private final Map<String, PrintWriter> playerWriters = new ConcurrentHashMap<>();

    @Autowired
    public GameWebSocketHandler(GameFacade gameFacade) {
        this.gameFacade = gameFacade;
        this.eventManager = gameFacade.getEventManager();
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String playerId = getPlayerIdFromSession(session);
        if (playerId == null) {
            logger.error("PlayerId not found in WebSocket URI. Closing session {}.", session.getId());
            session.close(CloseStatus.BAD_DATA.withReason("PlayerId is required"));
            return;
        }

        logger.info("WebSocket connection established for player {}: session {}", playerId, session.getId());
        sessionToPlayerId.put(session.getId(), playerId);

        // Create a writer for this session and store it
        PrintWriter writer = new PrintWriter(new WebSocketWriter(session));
        playerWriters.put(playerId, writer);

        // Register player in facade and subscribe writer to events
        gameFacade.registerPlayer(playerId);
        eventManager.subscribe(playerId, writer);
        
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
        
        // The facade expects "GAME:playerId:ACTION..." so we prepend the GAME type and playerId
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
            
            // Retrieve the writer to unsubscribe it
            PrintWriter writer = playerWriters.remove(playerId);
            if (writer != null) {
                eventManager.unsubscribe(playerId, writer);
            }

            // Unregister player from the facade (which handles game cleanup)
            gameFacade.unregisterPlayer(playerId);
        }
    }

    private String getPlayerIdFromSession(WebSocketSession session) {
        // Assumes URI is like /ws?playerId=some-id
        String query = session.getUri().getQuery();
        if (query != null) {
            for (String param : query.split("&")) {
                String[] pair = param.split("=");
                if (pair.length == 2 && "playerId".equals(pair[0])) {
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
                logger.error("Error sending message via WebSocket for session {}: {}", session.getId(), e.getMessage());
            }
        }
    }
}
