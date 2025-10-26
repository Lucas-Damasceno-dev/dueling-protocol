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
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import repository.PlayerRepository;
import repository.UserRepository;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.util.Timeout;

@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class FriendControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    private String registerAndLogin(String username, String password, String playerId) {
        // Register the user - since we're using unique data, this should succeed
        RegisterRequest registerRequest = new RegisterRequest(username, password, playerId);
        ResponseEntity<Map> regResponse = restTemplate.postForEntity("/api/auth/register", registerRequest, Map.class);
        
        // Confirm registration was successful
        assertThat(regResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        
        LoginRequest loginRequest = new LoginRequest(username, password);
        ResponseEntity<Map> loginResponse = restTemplate.postForEntity("/api/auth/login", loginRequest, Map.class);

        // Handle potential login failures more gracefully in the helper
        if (loginResponse.getStatusCode() != HttpStatus.OK || loginResponse.getBody() == null || !loginResponse.getBody().containsKey("token")) {
            throw new RuntimeException("Failed to login user '" + username + "' in test setup. Status: " + loginResponse.getStatusCode() + 
                ", Response Body: " + (loginResponse.getBody() != null ? loginResponse.getBody() : "null"));
        }
        return (String) loginResponse.getBody().get("token");
    }

    @Test
    void testSendFriendRequest_Success() {
        // Arrange
        String user1Username = TestDataUtil.generateUniqueUsername("user1");
        String user1Password = TestDataUtil.generateUniquePassword();
        String user1PlayerId = TestDataUtil.generateUniquePlayerId("player1");
        String user2Username = TestDataUtil.generateUniqueUsername("user2");
        String user2Password = TestDataUtil.generateUniquePassword();
        String user2PlayerId = TestDataUtil.generateUniquePlayerId("player2");
        
        String user1Token = registerAndLogin(user1Username, user1Password, user1PlayerId);
        
        // Register the target user (no need to log them in for this part)
        RegisterRequest registerRequest = new RegisterRequest(user2Username, user2Password, user2PlayerId);
        ResponseEntity<Map> regResponse = restTemplate.postForEntity("/api/auth/register", registerRequest, Map.class);
        assertThat(regResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Create Headers with JWT Token
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(user1Token); // <-- Use the token here

        // Create request body
        Map<String, String> requestBody = Map.of("targetUsername", user2Username);

        // Create HttpEntity with body and headers
        HttpEntity<Map<String, String>> request = new HttpEntity<>(requestBody, headers); // <-- Pass headers

        // Act
        ResponseEntity<Map> response = restTemplate.postForEntity("/api/friends/request", request, Map.class); // Expect Map response based on controller

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("message", "Friend request sent successfully");
    }

    @Test
    void testAcceptFriendRequest_Success() {
         // Arrange
         String user3Username = TestDataUtil.generateUniqueUsername("user3");
         String user3Password = TestDataUtil.generateUniquePassword();
         String user3PlayerId = TestDataUtil.generateUniquePlayerId("player3");
         String user4Username = TestDataUtil.generateUniqueUsername("user4");
         String user4Password = TestDataUtil.generateUniquePassword();
         String user4PlayerId = TestDataUtil.generateUniquePlayerId("player4");

         // Register both users
         String user3Token = registerAndLogin(user3Username, user3Password, user3PlayerId);
         String user4Token = registerAndLogin(user4Username, user4Password, user4PlayerId);

         // User3 sends request to User4
         HttpHeaders user3Headers = new HttpHeaders();
         user3Headers.setBearerAuth(user3Token);
         HttpEntity<Map<String, String>> sendRequest = new HttpEntity<>(Map.of("targetUsername", user4Username), user3Headers);
         ResponseEntity<Map> sendResponse = restTemplate.postForEntity("/api/friends/request", sendRequest, Map.class);
         assertThat(sendResponse.getStatusCode()).isEqualTo(HttpStatus.OK); // Verify request was sent

         // User4 accepts request from User3
         HttpHeaders user4Headers = new HttpHeaders();
         user4Headers.setBearerAuth(user4Token); // <-- User4 uses their token
         HttpEntity<Map<String, String>> acceptRequest = new HttpEntity<>(Map.of("senderUsername", user3Username), user4Headers); // <-- Pass headers

         // Act
         ResponseEntity<Map> response = restTemplate.postForEntity("/api/friends/accept", acceptRequest, Map.class); // Expect Map

         // Assert
         assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
         assertThat(response.getBody()).containsEntry("message", "Friend request accepted successfully");
    }
}