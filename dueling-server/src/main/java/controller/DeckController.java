package controller;

import dto.DeckDTO;
import jakarta.validation.Valid;
import model.Deck;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import service.deck.DeckService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/decks") // Changed to plural for REST conventions
@Tag(name = "Decks", description = "Endpoints for managing player decks")
public class DeckController {

    @Autowired
    private DeckService deckService;

    // Helper to convert Entity to DTO
    private DeckDTO convertToDTO(Deck deck) {
        return new DeckDTO(deck.getId(), deck.getName(), deck.getDescription(), deck.getPlayer().getId(), deck.getCards(), deck.isDefault());
    }

    @Operation(summary = "Get all decks for a player")
    @GetMapping("/player/{playerId}")
    public ResponseEntity<List<DeckDTO>> getPlayerDecks(@PathVariable String playerId) {
        List<DeckDTO> deckDTOs = deckService.getPlayerDecks(playerId).stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
        return ResponseEntity.ok(deckDTOs);
    }

    @Operation(summary = "Create a new deck for a player")
    @PostMapping("/player/{playerId}")
    public ResponseEntity<DeckDTO> createDeck(@PathVariable String playerId, @Valid @RequestBody DeckDTO deckDTO) {
        Deck createdDeck = deckService.createDeck(playerId, deckDTO);
        return ResponseEntity.ok(convertToDTO(createdDeck));
    }

    @Operation(summary = "Add a card to a deck")
    @PostMapping("/{deckId}/cards/{cardId}")
    public ResponseEntity<DeckDTO> addCardToDeck(@RequestParam String playerId, @PathVariable String deckId, @PathVariable String cardId) {
        Deck updatedDeck = deckService.addCardToDeck(playerId, deckId, cardId);
        return ResponseEntity.ok(convertToDTO(updatedDeck));
    }

    @Operation(summary = "Remove a card from a deck")
    @DeleteMapping("/{deckId}/cards/{cardId}")
    public ResponseEntity<DeckDTO> removeCardFromDeck(@RequestParam String playerId, @PathVariable String deckId, @PathVariable String cardId) {
        Deck updatedDeck = deckService.removeCardFromDeck(playerId, deckId, cardId);
        return ResponseEntity.ok(convertToDTO(updatedDeck));
    }

    @Operation(summary = "Delete a deck")
    @DeleteMapping("/{deckId}")
    public ResponseEntity<Void> deleteDeck(@RequestParam String playerId, @PathVariable String deckId) {
        deckService.deleteDeck(playerId, deckId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Set a deck as the default for a player")
    @PutMapping("/player/{playerId}/default/{deckId}")
    public ResponseEntity<Void> setDefaultDeck(@PathVariable String playerId, @PathVariable String deckId) {
        deckService.setDefaultDeck(playerId, deckId);
        return ResponseEntity.noContent().build();
    }
}
