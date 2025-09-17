package model;

import java.util.List;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a player in the Dueling Protocol game.
 * A player has attributes such as health, coins, cards and characteristics
 * such as race and class that influence their base attributes.
 */
public class Player {
    private String id;
    private String nickname;
    private int coins;
    private List<Card> cardCollection;
    
    private String playerRace;
    private String playerClass;
    private int healthPoints;
    private int upgradePoints;
    private int baseAttack;
    private int baseDefense;
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
        // Add basic cards to starter deck
        for (int i = 0; i < 5; i++) {
            this.cardCollection.add(new Card(
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
    /**
     * Returns the player's character race.
     *
     * @return the character race
     */
    public String getPlayerRace() { return playerRace; }
    
    /**
     * Sets the player's character race.
     *
     * @param playerRace the new character race
     */
    public void setPlayerRace(String playerRace) { this.playerRace = playerRace; }
    
    /**
     * Returns the player's character class.
     *
     * @return the character class
     */
    public String getPlayerClass() { return playerClass; }
    
    /**
     * Sets the player's character class.
     *
     * @param playerClass the new character class
     */
    public void setPlayerClass(String playerClass) { this.playerClass = playerClass; }
    
    /**
     * Returns the player's health points.
     *
     * @return the player's health points
     */
    public int getHealthPoints() { return healthPoints; }
    
    /**
     * Sets the player's health points.
     * Negative values are converted to zero.
     *
     * @param healthPoints the new health points for the player
     */
    public void setHealthPoints(int healthPoints) { 
        this.healthPoints = Math.max(0, healthPoints); // Ensure health doesn't go below 0
    }
    
    /**
     * Returns the player's available upgrade points.
     *
     * @return the player's upgrade points
     */
    public int getUpgradePoints() { return upgradePoints; }
    
    /**
     * Sets the player's upgrade points.
     * Negative values are converted to zero.
     *
     * @param upgradePoints the new upgrade points for the player
     */
    public void setUpgradePoints(int upgradePoints) { this.upgradePoints = Math.max(0, upgradePoints); }
    
    /**
     * Returns the player's base attack.
     *
     * @return the player's base attack
     */
    public int getBaseAttack() { return baseAttack; }
    
    /**
     * Sets the player's base attack.
     * Negative values are converted to zero.
     *
     * @param baseAttack the new base attack for the player
     */
    public void setBaseAttack(int baseAttack) { this.baseAttack = Math.max(0, baseAttack); }
    
    /**
     * Returns the player's base defense.
     *
     * @return the player's base defense
     */
    public int getBaseDefense() { return baseDefense; }
    
    /**
     * Sets the player's base defense.
     * Negative values are converted to zero.
     *
     * @param baseDefense the new base defense for the player
     */
    public void setBaseDefense(int baseDefense) { this.baseDefense = Math.max(0, baseDefense); }
    
    /**
     * Returns the player's base mana.
     *
     * @return the player's base mana
     */
    public int getBaseMana() { return baseMana; }
    
    /**
     * Sets the player's base mana.
     * Negative values are converted to zero.
     *
     * @param baseMana the new base mana for the player
     */
    public void setBaseMana(int baseMana) { this.baseMana = Math.max(0, baseMana); }

    /**
     * Returns the player's unique identifier.
     *
     * @return the player ID
     */
    public String getId() { return id; }
    
    /**
     * Sets the player's unique identifier.
     *
     * @param id the new player ID
     */
    public void setId(String id) { this.id = id; }
    
    /**
     * Returns the player's display name.
     *
     * @return the player's nickname
     */
    public String getNickname() { return nickname; }
    
    /**
     * Sets the player's display name.
     *
     * @param nickname the new player nickname
     */
    public void setNickname(String nickname) { this.nickname = nickname; }
    
    /**
     * Returns the player's coin amount.
     *
     * @return the player's coin amount
     */
    public int getCoins() { return coins; }
    
    /**
     * Sets the player's coin amount.
     * Negative values are converted to zero.
     *
     * @param coins the new coin amount for the player
     */
    public void setCoins(int coins) { this.coins = Math.max(0, coins); }
    
    /**
     * Returns the player's card collection.
     *
     * @return the player's card list
     */
    public List<Card> getCardCollection() { return cardCollection; }
    
    /**
     * Sets the player's card collection.
     *
     * @param cardCollection the new card list for the player
     */
    public void setCardCollection(List<Card> cardCollection) { this.cardCollection = cardCollection; }
}