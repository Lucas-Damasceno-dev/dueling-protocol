package websocket;

import org.springframework.beans.factory.annotation.Autowired; // <-- Import adicionado
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import controller.GameFacade;
import java.io.PrintWriter;
import java.io.StringWriter;

@Component
public class GameWebSocketHandler extends TextWebSocketHandler {

    private final GameFacade gameFacade;

    @Autowired // <-- CORREÇÃO: Anotação adicionada
    public GameWebSocketHandler(GameFacade gameFacade) {
        this.gameFacade = gameFacade;
    }

    // ... o restante do arquivo permanece o mesmo
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        System.out.println("Nova conexão WebSocket: " + session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        String[] command = payload.split(":");
        
        if (command.length > 1) {
            String playerId = command[1];
            
            PrintWriter writer = new PrintWriter(new WebSocketWriter(session));

            gameFacade.registerClient(playerId, writer);
            
            gameFacade.processGameCommand(command, writer);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        System.out.println("Conexão WebSocket fechada: " + session.getId() + " com status: " + status);
    }

    private static class WebSocketWriter extends StringWriter {
        private final WebSocketSession session;

        public WebSocketWriter(WebSocketSession session) {
            this.session = session;
        }

        @Override
        public void flush() {
            try {
                session.sendMessage(new TextMessage(this.toString()));
                getBuffer().setLength(0); 
            } catch (Exception e) {
                System.err.println("Erro ao enviar mensagem via WebSocket: " + e.getMessage());
            }
        }
    }
}