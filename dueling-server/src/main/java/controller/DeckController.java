package controller;

import dto.DeckDTO;
import dto.ErrorResponse;
import jakarta.validation.Valid;

import model.Deck;
import model.Player;
import repository.DeckRepository;
import repository.PlayerRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Controller for handling deck-related REST API requests.
 * Manages the creation, retrieval, update, and deletion of decks.
 */
@RestController
@RequestMapping("/api/deck")
@Tag(name = "Decks", description = "Endpoints for managing player decks")
public class DeckController {

    @Autowired
    private DeckRepository deckRepository;

    @Autowired
    private PlayerRepository playerRepository;

    @Operation(
        summary = "Create a new deck for a player",
        description = "Creates a new deck for a player with the provided deck data."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Deck created successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = DeckDTO.class)
            )
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "Bad request - Validation error in deck data",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "404", 
            description = "Not found - Player with the specified ID does not exist",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "409", 
            description = "Conflict - A deck with the same name already exists for this player",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "500", 
            description = "Internal server error - Error creating deck",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        )
    })
    @PostMapping("/create/{playerId}")
    public ResponseEntity<?> createDeck(@PathVariable String playerId, @Valid @RequestBody DeckDTO deckDTO) {
        try {
            // Validate if player exists
            Optional<Player> playerOpt = playerRepository.findById(playerId);
            if (!playerOpt.isPresent()) {
                ErrorResponse errorResponse = new ErrorResponse("Player with ID " + playerId + " not found", "PLAYER_NOT_FOUND");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            }

            Player player = playerOpt.get();

            // Check if a deck with this name already exists for the player
            if (deckRepository.existsByNameAndPlayerId(deckDTO.getName(), playerId)) {
                ErrorResponse errorResponse = new ErrorResponse("A deck with name '" + deckDTO.getName() + "' already exists for this player", "CONFLICT");
                return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
            }

            // Create a new deck
            Deck deck = new Deck();
            deck.setId(java.util.UUID.randomUUID().toString());
            deck.setName(deckDTO.getName());
            deck.setDescription(deckDTO.getDescription());
            deck.setPlayer(player);
            deck.setCards(deckDTO.getCards());
            deck.setDefault(deckDTO.isDefault());

            // If this is the first deck for the player, make it the default
            if (deckRepository.countByPlayerId(playerId) == 0) {
                deck.setDefault(true);
            }

            // Save the deck
            Deck savedDeck = deckRepository.save(deck);
            
            // Convert back to DTO and return
            DeckDTO savedDeckDTO = convertToDTO(savedDeck);
            return ResponseEntity.ok(savedDeckDTO);
        } catch (Exception e) {
            ErrorResponse errorResponse = new ErrorResponse("Error creating deck: " + e.getMessage(), "INTERNAL_ERROR");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @Operation(
        summary = "Get all decks for a specific player",
        description = "Retrieves all decks belonging to the specified player."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Decks retrieved successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = DeckDTO.class)
            )
        ),
        @ApiResponse(
            responseCode = "500", 
            description = "Internal server error - Error retrieving decks",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        )
    })
    @GetMapping("/player/{playerId}")
    public ResponseEntity<?> getPlayerDecks(@PathVariable String playerId) {
        try {
            List<Deck> decks = deckRepository.findByPlayerId(playerId);
            List<DeckDTO> deckDTOs = decks.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
            return ResponseEntity.ok(deckDTOs);
        } catch (Exception e) {
            ErrorResponse errorResponse = new ErrorResponse("Error retrieving decks: " + e.getMessage(), "INTERNAL_ERROR");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @Operation(
        summary = "Get a specific deck by its ID",
        description = "Retrieves a specific deck by its unique identifier."
    )
    @ApiResponse(
        responseCode = "200",
        description = "Deck retrieved successfully",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(
                example = "{\"id\": \"deck123\", \"name\": \"My Deck\", \"description\": \"A powerful deck\", \"playerId\": \"player123\", \"cards\": [], \"default\": false}"
            )
        )
    )
    @ApiResponse(
        responseCode = "404",
        description = "Not found - Deck with the specified ID does not exist",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(
                example = "{\"error\": \"Deck with ID deck123 not found\"}"
            )
        )
    )
    @ApiResponse(
        responseCode = "500",
        description = "Internal server error - Error retrieving deck",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(
                example = "{\"error\": \"Error retrieving deck: Database connection failed\"}"
            )
        )
    )
    @GetMapping("/{deckId}")
    public ResponseEntity<?> getDeck(@PathVariable String deckId) {
        try {
            Optional<Deck> deckOpt = deckRepository.findById(deckId);
            if (!deckOpt.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Deck with ID " + deckId + " not found");
            }

            DeckDTO deckDTO = convertToDTO(deckOpt.get());
            return ResponseEntity.ok(deckDTO);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(
        summary = "Update an existing deck",
        description = "Updates an existing deck with the provided deck data."
    )
    @ApiResponse(
        responseCode = "200",
        description = "Deck updated successfully",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(
                example = "{\"id\": \"deck123\", \"name\": \"Updated Deck\", \"description\": \"An even more powerful deck\", \"playerId\": \"player123\", \"cards\": [], \"default\": false}"
            )
        )
    )
    @ApiResponse(
        responseCode = "400",
        description = "Bad request - Validation error in deck data",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(
                example = "{\"error\": \"Validation failed: Name is required\"}"
            )
        )
    )
    @ApiResponse(
        responseCode = "403",
        description = "Forbidden - User is not authorized to update this deck",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(
                example = "{\"error\": \"You are not authorized to update this deck\"}"
            )
        )
    )
    @ApiResponse(
        responseCode = "404",
        description = "Not found - Deck with the specified ID does not exist",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(
                example = "{\"error\": \"Deck with ID deck123 not found\"}"
            )
        )
    )
    @ApiResponse(
        responseCode = "409",
        description = "Conflict - A deck with the same name already exists for this player",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(
                example = "{\"error\": \"A deck with name 'Updated Deck' already exists for this player\"}"
            )
        )
    )
    @ApiResponse(
        responseCode = "500",
        description = "Internal server error - Error updating deck",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(
                example = "{\"error\": \"Error updating deck: Database connection failed\"}"
            )
        )
    )
    @PutMapping("/{deckId}")
    public ResponseEntity<?> updateDeck(@PathVariable String deckId, @Valid @RequestBody DeckDTO deckDTO) {
        try {
            Optional<Deck> existingDeckOpt = deckRepository.findById(deckId);
            if (!existingDeckOpt.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Deck with ID " + deckId + " not found");
            }

            Deck existingDeck = existingDeckOpt.get();

            // Check if player is authorized to update this deck
            if (!existingDeck.getPlayer().getId().equals(deckDTO.getPlayerId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("You are not authorized to update this deck");
            }

            // Check if a deck with the new name already exists for the player (excluding the current deck)
            if (!existingDeck.getName().equals(deckDTO.getName()) &&
                deckRepository.existsByNameAndPlayerId(deckDTO.getName(), deckDTO.getPlayerId())) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("A deck with name '" + deckDTO.getName() + "' already exists for this player");
            }

            // Update deck properties
            existingDeck.setName(deckDTO.getName());
            existingDeck.setDescription(deckDTO.getDescription());
            existingDeck.setCards(deckDTO.getCards());
            
            // Only update isDefault if explicitly set to true or if removing from default
            if (deckDTO.isDefault() != existingDeck.isDefault()) {
                existingDeck.setDefault(deckDTO.isDefault());
                
                // If setting this as default, unset other decks for this player
                if (deckDTO.isDefault()) {
                    deckRepository.setDefaultDeck(deckId, deckDTO.getPlayerId());
                }
            }

            // Save updated deck
            Deck updatedDeck = deckRepository.save(existingDeck);
            DeckDTO updatedDeckDTO = convertToDTO(updatedDeck);
            return ResponseEntity.ok(updatedDeckDTO);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error updating deck: " + e.getMessage());
        }
    }

    @Operation(
        summary = "Delete a specific deck",
        description = "Deletes a specific deck by its ID. The player must be authorized to delete the deck."
    )
    @ApiResponse(
        responseCode = "200",
        description = "Deck deleted successfully",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(
                example = "{\"message\": \"Deck deleted successfully\"}"
            )
        )
    )
    @ApiResponse(
        responseCode = "404",
        description = "Not found - Deck with the specified ID does not exist for the player",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(
                example = "{\"error\": \"Deck with ID deck123 not found for player player123\"}"
            )
        )
    )
    @ApiResponse(
        responseCode = "500",
        description = "Internal server error - Error deleting deck",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(
                example = "{\"error\": \"Error deleting deck: Database connection failed\"}"
            )
        )
    )
    @DeleteMapping("/{deckId}")
    public ResponseEntity<?> deleteDeck(@PathVariable String deckId, @RequestParam String playerId) {
        try {
            Optional<Deck> deckOpt = deckRepository.findByIdAndPlayerId(deckId, playerId);
            if (!deckOpt.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Deck with ID " + deckId + " not found for player " + playerId);
            }

            deckRepository.deleteById(deckId);
            return ResponseEntity.ok("Deck deleted successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error deleting deck: " + e.getMessage());
        }
    }

    /**
     * Set a specific deck as the default for a player.
     *
     * @param deckId The ID of the deck to set as default
     * @param playerId The ID of the player
     * @return ResponseEntity indicating success or an error message
     */
    @PutMapping("/set-default/{deckId}")
    public ResponseEntity<?> setDefaultDeck(@PathVariable String deckId, @RequestParam String playerId) {
        try {
            // Verify that the deck belongs to the player
            Optional<Deck> deckOpt = deckRepository.findByIdAndPlayerId(deckId, playerId);
            if (!deckOpt.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Deck with ID " + deckId + " not found for player " + playerId);
            }

            // Set the deck as default (this will unset other decks as default)
            deckRepository.setDefaultDeck(deckId, playerId);

            // Return the updated deck
            Deck updatedDeck = deckRepository.findById(deckId).get();
            DeckDTO updatedDeckDTO = convertToDTO(updatedDeck);
            return ResponseEntity.ok(updatedDeckDTO);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error setting default deck: " + e.getMessage());
        }
    }

    /**
     * Get the default deck for a player.
     *
     * @param playerId The ID of the player whose default deck to retrieve
     * @return ResponseEntity containing the default deck or an error message
     */
    @GetMapping("/default/{playerId}")
    public ResponseEntity<?> getDefaultDeck(@PathVariable String playerId) {
        try {
            Optional<Deck> deckOpt = deckRepository.findByPlayerIdAndIsDefaultTrue(playerId);
            if (!deckOpt.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("No default deck found for player " + playerId);
            }

            DeckDTO deckDTO = convertToDTO(deckOpt.get());
            return ResponseEntity.ok(deckDTO);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Helper method to convert a Deck entity to a DeckDTO.
     *
     * @param deck The deck entity to convert
     * @return The corresponding DeckDTO
     */
    private DeckDTO convertToDTO(Deck deck) {
        if (deck == null) {
            return null;
        }

        return new DeckDTO(
            deck.getId(),
            deck.getName(),
            deck.getDescription(),
            deck.getPlayer().getId(),
            deck.getCards(),
            deck.isDefault()
        );
    }
}