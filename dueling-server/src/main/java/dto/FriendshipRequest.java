package dto;

/**
 * Data Transfer Object for friendship requests.
 * Contains information needed to send a friend request.
 */
public class FriendshipRequest {
    private String targetUsername;

    // Default constructor
    public FriendshipRequest() {
    }

    // Constructor with all fields
    public FriendshipRequest(String targetUsername) {
        this.targetUsername = targetUsername;
    }

    // Getters and setters
    public String getTargetUsername() {
        return targetUsername;
    }

    public void setTargetUsername(String targetUsername) {
        this.targetUsername = targetUsername;
    }

    @Override
    public String toString() {
        return "FriendshipRequest{" +
                "targetUsername='" + targetUsername + '\'' +
                '}';
    }
}