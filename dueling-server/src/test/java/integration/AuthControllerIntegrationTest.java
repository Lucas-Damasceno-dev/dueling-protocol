package integration;

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
 * Integration tests for the authentication API endpoints.
 */
@ActiveProfiles("test") // Use test profile to avoid conflicts with development/production configs
public class AuthControllerIntegrationTest extends AbstractIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private String baseUrl() {
        return "http://localhost:" + port + "/api/auth";
    }

    @Test
    public void testRegisterUser_Success() {
        // Prepare registration data
        Map<String, String> registrationData = new HashMap<>();
        registrationData.put("username", "testuser");
        registrationData.put("password", "testpass123");
        registrationData.put("playerId", "player123");

        // Send POST request to register endpoint
        ResponseEntity<Map> response = restTemplate.postForEntity(
                baseUrl() + "/register", 
                registrationData, 
                Map.class);

        // Verify response
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat((String) response.getBody().get("message")).contains("User registered successfully");
    }

    @Test
    public void testRegisterUser_MissingFields_BadRequest() {
        // Prepare incomplete registration data
        Map<String, String> registrationData = new HashMap<>();
        registrationData.put("username", "testuser");
        // Missing password and playerId

        // Send POST request to register endpoint
        ResponseEntity<Map> response = restTemplate.postForEntity(
                baseUrl() + "/register", 
                registrationData, 
                Map.class);

        // Verify response
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat((String) response.getBody().get("error")).contains("Username, password, and playerId are required");
    }

    @Test
    public void testLoginUser_Success() {
        // First register a user using the helper
        TestUserHelper.registerUser(restTemplate, "loginuser", "loginpass123", "player456");

        // Prepare login data
        Map<String, String> loginData = new HashMap<>();
        loginData.put("username", "loginuser");
        loginData.put("password", "loginpass123");

        // Send POST request to login endpoint
        ResponseEntity<Map> response = restTemplate.postForEntity(
                baseUrl() + "/login", 
                loginData, 
                Map.class);

        // Verify response
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsKey("token");
        assertThat((String) response.getBody().get("message")).contains("Login successful");
    }

    @Test
    public void testLoginUser_InvalidCredentials_Unauthorized() {
        // Prepare invalid login data
        Map<String, String> loginData = new HashMap<>();
        loginData.put("username", "nonexistent");
        loginData.put("password", "wrongpass");

        // Send POST request to login endpoint
        ResponseEntity<Map> response = restTemplate.postForEntity(
                baseUrl() + "/login", 
                loginData, 
                Map.class);

        // Verify response
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat((String) response.getBody().get("error")).contains("Invalid username or password");
    }
}