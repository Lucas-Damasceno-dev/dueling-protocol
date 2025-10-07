package mapper;

import dto.FriendInfo;
import dto.FriendRequestInfo;
import model.User;
import model.Friendship;
import repository.UserRepository;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper class for converting between Friend-related entities and DTOs.
 */
public class FriendMapper {

    /**
     * Converts a User entity to a FriendInfo DTO.
     *
     * @param user the User entity to convert
     * @return the corresponding FriendInfo DTO, or null if user is null
     */
    public static FriendInfo toFriendInfo(User user) {
        if (user == null) {
            return null;
        }
        
        FriendInfo friendInfo = new FriendInfo();
        friendInfo.setPlayerId(user.getPlayerId());
        friendInfo.setUsername(user.getUsername());
        return friendInfo;
    }

    /**
     * Converts a list of User entities to a list of FriendInfo DTOs.
     *
     * @param users the list of User entities to convert
     * @return the corresponding list of FriendInfo DTOs
     */
    public static List<FriendInfo> toFriendInfoList(List<User> users) {
        if (users == null) {
            return null;
        }
        
        return users.stream()
                .map(FriendMapper::toFriendInfo)
                .collect(Collectors.toList());
    }

    /**
     * Creates a FriendRequestInfo DTO from a Friendship entity and UserRepository.
     *
     * @param friendship the Friendship entity
     * @param userRepository the UserRepository to get sender information
     * @return the corresponding FriendRequestInfo DTO
     */
    public static FriendRequestInfo toFriendRequestInfo(Friendship friendship, UserRepository userRepository) {
        if (friendship == null) {
            return null;
        }
        
        User sender = userRepository.findByPlayerId(friendship.getUserAId()).orElse(null);
        
        FriendRequestInfo requestInfo = new FriendRequestInfo();
        requestInfo.setSenderPlayerId(friendship.getUserAId());
        requestInfo.setSenderUsername(sender != null ? sender.getUsername() : "Unknown");
        return requestInfo;
    }
}