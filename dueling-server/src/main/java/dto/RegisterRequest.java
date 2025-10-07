package dto;

/**
 * Data Transfer Object for user registration requests.
 * Contains the information needed to register a new user.
 */
public class RegisterRequest {
    private String username;
    private String password;
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