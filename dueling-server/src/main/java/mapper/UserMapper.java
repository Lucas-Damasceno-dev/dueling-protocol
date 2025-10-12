package mapper;

import dto.UserInfo;
import model.User;

/**
 * Mapper class for converting between User entities and UserInfo DTOs.
 */
public class UserMapper {

    /**
     * Converts a User entity to a UserInfo DTO.
     *
     * @param user the User entity to convert
     * @return the corresponding UserInfo DTO, or null if user is null
     */
    public static UserInfo toUserInfo(User user) {
        if (user == null) {
            return null;
        }
        
        UserInfo userInfo = new UserInfo();
        userInfo.setPlayerId(user.getPlayerId());
        userInfo.setUsername(user.getUsername());
        return userInfo;
    }

    /**
     * Converts a UserInfo DTO to a User entity.
     *
     * @param userInfo the UserInfo DTO to convert
     * @return the corresponding User entity, or null if userInfo is null
     */
    public static User toUser(UserInfo userInfo) {
        if (userInfo == null) {
            return null;
        }
        
        User user = new User();
        user.setPlayerId(userInfo.getPlayerId());
        user.setUsername(userInfo.getUsername());
        // Note: Password is not included in UserInfo for security reasons
        return user;
    }
}