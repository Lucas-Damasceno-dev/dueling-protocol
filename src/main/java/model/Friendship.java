package model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Entity representing a friendship relationship between users.
 * 
 * <p>This class models the relationship between two users in a social network,
 * supporting different states of the relationship such as pending, accepted, etc.</p>
 * 
 * <p>The entity supports bidirectional relationships where one user is the
 * requester (userA) and the other is the recipient (userB).</p>
 * 
 * @author Dueling Protocol Team
 * @since 1.0
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

        /**
     * Checks if the friendship status is ACCEPTED.
     *
     * @return {@code true} if the friendship status is {@link Status#ACCEPTED}, {@code false} otherwise.
     */
    public boolean isFriends() {
        return status == Status.ACCEPTED;
    }

    /**
     * Checks if the friendship involves a specific user.
     *
     * @param userId The unique identifier of the user to check for.
     * @return {@code true} if the specified user is part of this friendship, {@code false} otherwise.
     */
    public boolean isWithUser(String userId) {
        return userAId.equals(userId) || userBId.equals(userId);
    }

    /**
     * Gets the other user in the friendship relationship.
     *
     * @param userId The unique identifier of one user in the relationship.
     * @return The unique identifier of the other user in the friendship, or {@code null} 
     *         if the provided userId is not part of this friendship.
     */
    public String getOtherUser(String userId) {
        if (userAId.equals(userId)) {
            return userBId;
        } else if (userBId.equals(userId)) {
            return userAId;
        }
        return null;
    }
}