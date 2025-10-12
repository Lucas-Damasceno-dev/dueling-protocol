package dto;

/**
 * Data Transfer Object for friend request information.
 * Contains information about a pending friend request.
 */
public class FriendRequestInfo {
    private String senderPlayerId;
    private String senderUsername;

    // Default constructor
    public FriendRequestInfo() {
    }

    // Constructor with all fields
    public FriendRequestInfo(String senderPlayerId, String senderUsername) {
        this.senderPlayerId = senderPlayerId;
        this.senderUsername = senderUsername;
    }

    // Getters and setters
    public String getSenderPlayerId() {
        return senderPlayerId;
    }

    public void setSenderPlayerId(String senderPlayerId) {
        this.senderPlayerId = senderPlayerId;
    }

    public String getSenderUsername() {
        return senderUsername;
    }

    public void setSenderUsername(String senderUsername) {
        this.senderUsername = senderUsername;
    }

    @Override
    public String toString() {
        return "FriendRequestInfo{" +
                "senderPlayerId='" + senderPlayerId + '\'' +
                ", senderUsername='" + senderUsername + '\'' +
                '}';
    }
}