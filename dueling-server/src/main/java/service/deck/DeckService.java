package service.deck;

import dto.DeckDTO;
import exception.ResourceConflictException;
import exception.UserNotFoundException;
import model.Card;
import model.Deck;
import model.Player;
import repository.CardRepository;
import repository.DeckRepository;
import repository.PlayerRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
public class DeckService {

    private static final int MAX_DECK_SIZE = 30;
    private static final int MAX_COPIES_OF_CARD = 3;

    @Autowired
    private DeckRepository deckRepository;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private CardRepository cardRepository;

    @Transactional
    public Deck createDeck(String playerId, DeckDTO deckDTO) {
        Player player = playerRepository.findById(playerId)
            .orElseThrow(() -> new UserNotFoundException("Player with ID " + playerId + " not found"));

        if (deckRepository.existsByNameAndPlayerId(deckDTO.getName(), playerId)) {
            throw new ResourceConflictException("A deck with name '" + deckDTO.getName() + "' already exists for this player");
        }

        Deck deck = new Deck();
        deck.setId(java.util.UUID.randomUUID().toString());
        deck.setName(deckDTO.getName());
        deck.setDescription(deckDTO.getDescription());
        deck.setPlayer(player);

        if (deckRepository.countByPlayerId(playerId) == 0) {
            deck.setDefault(true);
        }

        return deckRepository.save(deck);
    }

    @Transactional
    public Deck addCardToDeck(String playerId, String deckId, String cardId) {
        Player player = playerRepository.findById(playerId)
            .orElseThrow(() -> new UserNotFoundException("Player with ID " + playerId + " not found"));
        Deck deck = deckRepository.findByIdAndPlayerId(deckId, playerId)
            .orElseThrow(() -> new ResourceConflictException("Deck with ID " + deckId + " not found or does not belong to player"));
        Card card = cardRepository.findById(cardId)
            .orElseThrow(() -> new ResourceConflictException("Card with ID " + cardId + " not found"));

        // Validate that player owns the card (assuming a master card collection)
        // This logic depends on how player card ownership is tracked. For now, we assume if the card exists, they can add it.

        if (deck.getCardCount() >= MAX_DECK_SIZE) {
            throw new IllegalStateException("Deck is full. Cannot add more than " + MAX_DECK_SIZE + " cards.");
        }

        long copiesInDeck = Collections.frequency(deck.getCards(), card);
        if (copiesInDeck >= MAX_COPIES_OF_CARD) {
            throw new IllegalStateException("Cannot add more than " + MAX_COPIES_OF_CARD + " copies of the same card.");
        }

        deck.addCard(card);
        return deckRepository.save(deck);
    }

    @Transactional
    public Deck removeCardFromDeck(String playerId, String deckId, String cardId) {
        Deck deck = deckRepository.findByIdAndPlayerId(deckId, playerId)
            .orElseThrow(() -> new ResourceConflictException("Deck with ID " + deckId + " not found or does not belong to player"));
        Card card = cardRepository.findById(cardId)
            .orElseThrow(() -> new ResourceConflictException("Card with ID " + cardId + " not found"));

        if (!deck.containsCard(card)) {
            throw new IllegalStateException("Deck does not contain the specified card.");
        }

        deck.removeCard(card);
        return deckRepository.save(deck);
    }

    @Transactional
    public void deleteDeck(String playerId, String deckId) {
        if (!deckRepository.existsByIdAndPlayerId(deckId, playerId)) {
            throw new ResourceConflictException("Deck with ID " + deckId + " not found or does not belong to player");
        }
        deckRepository.deleteById(deckId);
    }

    @Transactional
    public void setDefaultDeck(String playerId, String deckId) {
        if (!deckRepository.existsByIdAndPlayerId(deckId, playerId)) {
            throw new ResourceConflictException("Deck with ID " + deckId + " not found or does not belong to player");
        }
        deckRepository.setDefaultDeck(deckId, playerId);
    }

    public Optional<Deck> getDeckForPlayer(String deckId, String playerId) {
        return deckRepository.findByIdAndPlayerId(deckId, playerId);
    }

    public List<Deck> getPlayerDecks(String playerId) {
        return deckRepository.findByPlayerId(playerId);
    }

    public Optional<Deck> getDefaultDeck(String playerId) {
        return deckRepository.findByPlayerIdAndIsDefaultTrue(playerId);
    }

    public boolean isValidDeckForGame(String deckId, String playerId) {
        Optional<Deck> deckOpt = getDeckForPlayer(deckId, playerId);
        return deckOpt.map(Deck::isMinSize).orElse(false);
    }
}
