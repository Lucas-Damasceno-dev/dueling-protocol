package service.auth;

import model.Player;
import model.User;
import org.springframework.beans.factory.annotation.Autowired;
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
    private PlayerRepository playerRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private AuthenticationManager authenticationManager;

    /**
     * Register a new user with username, password, and associate with a player ID
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
     * Authenticate user with username and password, return JWT token if successful
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
     * Validate a JWT token and return the associated username
     */
    public String validateTokenAndGetUsername(String token) {
        if (jwtUtil.validateToken(token)) {
            return jwtUtil.getUsernameFromToken(token);
        }
        return null;
    }

    /**
     * Get user by username
     */
    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    /**
     * Get user by player ID
     */
    public Optional<User> getUserByPlayerId(String playerId) {
        return userRepository.findByPlayerId(playerId);
    }
}