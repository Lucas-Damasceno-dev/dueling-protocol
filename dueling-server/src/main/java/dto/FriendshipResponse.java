package dto;

import java.util.List;

/**
 * Data Transfer Object for friendship responses.
 * Contains information about a user's friends and friend requests.
 */
public class FriendshipResponse {
    private List<FriendInfo> friends;
    private List<FriendRequestInfo> friendRequests;
    private int count;
    private String message;
    private String error;

    // Default constructor
    public FriendshipResponse() {
    }

    // Constructor for successful responses with friends and friend requests
    public FriendshipResponse(List<FriendInfo> friends, List<FriendRequestInfo> friendRequests, int count) {
        this.friends = friends;
        this.friendRequests = friendRequests;
        this.count = count;
    }

    // Constructor for message responses
    public FriendshipResponse(String message) {
        this.message = message;
    }

    // Constructor for error responses
    public FriendshipResponse(String error, boolean isError) {
        if (isError) {
            this.error = error;
        } else {
            this.message = error;
        }
    }

    // Getters and setters
    public List<FriendInfo> getFriends() {
        return friends;
    }

    public void setFriends(List<FriendInfo> friends) {
        this.friends = friends;
    }

    public List<FriendRequestInfo> getFriendRequests() {
        return friendRequests;
    }

    public void setFriendRequests(List<FriendRequestInfo> friendRequests) {
        this.friendRequests = friendRequests;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    @Override
    public String toString() {
        return "FriendshipResponse{" +
                "friends=" + (friends != null ? friends.size() : 0) + " items" +
                ", friendRequests=" + (friendRequests != null ? friendRequests.size() : 0) + " items" +
                ", count=" + count +
                ", message='" + message + '\'' +
                ", error='" + error + '\'' +
                '}';
    }
}