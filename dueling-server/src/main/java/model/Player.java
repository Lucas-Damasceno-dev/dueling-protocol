package model;

import jakarta.persistence.*;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.Serializable;
import java.util.Map;
import java.util.HashMap;

@Entity
@Table(name = "players", indexes = {
    @Index(name = "idx_nickname", columnList = "nickname")
})
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Player implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @Column(name = "id", unique = true, nullable = false)
    private String id;
    
    @Column(name = "nickname", nullable = false)
    private String nickname;
    
    @Column(name = "coins", nullable = false)
    private int coins;
    
    @Column(name = "card_collection", columnDefinition = "TEXT")
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

    @OneToOne(mappedBy = "player", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private PlayerRanking playerRanking;

    private static final Logger logger = LoggerFactory.getLogger(Player.class);

    public Player() {
        this.cardCollection = new ArrayList<>();
    }
    
    public Player(String id, String nickname) {
        this.cardCollection = new ArrayList<>();
        this.id = id;
        this.nickname = nickname;
        this.coins = 1000;
        this.healthPoints = 100;
        this.upgradePoints = 0;
        this.playerRanking = new PlayerRanking(this);
        initializeStarterDeck();
        // Serialize cards immediately after creation
        serializeCardCollectionNow();
        logger.debug("New player created: {} ({})", nickname, id);
    }

    @Transient
    private List<Card> cardCollection;

    public List<Card> getCardCollection() {
        if (cardCollection == null) {
            cardCollection = new ArrayList<>();
        }
        return cardCollection;
    }

    public void setCardCollection(List<Card> cardCollection) {
        this.cardCollection = cardCollection;
        // Serialize immediately when collection is set
        serializeCardCollectionNow();
    }

    public void setCharacter(String race, String playerClassParam) {
        this.playerRace = race;
        this.playerClass = playerClassParam;
        applyAttributeBonuses();
        logger.info("Character set: {} as {} {}", id, race, playerClassParam);
    }

    /**
     * Serializes card collection to JSON before persisting to database
     */
    /**
     * Serialize card collection immediately (not waiting for PrePersist)
     */
    private void serializeCardCollectionNow() {
        if (cardCollection != null && !cardCollection.isEmpty()) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                this.cardCollectionJson = mapper.writeValueAsString(cardCollection);
                logger.info("[CARD-PERSIST] Serialized {} cards to JSON for player {}", cardCollection.size(), id);
            } catch (Exception e) {
                logger.error("[CARD-PERSIST] Failed to serialize card collection for player {}: {}", id, e.getMessage(), e);
                this.cardCollectionJson = "[]";
            }
        } else {
            logger.warn("[CARD-PERSIST] No cards to serialize for player {}", id);
            this.cardCollectionJson = "[]";
        }
    }
    
    @PrePersist
    @PreUpdate
    private void serializeCardCollection() {
        // Only serialize if cardCollection exists in memory
        // Don't overwrite existing JSON if cardCollection is transient-null
        if (cardCollection != null && !cardCollection.isEmpty()) {
            serializeCardCollectionNow();
        }
        // If cardCollection is null but cardCollectionJson is empty, it means cards weren't loaded yet
        // Keep existing JSON in database
    }
    
    /**
     * Deserializes card collection from JSON after loading from database
     */
    @PostLoad
    private void deserializeCardCollection() {
        logger.info("[CARD-LOAD] PostLoad called for player {}, JSON length: {}", 
            id, cardCollectionJson != null ? cardCollectionJson.length() : 0);
        if (cardCollectionJson != null && !cardCollectionJson.isEmpty() && !"[]".equals(cardCollectionJson)) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                com.fasterxml.jackson.core.type.TypeReference<List<Card>> typeRef = 
                    new com.fasterxml.jackson.core.type.TypeReference<List<Card>>() {};
                this.cardCollection = mapper.readValue(cardCollectionJson, typeRef);
                logger.info("[CARD-LOAD] Deserialized {} cards from JSON for player {}", cardCollection.size(), id);
            } catch (Exception e) {
                logger.error("[CARD-LOAD] Failed to deserialize card collection for player {}: {}", id, e.getMessage(), e);
                this.cardCollection = new ArrayList<>();
            }
        } else {
            logger.warn("[CARD-LOAD] No cards JSON to deserialize for player {}", id);
            this.cardCollection = new ArrayList<>();
        }
    }

    private void initializeStarterDeck() {
        List<Card> starterDeckCards = getCardCollection();
        for (int i = 0; i < 5; i++) {
            starterDeckCards.add(new Card(
                "basic-" + i,
                "Basic Card " + i,
                1, 1, "Common",
                Card.CardType.ATTACK,
                "Standard attack", 1
            ));
        }

        // Add a combo card
        Map<String, String> comboParams = new HashMap<>();
        comboParams.put("requiredCardName", "Basic Card 1");
        comboParams.put("bonusDamage", "3");
        starterDeckCards.add(new Card(
            "combo-1",
            "Combo Strike",
            2, 1, "Rare",
            Card.CardType.COMBO,
            "Deals +3 damage if you played 'Basic Card 1' this turn.",
            2,
            comboParams
        ));

        // Add a counter-spell card
        starterDeckCards.add(new Card(
            "counter-1",
            "Counter Spell",
            0, 0, "Rare",
            Card.CardType.COUNTER_SPELL,
            "Counters a magic spell.",
            3
        ));

        logger.info("[CARD-INIT] Starter deck created with {} cards for player {}", starterDeckCards.size(), id);
    }

    private void applyAttributeBonuses() {
        this.baseAttack = 10;
        this.baseDefense = 10;
        this.baseMana = 5;
        
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

    public String getPlayerRace() { return playerRace; }
    public void setPlayerRace(String playerRace) { this.playerRace = playerRace; }
    
    public String getPlayerClass() { return playerClass; }
    public void setPlayerClass(String playerClass) { this.playerClass = playerClass; }
    
    public int getHealthPoints() { return healthPoints; }
    public void setHealthPoints(int healthPoints) { 
        this.healthPoints = Math.max(0, healthPoints);
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

    public PlayerRanking getPlayerRanking() {
        return playerRanking;
    }

    public void setPlayerRanking(PlayerRanking playerRanking) {
        this.playerRanking = playerRanking;
    }

    public ResourceType getResourceType() {
        if (this.playerClass == null) {
            return ResourceType.MANA;
        }
        switch (this.playerClass) {
            case "Mage":
                return ResourceType.MANA;
            case "Warrior":
                return ResourceType.FURIA;
            case "Rogue":
                return ResourceType.ENERGIA;
            case "Archer":
                return ResourceType.FOCO;
            default:
                return ResourceType.MANA;
        }
    }

    public boolean hasCards(List<String> cardIds) {
        List<String> playerCardIds = getCardCollection().stream()
            .map(Card::getId)
            .collect(Collectors.toList());
        
        return playerCardIds.containsAll(cardIds);
    }
}
