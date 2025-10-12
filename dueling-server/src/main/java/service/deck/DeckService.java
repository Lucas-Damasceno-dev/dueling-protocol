package service.deck;

import model.Card;
import model.Deck;
import model.Player;
import repository.DeckRepository;
import repository.PlayerRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Service class for handling deck-related business logic.
 * Provides methods for deck operations and validation.
 */
@Service
public class DeckService {

    @Autowired
    private DeckRepository deckRepository;

    @Autowired
    private PlayerRepository playerRepository;

    /**
     * Get a deck by its ID and player ID for validation.
     *
     * @param deckId The ID of the deck
     * @param playerId The ID of the player
     * @return Optional containing the deck if found and belongs to the player
     */
    public Optional<Deck> getDeckForPlayer(String deckId, String playerId) {
        return deckRepository.findByIdAndPlayerId(deckId, playerId);
    }

    /**
     * Get the cards from a specific deck.
     *
     * @param deckId The ID of the deck
     * @param playerId The ID of the player who owns the deck
     * @return List of cards in the deck, or null if deck not found
     */
    public List<Card> getDeckCards(String deckId, String playerId) {
        Optional<Deck> deckOpt = deckRepository.findByIdAndPlayerId(deckId, playerId);
        if (deckOpt.isPresent()) {
            return deckOpt.get().getCards();
        }
        return null;
    }

    /**
     * Validate that a deck exists and belongs to the specified player.
     *
     * @param deckId The ID of the deck to validate
     * @param playerId The ID of the player
     * @return true if the deck exists and belongs to the player, false otherwise
     */
    public boolean validateDeckOwnership(String deckId, String playerId) {
        return deckRepository.findByIdAndPlayerId(deckId, playerId).isPresent();
    }

    /**
     * Validate that a deck has the minimum required number of cards.
     *
     * @param deck The deck to validate
     * @return true if the deck has at least the minimum number of cards, false otherwise
     */
    public boolean validateDeckSize(Deck deck) {
        if (deck == null) {
            return false;
        }
        return deck.isMinSize();
    }

    /**
     * Get the default deck for a player.
     *
     * @param playerId The ID of the player
     * @return Optional containing the default deck if found
     */
    public Optional<Deck> getDefaultDeck(String playerId) {
        return deckRepository.findByPlayerIdAndIsDefaultTrue(playerId);
    }

    /**
     * Get all decks for a player.
     *
     * @param playerId The ID of the player
     * @return List of decks belonging to the player
     */
    public List<Deck> getPlayerDecks(String playerId) {
        return deckRepository.findByPlayerId(playerId);
    }

    /**
     * Check if a deck is valid for use in a game (has required size and belongs to player).
     *
     * @param deckId The ID of the deck
     * @param playerId The ID of the player
     * @return true if the deck is valid for use in a game, false otherwise
     */
    public boolean isValidDeckForGame(String deckId, String playerId) {
        Optional<Deck> deckOpt = deckRepository.findByIdAndPlayerId(deckId, playerId);
        if (deckOpt.isPresent()) {
            Deck deck = deckOpt.get();
            return validateDeckSize(deck);
        }
        return false;
    }
}