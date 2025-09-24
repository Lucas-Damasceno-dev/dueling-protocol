package model;

import java.util.List;
import java.util.ArrayList;

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

    public Player() {
        this.cardCollection = new ArrayList<>();
    }
    
    public Player(String id, String nickname) {
        this();
        this.id = id;
        this.nickname = nickname;
        this.coins = 1000;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    
    public int getCoins() { return coins; }
    public void setCoins(int coins) { this.coins = coins; }
    
    public List<Card> getCardCollection() { return cardCollection; }
    public void setCardCollection(List<Card> cardCollection) { this.cardCollection = cardCollection; }

    public String getPlayerRace() { return playerRace; }
    public void setPlayerRace(String playerRace) { this.playerRace = playerRace; }
    public String getPlayerClass() { return playerClass; }
    public void setPlayerClass(String playerClass) { this.playerClass = playerClass; }
    public int getHealthPoints() { return healthPoints; }
    public void setHealthPoints(int healthPoints) { this.healthPoints = healthPoints; }
    public int getUpgradePoints() { return upgradePoints; }
    public void setUpgradePoints(int upgradePoints) { this.upgradePoints = upgradePoints; }
    public int getBaseAttack() { return baseAttack; }
    public void setBaseAttack(int baseAttack) { this.baseAttack = baseAttack; }
    public int getBaseDefense() { return baseDefense; }
    public void setBaseDefense(int baseDefense) { this.baseDefense = baseDefense; }
    public int getBaseMana() { return baseMana; }
    public void setBaseMana(int baseMana) { this.baseMana = baseMana; }
}
