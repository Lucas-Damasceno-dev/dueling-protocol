package controller;

import dto.LoginRequest;
import dto.RegisterRequest;
import model.Player;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import repository.PlayerRepository;
import service.auth.AuthenticationService;

import jakarta.validation.Valid;
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
    public ResponseEntity<Map<String, String>> register(@Valid @RequestBody RegisterRequest registerRequest) {
        String username = registerRequest.getUsername();
        String password = registerRequest.getPassword();
        String playerId = registerRequest.getPlayerId();

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
    public ResponseEntity<Map<String, String>> login(@Valid @RequestBody LoginRequest loginRequest) {
        String username = loginRequest.getUsername();
        String password = loginRequest.getPassword();

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