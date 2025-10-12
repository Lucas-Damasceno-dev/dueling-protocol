package integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
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
@ActiveProfiles("test") // Use test profile to avoid conflicts with development/production configs
public class FriendControllerIntegrationTest extends AbstractIntegrationTest {

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
        // Use the TestUserHelper to register and login users
        authToken1 = TestUserHelper.registerAndLoginUser(restTemplate, "user1", "pass123", "player1");
        authToken2 = TestUserHelper.registerAndLoginUser(restTemplate, "user2", "pass123", "player2");
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