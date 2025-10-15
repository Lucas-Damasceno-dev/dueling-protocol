package config;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

@Component
public class HttpHandshakeInterceptor implements HandshakeInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(HttpHandshakeInterceptor.class);

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) throws Exception {
        logger.info("HttpHandshakeInterceptor: Verificando o cabeçalho X-User-Id antes do handshake.");
        
        if (request.getHeaders().containsKey("X-User-Id")) {
            String userId = request.getHeaders().getFirst("X-User-Id");
            logger.info("HttpHandshakeInterceptor: Cabeçalho 'X-User-Id' encontrado: {}. Armazenando nos atributos da sessão.", userId);
            attributes.put("userId", userId);
        } else {
            logger.warn("HttpHandshakeInterceptor: Cabeçalho 'X-User-Id' não encontrado na requisição de handshake.");
        }
        
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler,
                               Exception exception) {
        // Nenhuma implementação necessária após o handshake.
    }
}
