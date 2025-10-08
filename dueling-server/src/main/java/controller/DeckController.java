package controller;

import dto.DeckDTO;
import jakarta.validation.Valid;
import model.Card;
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

/**
 * Controller for handling deck-related REST API requests.
 * Manages the creation, retrieval, update, and deletion of decks.
 */
@RestController
@RequestMapping("/api/deck")
public class DeckController {

    @Autowired
    private DeckRepository deckRepository;

    @Autowired
    private PlayerRepository playerRepository;

    /**
     * Create a new deck for a player.
     *
     * @param playerId The ID of the player creating the deck
     * @param deckDTO The deck data to create
     * @return ResponseEntity containing the created deck or an error message
     */
    @PostMapping("/create/{playerId}")
    public ResponseEntity<?> createDeck(@PathVariable String playerId, @Valid @RequestBody DeckDTO deckDTO) {
        try {
            // Validate if player exists
            Optional<Player> playerOpt = playerRepository.findById(playerId);
            if (!playerOpt.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Player with ID " + playerId + " not found");
            }

            Player player = playerOpt.get();

            // Check if a deck with this name already exists for the player
            if (deckRepository.existsByNameAndPlayerId(deckDTO.getName(), playerId)) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("A deck with name '" + deckDTO.getName() + "' already exists for this player");
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
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error creating deck: " + e.getMessage());
        }
    }

    /**
     * Get all decks for a specific player.
     *
     * @param playerId The ID of the player whose decks to retrieve
     * @return ResponseEntity containing a list of the player's decks
     */
    @GetMapping("/player/{playerId}")
    public ResponseEntity<List<DeckDTO>> getPlayerDecks(@PathVariable String playerId) {
        try {
            List<Deck> decks = deckRepository.findByPlayerId(playerId);
            List<DeckDTO> deckDTOs = decks.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
            return ResponseEntity.ok(deckDTOs);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get a specific deck by its ID.
     *
     * @param deckId The ID of the deck to retrieve
     * @return ResponseEntity containing the deck or an error message
     */
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

    /**
     * Update an existing deck.
     *
     * @param deckId The ID of the deck to update
     * @param deckDTO The updated deck data
     * @return ResponseEntity containing the updated deck or an error message
     */
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

    /**
     * Delete a specific deck.
     *
     * @param deckId The ID of the deck to delete
     * @param playerId The ID of the player requesting deletion (for authorization)
     * @return ResponseEntity indicating success or an error message
     */
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