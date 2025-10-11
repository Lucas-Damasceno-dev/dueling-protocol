package integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the friends API endpoints.
 */
@SpringBootTest(
    classes = {controller.DuelingProtocolApplication.class, config.TestConfig.class},
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test") // Use test profile to avoid conflicts with development/production configs
public class FriendControllerIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private String baseUrl() {
        return "http://localhost:" + port + "/api/friends";
    }

    private String authToken1;
    private String authToken2;

    @BeforeEach
    public void setupUsers() {
        // Register two test users
        Map<String, String> userData1 = new HashMap<>();
        userData1.put("username", "user1");
        userData1.put("password", "pass123");
        userData1.put("playerId", "player1");
        
        Map<String, String> userData2 = new HashMap<>();
        userData2.put("username", "user2");
        userData2.put("password", "pass123");
        userData2.put("playerId", "player2");

        // Register users
        restTemplate.postForEntity("/api/auth/register", userData1, Map.class);
        restTemplate.postForEntity("/api/auth/register", userData2, Map.class);

        // Login users to get authentication tokens
        Map<String, String> loginData1 = new HashMap<>();
        loginData1.put("username", "user1");
        loginData1.put("password", "pass123");
        
        Map<String, String> loginData2 = new HashMap<>();
        loginData2.put("username", "user2");
        loginData2.put("password", "pass123");

        ResponseEntity<Map> loginResponse1 = restTemplate.postForEntity("/api/auth/login", loginData1, Map.class);
        ResponseEntity<Map> loginResponse2 = restTemplate.postForEntity("/api/auth/login", loginData2, Map.class);

        authToken1 = (String) loginResponse1.getBody().get("token");
        authToken2 = (String) loginResponse2.getBody().get("token");
    }

    @Test
    public void testSendFriendRequest_Success() {
        // Prepare friend request data
        Map<String, String> requestData = new HashMap<>();
        requestData.put("targetUsername", "user2");

        // Set up authentication header
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(authToken1);
        HttpEntity<Map> requestEntity = new HttpEntity<>(requestData, headers);

        // Send POST request to send friend request
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl() + "/request",
                HttpMethod.POST,
                requestEntity,
                String.class);

        // Verify response
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("Friend request sent successfully");
    }

    @Test
    public void testSendFriendRequest_Unauthorized_NoToken() {
        // Prepare friend request data without authentication
        Map<String, String> requestData = new HashMap<>();
        requestData.put("targetUsername", "user2");

        HttpEntity<Map> requestEntity = new HttpEntity<>(requestData);

        // Send POST request without authentication
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl() + "/request",
                HttpMethod.POST,
                requestEntity,
                String.class);

        // Verify response - should be unauthorized
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    public void testGetFriendRequests_Success() {
        // First send a friend request
        Map<String, String> requestData = new HashMap<>();
        requestData.put("targetUsername", "user2");

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(authToken1);
        HttpEntity<Map> requestEntity = new HttpEntity<>(requestData, headers);

        restTemplate.exchange(baseUrl() + "/request", HttpMethod.POST, requestEntity, String.class);

        // Now get friend requests as user2
        HttpHeaders headers2 = new HttpHeaders();
        headers2.setBearerAuth(authToken2);
        HttpEntity<Void> requestEntity2 = new HttpEntity<>(headers2);

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl() + "/requests",
                HttpMethod.GET,
                requestEntity2,
                Map.class);

        // Verify response
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> responseBody = response.getBody();
        assertThat(responseBody).containsKey("requests");
        assertThat(responseBody).containsKey("count");
    }

    @Test
    public void testAcceptFriendRequest_Success() {
        // First send a friend request
        Map<String, String> requestData = new HashMap<>();
        requestData.put("targetUsername", "user2");

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(authToken1);
        HttpEntity<Map> requestEntity = new HttpEntity<>(requestData, headers);

        restTemplate.exchange(baseUrl() + "/request", HttpMethod.POST, requestEntity, String.class);

        // Now accept the friend request as user2
        Map<String, String> acceptData = new HashMap<>();
        acceptData.put("senderUsername", "user1");

        HttpHeaders headers2 = new HttpHeaders();
        headers2.setBearerAuth(authToken2);
        HttpEntity<Map> acceptEntity = new HttpEntity<>(acceptData, headers2);

        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl() + "/accept",
                HttpMethod.POST,
                acceptEntity,
                String.class);

        // Verify response
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("Friend request accepted successfully");
    }

    @Test
    public void testGetFriendsList_Success() {
        // Get friends list for user1
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(authToken1);
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl(),
                HttpMethod.GET,
                requestEntity,
                Map.class);

        // Verify response
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> responseBody = response.getBody();
        assertThat(responseBody).containsKey("friends");
        assertThat(responseBody).containsKey("count");
    }
}