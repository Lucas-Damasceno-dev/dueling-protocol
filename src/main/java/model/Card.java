package model;

public class Card {
    private String id;
    private String name;
    private int attack;
    private int defense;
    private String rarity;
    
    public Card() {}
    
    public Card(String id, String name, int attack, int defense, String rarity) {
        this.id = id;
        this.name = name;
        this.attack = attack;
        this.defense = defense;
        this.rarity = rarity;
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
}
