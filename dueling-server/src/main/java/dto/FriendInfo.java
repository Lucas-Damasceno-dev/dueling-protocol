package dto;

/**
 * Data Transfer Object for friend information.
 * Contains information about a user's friend that can be safely exposed.
 */
public class FriendInfo {
    private String playerId;
    private String username;

    // Default constructor
    public FriendInfo() {
    }

    // Constructor with all fields
    public FriendInfo(String playerId, String username) {
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
        return "FriendInfo{" +
                "playerId='" + playerId + '\'' +
                ", username='" + username + '\'' +
                '}';
    }
}