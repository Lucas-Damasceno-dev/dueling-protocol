package controller;

import dto.AuthenticationResponse;
import dto.ErrorResponse;
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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Controller for handling authentication operations.
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*") // Adjust as needed for your frontend
@Tag(name = "Autenticação", description = "Endpoints para registro e login de usuários") // Agrupa endpoints na UI
public class AuthController {

    @Autowired
    private AuthenticationService authenticationService;

    @Autowired
    private PlayerRepository playerRepository; // To verify player exists

    @Operation(
        summary = "Registra um novo usuário",
        description = "Cria uma nova conta de usuário associada a um Player ID existente. O username deve ser único."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Usuário registrado com sucesso",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = AuthenticationResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "Dados de requisição inválidos (ex: campos faltando)",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "404", 
            description = "Player ID não encontrado no sistema",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "409", 
            description = "Username ou Player ID já em uso",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        )
    })
    @PostMapping("/register")
    public ResponseEntity<AuthenticationResponse> register(@Valid @RequestBody RegisterRequest registerRequest) {
        String username = registerRequest.getUsername();
        String password = registerRequest.getPassword();
        String playerId = registerRequest.getPlayerId();

        // Check if the player exists in the system
        Optional<Player> playerOpt = playerRepository.findById(playerId);
        if (playerOpt.isEmpty()) {
            ErrorResponse response = new ErrorResponse("Player ID does not exist in the system", "PLAYER_NOT_FOUND");
            return ResponseEntity.badRequest().body(new AuthenticationResponse(response.getError()));
        }

        boolean success = authenticationService.registerUser(username, password, playerId);
        if (success) {
            AuthenticationResponse response = new AuthenticationResponse("User registered successfully");
            return ResponseEntity.ok(response);
        } else {
            ErrorResponse response = new ErrorResponse("Username or Player ID already exists", "CONFLICT");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new AuthenticationResponse(response.getError(), true));
        }
    }

    @Operation(
        summary = "Autentica um usuário",
        description = "Valida as credenciais do usuário e retorna um token JWT para ser usado em requisições futuras."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Login bem-sucedido, retorna o token JWT",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = AuthenticationResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "401", 
            description = "Credenciais inválidas",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        )
    })
    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        String username = loginRequest.getUsername();
        String password = loginRequest.getPassword();

        String token = authenticationService.authenticateUser(username, password);
        if (token != null) {
            AuthenticationResponse response = new AuthenticationResponse(token, "Login successful");
            return ResponseEntity.ok(response);
        } else {
            ErrorResponse response = new ErrorResponse("Invalid username or password", "UNAUTHORIZED");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new AuthenticationResponse(response.getError(), true));
        }
    }
}