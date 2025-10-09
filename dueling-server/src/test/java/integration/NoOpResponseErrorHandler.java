package integration;

import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.ResponseErrorHandler;

import java.io.IOException;

public class NoOpResponseErrorHandler implements ResponseErrorHandler {
    @Override
    public boolean hasError(ClientHttpResponse response) throws IOException {
        // Não considerar nenhuma resposta como erro, evitando problemas de retry
        return false;
    }

    @Override
    public void handleError(ClientHttpResponse response) throws IOException {
        // Não fazer nada, evitando interferência no mecanismo de retry do HTTP
    }
}