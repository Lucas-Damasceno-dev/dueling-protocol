package integration;

import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for test user operations.
 */
public class TestUserHelper {
    
    /**
     * Registers and logs in a user, returning the JWT token.
     * 
     * @param restTemplate The TestRestTemplate instance to use for HTTP requests
     * @param username The username for the new user
     * @param password The password for the new user
     * @param playerId The player ID for the user
     * @return the JWT token obtained after login
     */
    public static String registerAndLoginUser(TestRestTemplate restTemplate, String username, String password, String playerId) {
        // Register the user
        Map<String, String> registrationData = new HashMap<>();
        registrationData.put("username", username);
        registrationData.put("password", password);
        registrationData.put("playerId", playerId);
        
        restTemplate.postForEntity("/api/auth/register", registrationData, Map.class);
        
        // Login the user to get the token
        Map<String, String> loginData = new HashMap<>();
        loginData.put("username", username);
        loginData.put("password", password);
        
        ResponseEntity<Map> loginResponse = restTemplate.postForEntity("/api/auth/login", loginData, Map.class);
        
        return (String) loginResponse.getBody().get("token");
    }
    
    /**
     * Registers a user without logging in.
     * 
     * @param restTemplate The TestRestTemplate instance to use for HTTP requests
     * @param username The username for the new user
     * @param password The password for the new user
     * @param playerId The player ID for the user
     */
    public static void registerUser(TestRestTemplate restTemplate, String username, String password, String playerId) {
        Map<String, String> registrationData = new HashMap<>();
        registrationData.put("username", username);
        registrationData.put("password", password);
        registrationData.put("playerId", playerId);
        
        restTemplate.postForEntity("/api/auth/register", registrationData, Map.class);
    }
    
    /**
     * Logs in a user and returns the JWT token.
     * 
     * @param restTemplate The TestRestTemplate instance to use for HTTP requests
     * @param username The username of the user
     * @param password The password of the user
     * @return the JWT token obtained after login
     */
    public static String loginUser(TestRestTemplate restTemplate, String username, String password) {
        Map<String, String> loginData = new HashMap<>();
        loginData.put("username", username);
        loginData.put("password", password);
        
        ResponseEntity<Map> loginResponse = restTemplate.postForEntity("/api/auth/login", loginData, Map.class);
        
        return (String) loginResponse.getBody().get("token");
    }
}