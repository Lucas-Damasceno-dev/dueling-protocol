package controller;

import model.Friendship;
import model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import pubsub.IEventManager;
import repository.FriendshipRepository;
import repository.UserRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Controller for handling friend-related operations.
 */
@RestController
@RequestMapping("/api/friends")
@CrossOrigin(origins = "*") // Adjust as needed for your frontend
public class FriendController {

    @Autowired
    private FriendshipRepository friendshipRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private IEventManager eventManager;

    /**
     * Send a friend request to another user
     */
    @PostMapping("/request")
    public ResponseEntity<Map<String, String>> sendFriendRequest(@RequestBody Map<String, String> request) {
        String authenticatedUsername = getCurrentUsername();
        if (authenticatedUsername == null) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "User not authenticated");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        String targetUsername = request.get("targetUsername");
        if (targetUsername == null || targetUsername.equals(authenticatedUsername)) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "Invalid target username");
            return ResponseEntity.badRequest().body(response);
        }

        // Get users by username
        User sender = userRepository.findByUsername(authenticatedUsername).orElse(null);
        User target = userRepository.findByUsername(targetUsername).orElse(null);

        if (sender == null || target == null) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "User not found");
            return ResponseEntity.badRequest().body(response);
        }

        // Check if friendship already exists
        if (friendshipRepository.existsBetweenUsers(sender.getPlayerId(), target.getPlayerId())) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "Friendship already exists or request is pending");
            return ResponseEntity.badRequest().body(response);
        }

        // Create new friendship request (PENDING status)
        Friendship friendship = new Friendship(sender.getPlayerId(), target.getPlayerId(), Friendship.Status.PENDING);
        friendshipRepository.save(friendship);

        // Send real-time notification to the target user
        String notificationChannel = "user-notifications:" + target.getPlayerId();
        String notification = "FRIEND_REQUEST:" + sender.getPlayerId() + ":" + sender.getUsername();
        eventManager.publish(notificationChannel, notification);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Friend request sent successfully");
        return ResponseEntity.ok(response);
    }

    /**
     * Accept a friend request
     */
    @PostMapping("/accept")
    public ResponseEntity<Map<String, String>> acceptFriendRequest(@RequestBody Map<String, String> request) {
        String authenticatedUsername = getCurrentUsername();
        if (authenticatedUsername == null) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "User not authenticated");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        String senderUsername = request.get("senderUsername");
        if (senderUsername == null) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "Sender username is required");
            return ResponseEntity.badRequest().body(response);
        }

        // Get users by username
        User receiver = userRepository.findByUsername(authenticatedUsername).orElse(null);
        User sender = userRepository.findByUsername(senderUsername).orElse(null);

        if (receiver == null || sender == null) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "User not found");
            return ResponseEntity.badRequest().body(response);
        }

        // Find the pending friendship request
        Friendship friendship = friendshipRepository.findByUsers(sender.getPlayerId(), receiver.getPlayerId())
                .orElse(null);

        if (friendship == null || friendship.getStatus() != Friendship.Status.PENDING) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "No pending friend request found");
            return ResponseEntity.badRequest().body(response);
        }

        // Ensure that the authenticated user is the one who received the request
        if (!friendship.getUserBId().equals(receiver.getPlayerId())) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "You cannot accept a request you sent");
            return ResponseEntity.badRequest().body(response);
        }

        // Update the friendship status to ACCEPTED
        friendship.setStatus(Friendship.Status.ACCEPTED);
        friendshipRepository.save(friendship);

        // Send real-time notification to the sender that the request was accepted
        String notificationChannel = "user-notifications:" + sender.getPlayerId();
        String notification = "FRIEND_REQUEST_ACCEPTED:" + receiver.getPlayerId() + ":" + receiver.getUsername();
        eventManager.publish(notificationChannel, notification);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Friend request accepted successfully");
        return ResponseEntity.ok(response);
    }

    /**
     * Reject a friend request
     */
    @PostMapping("/reject")
    public ResponseEntity<Map<String, String>> rejectFriendRequest(@RequestBody Map<String, String> request) {
        String authenticatedUsername = getCurrentUsername();
        if (authenticatedUsername == null) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "User not authenticated");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        String senderUsername = request.get("senderUsername");
        if (senderUsername == null) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "Sender username is required");
            return ResponseEntity.badRequest().body(response);
        }

        // Get users by username
        User receiver = userRepository.findByUsername(authenticatedUsername).orElse(null);
        User sender = userRepository.findByUsername(senderUsername).orElse(null);

        if (receiver == null || sender == null) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "User not found");
            return ResponseEntity.badRequest().body(response);
        }

        // Find the pending friendship request
        Friendship friendship = friendshipRepository.findByUsers(sender.getPlayerId(), receiver.getPlayerId())
                .orElse(null);

        if (friendship == null || friendship.getStatus() != Friendship.Status.PENDING) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "No pending friend request found");
            return ResponseEntity.badRequest().body(response);
        }

        // Ensure that the authenticated user is the one who received the request
        if (!friendship.getUserBId().equals(receiver.getPlayerId())) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "You cannot reject a request you sent");
            return ResponseEntity.badRequest().body(response);
        }

        // Update the friendship status to REJECTED
        friendship.setStatus(Friendship.Status.REJECTED);
        friendshipRepository.save(friendship);

        // Send real-time notification to the sender that the request was rejected
        String notificationChannel = "user-notifications:" + sender.getPlayerId();
        String notification = "FRIEND_REQUEST_REJECTED:" + receiver.getPlayerId() + ":" + receiver.getUsername();
        eventManager.publish(notificationChannel, notification);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Friend request rejected successfully");
        return ResponseEntity.ok(response);
    }

    /**
     * Get list of friends for the authenticated user
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getFriends() {
        String authenticatedUsername = getCurrentUsername();
        if (authenticatedUsername == null) {
            Map<String, Object> response = new HashMap<>();
            response.put("error", "User not authenticated");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        User currentUser = userRepository.findByUsername(authenticatedUsername).orElse(null);
        if (currentUser == null) {
            Map<String, Object> response = new HashMap<>();
            response.put("error", "User not found");
            return ResponseEntity.badRequest().body(response);
        }

        // Get all accepted friendships for the user
        List<Friendship> friendships = friendshipRepository.findFriendsByUserId(currentUser.getPlayerId());

        // Get the friend User objects
        List<Map<String, String>> friends = friendships.stream().map(friendship -> {
            String friendPlayerId = friendship.getOtherUser(currentUser.getPlayerId());
            User friend = userRepository.findByPlayerId(friendPlayerId).orElse(null);
            
            Map<String, String> friendInfo = new HashMap<>();
            friendInfo.put("playerId", friendPlayerId);
            friendInfo.put("username", friend != null ? friend.getUsername() : "Unknown");
            return friendInfo;
        }).collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("friends", friends);
        response.put("count", friends.size());
        return ResponseEntity.ok(response);
    }

    /**
     * Get pending friend requests received by the authenticated user
     */
    @GetMapping("/requests")
    public ResponseEntity<Map<String, Object>> getFriendRequests() {
        String authenticatedUsername = getCurrentUsername();
        if (authenticatedUsername == null) {
            Map<String, Object> response = new HashMap<>();
            response.put("error", "User not authenticated");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        User currentUser = userRepository.findByUsername(authenticatedUsername).orElse(null);
        if (currentUser == null) {
            Map<String, Object> response = new HashMap<>();
            response.put("error", "User not found");
            return ResponseEntity.badRequest().body(response);
        }

        // Get all pending friend requests sent to the current user
        List<Friendship> pendingRequests = friendshipRepository.findByUserBIdAndStatus(
                currentUser.getPlayerId(), Friendship.Status.PENDING);

        // Get the sender User objects
        List<Map<String, String>> requests = pendingRequests.stream().map(friendship -> {
            User sender = userRepository.findByPlayerId(friendship.getUserAId()).orElse(null);
            
            Map<String, String> requestInfo = new HashMap<>();
            requestInfo.put("senderPlayerId", friendship.getUserAId());
            requestInfo.put("senderUsername", sender != null ? sender.getUsername() : "Unknown");
            return requestInfo;
        }).collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("requests", requests);
        response.put("count", requests.size());
        return ResponseEntity.ok(response);
    }

    /**
     * Helper method to get the currently authenticated username
     */
    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && 
            !"anonymousUser".equals(authentication.getName())) {
            return authentication.getName();
        }
        return null;
    }
}