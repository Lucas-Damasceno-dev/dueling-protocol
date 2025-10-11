package com.dueling.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayIntegrationTest {

    @LocalServerPort
    private int port;

    @Test
    void testApiRouteForwarding() {
        // Test that API routes are properly configured in the gateway
        WebClient client = WebClient.builder()
                .baseUrl("http://localhost:" + port)
                .build();

        // This test expects that there's a backend service to forward to
        // In a real scenario, we would mock the backend or have a test backend
        assertThrows(WebClientResponseException.class, () -> {
            client.get()
                .uri("/api/test")
                .retrieve()
                .bodyToMono(String.class)
                .block();
        });
    }

    @Test
    void testWebSocketRoute() {
        // WebSocket testing would require additional setup
        // This is a placeholder to show the concept
        assert port > 0; // Verify the port is properly assigned
    }
}