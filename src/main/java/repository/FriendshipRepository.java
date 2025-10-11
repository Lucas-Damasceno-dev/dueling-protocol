package repository;

import model.Friendship;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * JPA repository interface for Friendship entities.
 */
@Repository
public interface FriendshipRepository extends JpaRepository<Friendship, Long> {

    /**
     * Find a friendship between two users regardless of who sent the request
     */
    @Query("SELECT f FROM Friendship f WHERE (f.userAId = :user1Id AND f.userBId = :user2Id) OR (f.userAId = :user2Id AND f.userBId = :user1Id)")
    Optional<Friendship> findByUsers(@Param("user1Id") String user1Id, @Param("user2Id") String user2Id);

    /**
     * Find all friendships for a specific user where the status is ACCEPTED
     */
    @Query("SELECT f FROM Friendship f WHERE (f.userAId = :userId OR f.userBId = :userId) AND f.status = 'ACCEPTED'")
    List<Friendship> findFriendsByUserId(@Param("userId") String userId);

    /**
     * Find all pending friend requests sent by a user
     */
    List<Friendship> findByUserAIdAndStatus(String userAId, model.Friendship.Status status);

    /**
     * Find all pending friend requests received by a user
     */
    List<Friendship> findByUserBIdAndStatus(String userBId, model.Friendship.Status status);

    /**
     * Check if a friendship exists between two users
     */
    @Query("SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END FROM Friendship f WHERE (f.userAId = :user1Id AND f.userBId = :user2Id) OR (f.userAId = :user2Id AND f.userBId = :user1Id)")
    boolean existsBetweenUsers(@Param("user1Id") String user1Id, @Param("user2Id") String user2Id);
}