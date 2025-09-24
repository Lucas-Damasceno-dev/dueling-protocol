package model;

public class Card {
    private String id;
    private String name;
    private int attack;
    private int defense;
    private String rarity;
    private CardType cardType;
    private String effectDescription;
    private int manaCost;

    public enum CardType {
        ATTACK,
        DEFENSE,
        MAGIC,
        ATTRIBUTE,
        SCENARIO,
        EQUIPMENT
    }

    public Card() {}

    public Card(String id, String name, int attack, int defense, String rarity, CardType cardType, String effectDescription, int manaCost) {
        this.id = id;
        this.name = name;
        this.attack = attack;
        this.defense = defense;
        this.rarity = rarity;
        this.cardType = cardType;
        this.effectDescription = effectDescription;
        this.manaCost = manaCost;
    }

    // Getters e Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getAttack() { return attack; }
    public void setAttack(int attack) { this.attack = attack; }

    public int getDefense() { return defense; }
    public void setDefense(int defense) { this.defense = defense; }

    public String getRarity() { return rarity; }
    public void setRarity(String rarity) { this.rarity = rarity; }

    public CardType getCardType() { return cardType; }
    public void setCardType(CardType cardType) { this.cardType = cardType; }

    public String getEffectDescription() { return effectDescription; }
    public void setEffectDescription(String effectDescription) { this.effectDescription = effectDescription; }

    public int getManaCost() { return manaCost; }
    public void setManaCost(int manaCost) { this.manaCost = manaCost; }
}