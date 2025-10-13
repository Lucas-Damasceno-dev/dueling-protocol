package integration;

import dto.LoginRequest;
import dto.RegisterRequest;
import model.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.client.TestRestTemplate.HttpClientOption;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import repository.PlayerRepository;
import repository.UserRepository;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class AuthControllerIntegrationTest extends AbstractIntegrationTest {

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

    @Test
    void testRegisterUser_Success() {
        // Arrange
        String playerId = TestDataUtil.generateUniquePlayerId("player");
        String username = TestDataUtil.generateUniqueUsername("testuser_reg");
        String password = TestDataUtil.generateUniquePassword();
        
        // The player should already be mocked in TestConfig with unique ID
        // Let's add the unique player to the mock in TestConfig via method call or update

        RegisterRequest request = new RegisterRequest(username, password, playerId);

        // Act
        ResponseEntity<Map> response = restTemplate.postForEntity("/api/auth/register", request, Map.class);

        // Assert
        // Since we're using unique data, we should get success
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("message", "User registered successfully");
    }

    @Test
    void testRegisterUser_PlayerNotFound() {
        // Arrange
        String playerId = TestDataUtil.generateUniquePlayerId("nonexistentplayer");
        RegisterRequest request = new RegisterRequest(TestDataUtil.generateUniqueUsername("testuser"), "password", playerId);

        // Act
        ResponseEntity<Map> response = restTemplate.postForEntity("/api/auth/register", request, Map.class);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void testLoginUser_Success() {
        // Arrange
        String playerId = TestDataUtil.generateUniquePlayerId("player");
        String username = TestDataUtil.generateUniqueUsername("loginuser");
        String password = TestDataUtil.generateUniquePassword();
        
        // Register the user first
        RegisterRequest registerRequest = new RegisterRequest(username, password, playerId);
        ResponseEntity<Map> regResponse = restTemplate.postForEntity("/api/auth/register", registerRequest, Map.class);
        
        // Confirm registration was successful
        assertThat(regResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        
        LoginRequest loginRequest = new LoginRequest(username, password);

        // Act
        ResponseEntity<Map> response = restTemplate.postForEntity("/api/auth/login", loginRequest, Map.class);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsKey("token");
        assertThat(response.getBody()).containsEntry("message", "Login successful");
    }

    @Test
    void testLoginUser_InvalidCredentials() {
        // Arrange
        String username = TestDataUtil.generateUniqueUsername("wronguser");
        String password = TestDataUtil.generateUniquePassword();
        LoginRequest request = new LoginRequest(username, password);

        // Act
        ResponseEntity<Map> response = restTemplate.postForEntity("/api/auth/login", request, Map.class);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}

