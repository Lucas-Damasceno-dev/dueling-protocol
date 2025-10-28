package dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Data Transfer Object for user registration requests.
 * Contains the information needed to register a new user.
 */
public class RegisterRequest {
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;
    
    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 100, message = "Password must be between 6 and 100 characters")
    private String password;
    
    // Player ID is optional for registration
    private String playerId;

    // Default constructor
    public RegisterRequest() {
    }

    // Constructor with all fields
    public RegisterRequest(String username, String password, String playerId) {
        this.username = username;
        this.password = password;
        this.playerId = playerId;
    }

    // Getters and setters
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPlayerId() {
        return playerId;
    }

    public void setPlayerId(String playerId) {
        this.playerId = playerId;
    }

    @Override
    public String toString() {
        return "RegisterRequest{" +
                "username='" + username + '\'' +
                ", password='[PROTECTED]'" +
                ", playerId='" + playerId + '\'' +
                '}';
    }
}