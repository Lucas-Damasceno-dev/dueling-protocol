package model;

import jakarta.persistence.*;
import java.util.List;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a player in the Dueling Protocol game.
 * A player has attributes such as health, coins, cards and characteristics
 * such as race and class that influence their base attributes.
 */
@Entity
@Table(name = "players")
public class Player {
    @Id
    @Column(name = "id", unique = true, nullable = false)
    private String id;
    
    @Column(name = "nickname", nullable = false)
    private String nickname;
    
    @Column(name = "coins", nullable = false)
    private int coins;
    
    @Column(name = "card_collection", columnDefinition = "TEXT") // Store as JSON string
    private String cardCollectionJson;
    
    @Column(name = "player_race")
    private String playerRace;
    
    @Column(name = "player_class")
    private String playerClass;
    
    @Column(name = "health_points", nullable = false)
    private int healthPoints;
    
    @Column(name = "upgrade_points", nullable = false)
    private int upgradePoints;
    
    @Column(name = "base_attack", nullable = false)
    private int baseAttack;
    
    @Column(name = "base_defense", nullable = false)
    private int baseDefense;
    
    @Column(name = "base_mana", nullable = false)
    private int baseMana;
    
    private static final Logger logger = LoggerFactory.getLogger(Player.class);

    /**
     * Default constructor for the Player class.
     * Initializes the card collection as an empty list.
     */
    public Player() {
        this.cardCollection = new ArrayList<>();
    }
    
    /**
     * Constructor to create a player with specified ID and nickname.
     * Initializes the player's default attributes, including coins, health points
     * and an initial deck of cards.
     *
     * @param id Unique identifier of the player
     * @param nickname Display name of the player
     */
    public Player(String id, String nickname) {
        this.cardCollection = new ArrayList<>();
        this.id = id;
        this.nickname = nickname;
        this.coins = 1000;
        this.healthPoints = 100;
        this.upgradePoints = 0;
        initializeStarterDeck();
        logger.debug("New player created: {} ({})", nickname, id);
    }

    // Transient field to hold card collection for runtime access
    @Transient
    private List<Card> cardCollection;

    /**
     * Gets the card collection from JSON string when needed
     */
    public List<Card> getCardCollection() {
        if (cardCollection == null && cardCollectionJson != null) {
            // Parse the JSON string back to List<Card>
            // For now, we'll return an empty list - in a full implementation,
            // we would use a JSON library like Jackson to deserialize
            cardCollection = new ArrayList<>();
        }
        return cardCollection != null ? cardCollection : new ArrayList<>();
    }

    /**
     * Sets the card collection and updates the JSON string
     */
    public void setCardCollection(List<Card> cardCollection) {
        this.cardCollection = cardCollection;
        // Convert to JSON string for persistence
        // In a full implementation, we would serialize the list to JSON
        this.cardCollectionJson = cardCollection != null ? cardCollection.toString() : "[]";
    }

    /**
     * Sets the player's character race and class.
     * Applies attribute bonuses corresponding to the race and class combination.
     *
     * @param race Character race (e.g.: "Elf", "Dwarf", "Human", "Orc")
     * @param playerClass Character class (e.g.: "Mage", "Archer", "Warrior", "Rogue")
     */
    public void setCharacter(String race, String playerClass) {
        this.playerRace = race;
        this.playerClass = playerClass;
        applyAttributeBonuses();
        logger.info("Character set: {} as {} {}", id, race, playerClass);
    }

    /**
     * Initializes the player's initial deck with basic cards.
     * Adds 5 basic ATTACK-type cards to the player's deck.
     */
    private void initializeStarterDeck() {
        List<Card> cardCollection = getCardCollection(); // This will initialize if null
        // Add basic cards to starter deck
        for (int i = 0; i < 5; i++) {
            cardCollection.add(new Card(
                "basic-" + i,
                "Basic Card " + i,
                1, 1, "Common",
                Card.CardType.ATTACK,
                "Standard attack", 1
            ));
        }
        logger.debug("Starter deck created with {} cards for player {}", cardCollection.size(), id);
    }

    /**
     * Applies attribute bonuses based on the player's race and class.
     * Bonuses vary according to the specific race and class combination.
     */
    private void applyAttributeBonuses() {
        // Reset attributes
        this.baseAttack = 10;
        this.baseDefense = 10;
        this.baseMana = 5;
        
        // Apply race and class bonuses
        if ("Elf".equals(this.playerRace)) {
            this.baseMana += 3;
            if ("Mage".equals(this.playerClass)) {
                this.baseAttack += 2;
                this.baseMana += 5;
            } else if ("Archer".equals(this.playerClass)) {
                this.baseAttack += 5;
                this.baseDefense += 2;
            }
        } else if ("Dwarf".equals(this.playerRace)) {
            this.baseDefense += 5;
            if ("Warrior".equals(this.playerClass)) {
                this.baseAttack += 5;
                this.baseDefense += 5;
            } else if ("Rogue".equals(this.playerClass)) {
                this.baseAttack += 3;
                this.baseDefense += 2;
                this.baseMana += 2;
            }
        } else if ("Human".equals(this.playerRace)) {
            // Balanced bonuses
            this.baseAttack += 2;
            this.baseDefense += 2;
            this.baseMana += 2;
        } else if ("Orc".equals(this.playerRace)) {
            this.baseAttack += 5;
            this.baseDefense += 3;
            this.baseMana -= 1;
        }
        
        logger.debug("Attributes applied for {}: Atk={}, Def={}, Mana={}", 
                    id, baseAttack, baseDefense, baseMana);
    }

    // Getters and setters
    public String getPlayerRace() { return playerRace; }
    public void setPlayerRace(String playerRace) { this.playerRace = playerRace; }
    
    public String getPlayerClass() { return playerClass; }
    public void setPlayerClass(String playerClass) { this.playerClass = playerClass; }
    
    public int getHealthPoints() { return healthPoints; }
    public void setHealthPoints(int healthPoints) { 
        this.healthPoints = Math.max(0, healthPoints); // Ensure health doesn't go below 0
    }
    
    public int getUpgradePoints() { return upgradePoints; }
    public void setUpgradePoints(int upgradePoints) { this.upgradePoints = Math.max(0, upgradePoints); }
    
    public int getBaseAttack() { return baseAttack; }
    public void setBaseAttack(int baseAttack) { this.baseAttack = Math.max(0, baseAttack); }
    
    public int getBaseDefense() { return baseDefense; }
    public void setBaseDefense(int baseDefense) { this.baseDefense = Math.max(0, baseDefense); }
    
    public int getBaseMana() { return baseMana; }
    public void setBaseMana(int baseMana) { this.baseMana = Math.max(0, baseMana); }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    
    public int getCoins() { return coins; }
    public void setCoins(int coins) { this.coins = Math.max(0, coins); }
    
    public String getCardCollectionJson() { return cardCollectionJson; }
    public void setCardCollectionJson(String cardCollectionJson) { this.cardCollectionJson = cardCollectionJson; }
}