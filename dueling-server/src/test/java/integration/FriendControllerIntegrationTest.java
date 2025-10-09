package integration;

import dto.LoginRequest;
import dto.RegisterRequest;
import model.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import repository.PlayerRepository;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.util.Timeout;

@ActiveProfiles("test")
public class FriendControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private PlayerRepository playerRepository;

    @BeforeEach
    void setUp() {
        // Configure the restTemplate with a custom request factory that avoids connection reuse issues
        // This helps prevent authentication retry problems that occur in test environments
        RequestConfig config = RequestConfig.custom()
            .setConnectTimeout(Timeout.ofMilliseconds(5000))
            .setResponseTimeout(Timeout.ofMilliseconds(10000))
            .build();

        CloseableHttpClient httpClient = HttpClients.custom()
            .setDefaultRequestConfig(config)
            .build();

        org.springframework.http.client.HttpComponentsClientHttpRequestFactory requestFactory =
            new org.springframework.http.client.HttpComponentsClientHttpRequestFactory(httpClient);

        restTemplate.getRestTemplate().setRequestFactory(requestFactory);
    }

    private String registerAndLogin(String username, String password, String playerId) {
        playerRepository.save(new Player(playerId, username));
        RegisterRequest registerRequest = new RegisterRequest(username, password, playerId);
        restTemplate.postForEntity("/api/auth/register", registerRequest, Map.class);

        LoginRequest loginRequest = new LoginRequest(username, password);
        ResponseEntity<Map> loginResponse;
        try {
            loginResponse = restTemplate.postForEntity("/api/auth/login", loginRequest, Map.class);
        } catch (org.springframework.web.client.ResourceAccessException e) {
            // Handle specific authentication retry exceptions that can occur in test environments
            if (e.getMessage() != null && e.getMessage().contains("cannot retry due to server authentication")) {
                // This is a known intermittent issue with the HTTP client when dealing with authentication in test environments
                // Sleep and retry once, which often resolves the issue
                try {
                    Thread.sleep(50); // Short delay before retry
                    loginResponse = restTemplate.postForEntity("/api/auth/login", loginRequest, Map.class);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted while waiting to retry authentication", ie);
                } catch (org.springframework.web.client.ResourceAccessException e2) {
                    if (e2.getMessage() != null && e2.getMessage().contains("cannot retry due to server authentication")) {
                        // Still getting the same error after retry, treat as authentication failure
                        throw new RuntimeException("Authentication failed due to HTTP client retry issue", e2);
                    } else {
                        throw e2; // Different error, re-throw
                    }
                }
            } else {
                throw e; // Different error, re-throw
            }
        }
        return (String) loginResponse.getBody().get("token");
    }

    @Test
    void testSendFriendRequest_Success() {
        String user1Token = registerAndLogin("user1_fr", "password", "player1_fr");
        registerAndLogin("user2_fr", "password", "player2_fr");

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(user1Token);
        HttpEntity<Map> request = new HttpEntity<>(Map.of("targetUsername", "user2_fr"), headers);

        ResponseEntity<String> response = restTemplate.postForEntity("/api/friends/request", request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("Friend request sent");
    }

    @Test
    void testAcceptFriendRequest_Success() {
        String user1Token = registerAndLogin("user3", "password", "player3");
        String user2Token = registerAndLogin("user4", "password", "player4");

        HttpHeaders user1Headers = new HttpHeaders();
        user1Headers.setBearerAuth(user1Token);
        HttpEntity<Map> request = new HttpEntity<>(Map.of("targetUsername", "user4"), user1Headers);
        restTemplate.postForEntity("/api/friends/request", request, String.class);

        HttpHeaders user2Headers = new HttpHeaders();
        user2Headers.setBearerAuth(user2Token);
        HttpEntity<Map> acceptRequest = new HttpEntity<>(Map.of("senderUsername", "user3"), user2Headers);

        ResponseEntity<String> response = restTemplate.postForEntity("/api/friends/accept", acceptRequest, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("Friend request accepted");
    }
}