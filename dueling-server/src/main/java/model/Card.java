package model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Objects;

/**
 * Represents a card in the Dueling Protocol game.
 * Each card has attributes such as attack, defense, rarity, type, and mana cost.
 * Cards are used by players during matches to apply effects and strategies.
 */
@Entity
@Table(name = "cards")
public class Card {
    @Id
    @Column(name = "id", unique = true, nullable = false)
    private String id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "attack")
    private int attack;

    @Column(name = "defense")
    private int defense;

    @Column(name = "rarity")
    private String rarity;

    @Enumerated(EnumType.STRING)
    @Column(name = "card_type", nullable = false)
    private CardType cardType;

    @Column(name = "effect_description", length = 512)
    private String effectDescription;

    @Column(name = "mana_cost")
    private int manaCost;

    /**
     * Enumeration that defines the possible card types in the game.
     */
    public enum CardType {
        /** Attack card that deals damage to the opponent */
        ATTACK,
        /** Defense card that protects the player */
        DEFENSE,
        /** Magic card with special effects */
        MAGIC,
        /** Card that improves player attributes */
        ATTRIBUTE,
        /** Card that modifies the match scenario */
        SCENARIO,
        /** Equipment card that provides permanent bonuses */
        EQUIPMENT
    }

    /**
     * Default constructor for the Card class.
     */
    public Card() {}

    /**
     * Complete constructor to create a card with all specified attributes.
     *
     * @param id Unique identifier of the card
     * @param name Card name
     * @param attack Card attack value
     * @param defense Card defense value
     * @param rarity Card rarity (e.g.: "Common", "Rare", "Legendary")
     * @param cardType Card type, defined by the CardType enumeration
     * @param effectDescription Textual description of the card's effect
     * @param manaCost Mana cost to play the card
     * @throws NullPointerException if id, name, rarity, cardType or effectDescription are null
     */
    public Card(String id, String name, int attack, int defense, String rarity, CardType cardType, String effectDescription, int manaCost) {
        this.id = Objects.requireNonNull(id, "ID cannot be null");
        this.name = Objects.requireNonNull(name, "Name cannot be null");
        this.attack = Math.max(0, attack);
        this.defense = Math.max(0, defense);
        this.rarity = Objects.requireNonNull(rarity, "Rarity cannot be null");
        this.cardType = Objects.requireNonNull(cardType, "Card type cannot be null");
        this.effectDescription = Objects.requireNonNull(effectDescription, "Effect description cannot be null");
        this.manaCost = Math.max(0, manaCost);
    }

    // Getters and Setters
    /**
     * Returns the card's unique identifier.
     *
     * @return the card ID
     */
    public String getId() { return id; }
    
    /**
     * Sets the card's unique identifier.
     *
     * @param id the new card ID
     * @throws NullPointerException if the id is null
     */
    public void setId(String id) { this.id = Objects.requireNonNull(id, "ID cannot be null"); }

    /**
     * Returns the card's name.
     *
     * @return the card name
     */
    public String getName() { return name; }
    
    /**
     * Sets the card's name.
     *
     * @param name the new card name
     * @throws NullPointerException if the name is null
     */
    public void setName(String name) { this.name = Objects.requireNonNull(name, "Name cannot be null"); }

    /**
     * Returns the card's attack value.
     *
     * @return the card's attack value
     */
    public int getAttack() { return attack; }
    
    /**
     * Sets the card's attack value.
     * Negative values are converted to zero.
     *
     * @param attack the new attack value for the card
     */
    public void setAttack(int attack) { this.attack = Math.max(0, attack); }

    /**
     * Returns the card's defense value.
     *
     * @return the card's defense value
     */
    public int getDefense() { return defense; }
    
    /**
     * Sets the card's defense value.
     * Negative values are converted to zero.
     *
     * @param defense the new defense value for the card
     */
    public void setDefense(int defense) { this.defense = Math.max(0, defense); }

    /**
     * Returns the card's rarity.
     *
     * @return the card's rarity
     */
    public String getRarity() { return rarity; }
    
    /**
     * Sets the card's rarity.
     *
     * @param rarity the new card rarity
     * @throws NullPointerException if the rarity is null
     */
    public void setRarity(String rarity) { this.rarity = Objects.requireNonNull(rarity, "Rarity cannot be null"); }

    /**
     * Returns the card's type.
     *
     * @return the card's type
     */
    public CardType getCardType() { return cardType; }
    
    /**
     * Sets the card's type.
     *
     * @param cardType the new card type
     * @throws NullPointerException if the type is null
     */
    public void setCardType(CardType cardType) { this.cardType = Objects.requireNonNull(cardType, "Card type cannot be null"); }

    /**
     * Returns the card's effect description.
     *
     * @return the card's effect description
     */
    public String getEffectDescription() { return effectDescription; }
    
    /**
     * Sets the card's effect description.
     *
     * @param effectDescription the new effect description for the card
     * @throws NullPointerException if the description is null
     */
    public void setEffectDescription(String effectDescription) { this.effectDescription = Objects.requireNonNull(effectDescription, "Effect description cannot be null"); }

    /**
     * Returns the card's mana cost.
     *
     * @return the card's mana cost
     */
    public int getManaCost() { return manaCost; }
    
    /**
     * Sets the card's mana cost.
     * Negative values are converted to zero.
     *
     * @param manaCost the new mana cost for the card
     */
    public void setManaCost(int manaCost) { this.manaCost = Math.max(0, manaCost); }

    /**
     * Compares this card with another object to check equality.
     * Two cards are considered equal if they have the same ID.
     *
     * @param o the object to compare
     * @return true if the objects are equal, false otherwise
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Card card = (Card) o;
        return Objects.equals(id, card.id);
    }

    /**
     * Returns the hash code for this card.
     * The hash code is based on the card's ID.
     *
     * @return the card's hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    /**
     * Returns a string representation of this card.
     *
     * @return a string representing the card
     */
    @Override
    public String toString() {
        return "Card{" +
                "id='" + id + "'" +
                ", name='" + name + "'" +
                ", attack=" + attack +
                ", defense=" + defense +
                ", rarity='" + rarity + "'" +
                ", cardType=" + cardType +
                ", manaCost=" + manaCost +
                '}';
    }
}
