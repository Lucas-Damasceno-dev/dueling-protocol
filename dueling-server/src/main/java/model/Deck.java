package model;

import jakarta.persistence.*;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import java.util.List;
import java.util.ArrayList;

/**
 * Represents a deck in the Dueling Protocol game.
 * A deck is a collection of cards selected by a player for gameplay.
 * Each deck belongs to a single player and contains multiple cards.
 */
@Entity
@Table(name = "decks", indexes = {
    @Index(name = "idx_deck_player_id", columnList = "player_id")
})
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
import java.io.Serializable;
public class Deck implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @Column(name = "id", unique = true, nullable = false)
    private String id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false, foreignKey = @ForeignKey(name = "fk_deck_player"))
    private Player player;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "deck_cards",
        joinColumns = @JoinColumn(name = "deck_id", foreignKey = @ForeignKey(name = "fk_deck_cards_deck")),
        inverseJoinColumns = @JoinColumn(name = "card_id", foreignKey = @ForeignKey(name = "fk_deck_cards_card"))
    )
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    private List<Card> cards;

    @Column(name = "is_default")
    private boolean isDefault;

    /**
     * Default constructor for the Deck class.
     */
    public Deck() {
        this.cards = new ArrayList<>();
    }

    /**
     * Constructor to create a deck with specific name and player.
     *
     * @param id Unique identifier of the deck
     * @param name The name of the deck
     * @param player The player who owns this deck
     */
    public Deck(String id, String name, Player player) {
        this.id = id;
        this.name = name;
        this.player = player;
        this.cards = new ArrayList<>();
        this.isDefault = false;
    }

    /**
     * Constructor to create a deck with name, player, and initial cards.
     *
     * @param id Unique identifier of the deck
     * @param name The name of the deck
     * @param player The player who owns this deck
     * @param cards The initial cards in this deck
     */
    public Deck(String id, String name, Player player, List<Card> cards) {
        this.id = id;
        this.name = name;
        this.player = player;
        this.cards = cards != null ? new ArrayList<>(cards) : new ArrayList<>();
        this.isDefault = false;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Player getPlayer() {
        return player;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    public List<Card> getCards() {
        return cards != null ? new ArrayList<>(cards) : new ArrayList<>();
    }

    public void setCards(List<Card> cards) {
        this.cards = cards != null ? new ArrayList<>(cards) : new ArrayList<>();
    }

    /**
     * Add a card to the deck.
     * 
     * @param card The card to add
     * @return true if the card was added, false otherwise
     */
    public boolean addCard(Card card) {
        if (card != null && !cards.contains(card)) {
            return cards.add(card);
        }
        return false;
    }

    /**
     * Remove a card from the deck.
     * 
     * @param card The card to remove
     * @return true if the card was removed, false otherwise
     */
    public boolean removeCard(Card card) {
        if (card != null) {
            return cards.remove(card);
        }
        return false;
    }

    /**
     * Check if the deck contains a specific card.
     * 
     * @param card The card to check for
     * @return true if the deck contains the card, false otherwise
     */
    public boolean containsCard(Card card) {
        return card != null && cards.contains(card);
    }

    /**
     * Get the number of cards in the deck.
     * 
     * @return The number of cards in the deck
     */
    public int getCardCount() {
        return cards != null ? cards.size() : 0;
    }

    /**
     * Check if the deck is at the maximum allowed size.
     * 
     * @return true if the deck is at max size (30 cards), false otherwise
     */
    public boolean isMaxSize() {
        return getCardCount() >= 30; // Standard deck size limit
    }

    /**
     * Check if the deck is at the minimum required size.
     * 
     * @return true if the deck has at least 5 cards, false otherwise
     */
    public boolean isMinSize() {
        return getCardCount() >= 5; // Minimum deck size
    }

    public boolean isDefault() {
        return isDefault;
    }

    public void setDefault(boolean defaultDeck) {
        this.isDefault = defaultDeck;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Deck deck = (Deck) o;
        return id != null ? id.equals(deck.id) : deck.id == null;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "Deck{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", cardCount=" + getCardCount() +
                ", isDefault=" + isDefault +
                "}";
    }
}