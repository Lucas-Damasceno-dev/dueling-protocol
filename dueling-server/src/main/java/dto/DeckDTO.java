package dto;

import model.Card;

import java.util.List;

/**
 * Data Transfer Object for Deck entity.
 * Used for sending deck information between client and server.
 */
public class DeckDTO {
    private String id;
    private String name;
    private String description;
    private String playerId;
    private List<Card> cards;
    private boolean isDefault;

    /**
     * Default constructor for DeckDTO.
     */
    public DeckDTO() {}

    /**
     * Constructor to create a DeckDTO from deck properties.
     *
     * @param id The deck ID
     * @param name The deck name
     * @param description The deck description
     * @param playerId The ID of the player who owns this deck
     * @param cards The cards in this deck
     * @param isDefault Whether this is the player's default deck
     */
    public DeckDTO(String id, String name, String description, String playerId, List<Card> cards, boolean isDefault) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.playerId = playerId;
        this.cards = cards;
        this.isDefault = isDefault;
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

    public String getPlayerId() {
        return playerId;
    }

    public void setPlayerId(String playerId) {
        this.playerId = playerId;
    }

    public List<Card> getCards() {
        return cards;
    }

    public void setCards(List<Card> cards) {
        this.cards = cards;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public void setDefault(boolean isDefault) {
        this.isDefault = isDefault;
    }

    @Override
    public String toString() {
        return "DeckDTO{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", playerId='" + playerId + '\'' +
                ", cardCount=" + (cards != null ? cards.size() : 0) +
                ", isDefault=" + isDefault +
                '}';
    }
}