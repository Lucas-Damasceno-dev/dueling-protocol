package dto;

/**
 * Data Transfer Object for user information.
 * Contains public information about a user that can be safely exposed.
 */
public class UserInfo {
    private String playerId;
    private String username;

    // Default constructor
    public UserInfo() {
    }

    // Constructor with all fields
    public UserInfo(String playerId, String username) {
        this.playerId = playerId;
        this.username = username;
    }

    // Getters and setters
    public String getPlayerId() {
        return playerId;
    }

    public void setPlayerId(String playerId) {
        this.playerId = playerId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @Override
    public String toString() {
        return "UserInfo{" +
                "playerId='" + playerId + '\'' +
                ", username='" + username + '\'' +
                '}';
    }
}