package service.auth;


import model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import repository.PlayerRepository;
import repository.UserRepository;
import security.JwtUtil;

import java.util.Optional;

/**
 * Service class for handling authentication operations.
 */
@Service
public class AuthenticationService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    @Qualifier("playerRepositoryImpl")
    private PlayerRepository playerRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private AuthenticationManager authenticationManager;

    /**
     * Registers a new user in the system.
     * This method checks for existing usernames and player IDs before creating a new user.
     * The password is encoded before being stored.
     *
     * @param username The desired username for the new user.
     * @param password The plain-text password for the new user.
     * @param playerId The unique identifier of the player associated with this user account.
     * @return {@code true} if the user was successfully registered, {@code false} otherwise
     *         (e.g., username or player ID already exists, or player does not exist).
     */
    public boolean registerUser(String username, String password, String playerId) {
        // Check if user already exists
        if (userRepository.existsByUsername(username)) {
            return false;
        }

        // Check if playerId already has a user account
        if (userRepository.existsByPlayerId(playerId)) {
            return false;
        }

        // Verify that the player exists
        if (playerRepository.findById(playerId).isEmpty()) {
            return false;
        }

        // Create and save the user
        User user = new User(username, passwordEncoder.encode(password), playerId);
        userRepository.save(user);
        return true;
    }

    /**
     * Authenticates a user with the provided username and password.
     * If authentication is successful, a JSON Web Token (JWT) is generated and returned.
     *
     * @param username The username of the user attempting to authenticate.
     * @param password The plain-text password of the user.
     * @return A JWT token string if authentication is successful, otherwise {@code null}.
     */
    public String authenticateUser(String username, String password) {
        try {
            // Authenticate the user
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password)
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Generate JWT token
            return jwtUtil.generateToken(username);
        } catch (Exception e) {
            // Authentication failed
            return null;
        }
    }

    /**
     * Validates a given JWT token and extracts the username from it.
     *
     * @param token The JWT token string to validate.
     * @return The username associated with the token if valid, otherwise {@code null}.
     */
    public String validateTokenAndGetUsername(String token) {
        if (jwtUtil.validateToken(token)) {
            return jwtUtil.getUsernameFromToken(token);
        }
        return null;
    }

    /**
     * Retrieves a user by their username.
     *
     * @param username The username of the user to retrieve.
     * @return An {@link Optional} containing the {@link User} if found, otherwise empty.
     */
    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    /**
     * Retrieves a user by their associated player ID.
     *
     * @param playerId The unique identifier of the player.
     * @return An {@link Optional} containing the {@link User} if found, otherwise empty.
     */
    public Optional<User> getUserByPlayerId(String playerId) {
        return userRepository.findByPlayerId(playerId);
    }
}