package model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Entity representing a friendship relationship between users.
 */
@Entity
@Table(name = "friendships")
public class Friendship {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_a_id", nullable = false)
    private String userAId; // The user who sent the friend request

    @Column(name = "user_b_id", nullable = false)
    private String userBId; // The user who received the friend request

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private Status status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum Status {
        PENDING,    // Request sent, waiting for response
        ACCEPTED,   // Request accepted
        REJECTED,   // Request rejected
        BLOCKED     // One user blocked the other
    }

    // Default constructor required by JPA
    public Friendship() {
    }

    public Friendship(String userAId, String userBId, Status status) {
        this.userAId = userAId;
        this.userBId = userBId;
        this.status = status;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUserAId() {
        return userAId;
    }

    public void setUserAId(String userAId) {
        this.userAId = userAId;
    }

    public String getUserBId() {
        return userBId;
    }

    public void setUserBId(String userBId) {
        this.userBId = userBId;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
        this.updatedAt = LocalDateTime.now();
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    // Helper methods to check if users are friends
    public boolean isFriends() {
        return status == Status.ACCEPTED;
    }

    // Check if friendship is with a specific user
    public boolean isWithUser(String userId) {
        return userAId.equals(userId) || userBId.equals(userId);
    }

    // Get the other user in the friendship
    public String getOtherUser(String userId) {
        if (userAId.equals(userId)) {
            return userBId;
        } else if (userBId.equals(userId)) {
            return userAId;
        }
        return null;
    }
}