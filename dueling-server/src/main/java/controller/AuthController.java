package controller;

import model.Player;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import repository.PlayerRepository;
import service.auth.AuthenticationService;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Controller for handling authentication operations.
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*") // Adjust as needed for your frontend
public class AuthController {

    @Autowired
    private AuthenticationService authenticationService;

    @Autowired
    private PlayerRepository playerRepository; // To verify player exists

    /**
     * Register a new user account
     */
    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@RequestBody Map<String, String> credentials) {
        String username = credentials.get("username");
        String password = credentials.get("password");
        String playerId = credentials.get("playerId");

        // Validate input
        if (username == null || password == null || playerId == null) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "Username, password, and playerId are required");
            return ResponseEntity.badRequest().body(response);
        }

        // Check if the player exists in the system
        Optional<Player> playerOpt = playerRepository.findById(playerId);
        if (playerOpt.isEmpty()) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "Player ID does not exist in the system");
            return ResponseEntity.badRequest().body(response);
        }

        boolean success = authenticationService.registerUser(username, password, playerId);
        if (success) {
            Map<String, String> response = new HashMap<>();
            response.put("message", "User registered successfully");
            return ResponseEntity.ok(response);
        } else {
            Map<String, String> response = new HashMap<>();
            response.put("error", "Username or Player ID already exists");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }
    }

    /**
     * Authenticate user and return JWT token
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody Map<String, String> credentials) {
        String username = credentials.get("username");
        String password = credentials.get("password");

        // Validate input
        if (username == null || password == null) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "Username and password are required");
            return ResponseEntity.badRequest().body(response);
        }

        String token = authenticationService.authenticateUser(username, password);
        if (token != null) {
            Map<String, String> response = new HashMap<>();
            response.put("token", token);
            response.put("message", "Login successful");
            return ResponseEntity.ok(response);
        } else {
            Map<String, String> response = new HashMap<>();
            response.put("error", "Invalid username or password");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
    }
}