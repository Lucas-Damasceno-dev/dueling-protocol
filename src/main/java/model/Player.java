package model;

import java.util.List;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    initializeStarterDeck();
    logger.debug("Novo jogador criado: {} ({})", nickname, id);
    }

    public void setCharacter(String race, String playerClass) {
        this.playerRace = race;
        this.playerClass = playerClass;
        applyAttributeBonuses();
        logger.info("Personagem configurado: {} como {} {}", id, race, playerClass);
    }

    private void initializeStarterDeck() {
        // Add basic cards to starter deck
        for (int i = 0; i < 5; i++) {
            this.cardCollection.add(new Card(
                "basic-" + i,
                "Carta Básica " + i,
                1, 1, "Comum",
                Card.CardType.ATTACK,
                "Ataque padrão", 1
            ));
        }
        logger.debug("Deck inicial criado com {} cartas para o jogador {}", cardCollection.size(), id);
    }

    private void applyAttributeBonuses() {
        // Reset attributes
        this.baseAttack = 10;
        this.baseDefense = 10;
        this.baseMana = 5;
        
        // Apply race and class bonuses
        if ("Elfo".equals(this.playerRace)) {
            this.baseMana += 3;
            if ("Mago".equals(this.playerClass)) {
                this.baseAttack += 2;
                this.baseMana += 5;
            } else if ("Arqueiro".equals(this.playerClass)) {
                this.baseAttack += 5;
                this.baseDefense += 2;
            }
        } else if ("Anão".equals(this.playerRace)) {
            this.baseDefense += 5;
            if ("Guerreiro".equals(this.playerClass)) {
                this.baseAttack += 5;
                this.baseDefense += 5;
            } else if ("Ladino".equals(this.playerClass)) {
                this.baseAttack += 3;
                this.baseDefense += 2;
                this.baseMana += 2;
            }
        } else if ("Humano".equals(this.playerRace)) {
            // Balanced bonuses
            this.baseAttack += 2;
            this.baseDefense += 2;
            this.baseMana += 2;
        } else if ("Orc".equals(this.playerRace)) {
            this.baseAttack += 5;
            this.baseDefense += 3;
            this.baseMana -= 1;
        }
        
        logger.debug("Atributos aplicados para {}: Atk={}, Def={}, Mana={}", 
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
    public List<Card> getCardCollection() { return cardCollection; }
    public void setCardCollection(List<Card> cardCollection) { this.cardCollection = cardCollection; }
}